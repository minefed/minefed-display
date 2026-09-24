package team.minefed.mods.display.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.render.RenderLayer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/** Exercises actual depth-buffer behavior without starting a game or browser. */
public final class DisplayOcclusionTest {
    public static void main(String[] args) {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed; a desktop OpenGL driver is required");
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(32, 32, "Display occlusion regression", 0, 0);
        if (window == 0) {
            GLFW.glfwTerminate();
            throw new IllegalStateException("Could not create the hidden OpenGL context");
        }
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            RenderSystem.initRenderThread();
            SharedConstants.createGameVersion();
            Bootstrap.initialize();
            GL11.glViewport(0, 0, 32, 32);
            RenderLayer layer = DisplayRenderLayers.browser(0);

            // Reproduce the previous immediate-render path: a preceding layer's
            // teardown disables depth, letting the rear display cover the wall.
            clear();
            wall();
            RenderSystem.disableDepthTest();
            quad(0.5F, 1, 0, 0);
            expectPixel(255, 0, 0, "control reproduces the missing depth-test defect");

            clear();
            wall();
            RenderSystem.disableDepthTest();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            RenderSystem.setShaderColor(0.2F, 0.3F, 0.4F, 0.5F);
            layer.startDrawing();
            if (!GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                    || GL11.glGetInteger(GL11.GL_DEPTH_FUNC) != GL11.GL_LEQUAL) {
                throw new AssertionError("The display layer must restore LEQUAL depth testing");
            }
            for (float channel : RenderSystem.getShaderColor()) {
                if (channel != 1.0F) {
                    throw new AssertionError("The display layer must preserve full-bright, untinted content");
                }
            }
            quad(0.5F, 1, 0, 0);
            layer.endDrawing();
            expectPixel(0, 255, 0, "opaque wall hides a display behind it");

            clear();
            wall();
            RenderSystem.disableDepthTest();
            layer.startDrawing();
            quad(-0.75F, 1, 0, 0);
            layer.endDrawing();
            expectPixel(255, 0, 0, "display in front of the wall remains visible");
            RenderSystem.enableDepthTest();
            quad(0.5F, 0, 0, 1);
            expectPixel(255, 0, 0, "display writes depth and hides later geometry behind it");
            DisplayRenderLayers.releaseBrowser(0);
            System.out.println("Display occlusion regression passed (control, occlusion, foreground, depth write, tint).");
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    private static void clear() {
        RenderSystem.depthMask(true);
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private static void wall() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        quad(-0.5F, 0, 1, 0);
    }

    private static void quad(float z, float red, float green, float blue) {
        // Fixed-function drawing isolates the layer's depth state from Minecraft
        // shader/resource setup; the production layer start/end phases are real.
        GL11.glColor3f(red, green, blue);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-1, -1, z);
        GL11.glVertex3f(1, -1, z);
        GL11.glVertex3f(1, 1, z);
        GL11.glVertex3f(-1, 1, z);
        GL11.glEnd();
    }

    private static void expectPixel(int red, int green, int blue, String message) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pixel = stack.malloc(4);
            GL11.glReadPixels(16, 16, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
            if ((pixel.get(0) & 255) != red || (pixel.get(1) & 255) != green || (pixel.get(2) & 255) != blue) {
                throw new AssertionError(message + ": unexpected framebuffer pixel");
            }
        }
    }
}
