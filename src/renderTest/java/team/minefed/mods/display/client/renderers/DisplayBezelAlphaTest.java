package team.minefed.mods.display.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Differential framebuffer check with a transparent/partially transparent resource fixture. */
final class DisplayBezelAlphaTest {
    static void run() throws IOException {
        int texture = loadAlphaTexture();
        try {
            int cases = 0;
            for (Direction facing : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                for (int[] size : new int[][] {{1, 1}, {1, 5}, {2, 1}, {2, 5}, {5, 1}, {8, 5}, {32, 32}, {41, 7}}) {
                    CustomSizeDisplayBlockEntity entity = new CustomSizeDisplayBlockEntity(BlockPos.ORIGIN,
                            DisplayBezelTraceTest.state(facing));
                    NbtCompound nbt = new NbtCompound();
                    nbt.putString("url", "about:blank");
                    nbt.putInt("displayWidth", size[0]);
                    nbt.putInt("displayHeight", size[1]);
                    entity.readNbt(nbt);
                    byte[] expected = render(entity, facing, texture, true);
                    byte[] actual = render(entity, facing, texture, false);
                    if (!Arrays.equals(actual, expected)) {
                        throw new AssertionError("Alpha bezel framebuffer differs: " + size[0] + "x" + size[1]
                                + " " + facing + " byte " + Arrays.mismatch(actual, expected));
                    }
                    if (size[0] == 1 && size[1] == 1) {
                        expectColor(actual, 40, 80, 120, "transparent texel keeps the background");
                        expectColor(actual, 27, 138, 80, "one-third-alpha texel blends");
                        expectColor(actual, 13, 27, 210, "two-thirds-alpha texel blends");
                        expectColor(actual, 255, 255, 255, "opaque texel covers the background");
                    }
                    cases++;
                }
            }
            if (GL11.glGetError() != GL11.GL_NO_ERROR) {
                throw new AssertionError("OpenGL error in the alpha bezel regression");
            }
            System.out.println("Display bezel alpha framebuffer passed: " + cases
                    + " baseline/production pairs, all horizontal facings, transparent/partial/opaque texels.");
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glDeleteTextures(texture);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            RenderSystem.disableBlend();
        }
    }

    private static byte[] render(CustomSizeDisplayBlockEntity entity, Direction facing, int texture, boolean legacy) {
        int width = entity.getDisplayWidth();
        int height = entity.getDisplayHeight();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-0.25, width + 0.25, 0.75 - height, 1.25, -10, 10);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_CULL_FACE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glClearColor(40 / 255.0F, 80 / 255.0F, 120 / 255.0F, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // View each horizontal facing head-on without modifying the renderer's
        // own transforms. The same parent matrices are used for both renderers.
        MatrixStack matrices = new MatrixStack();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));
        matrices.translate(-0.5, -0.5, -0.5);
        FixedFunctionConsumer consumer = new FixedFunctionConsumer();
        if (legacy) {
            LegacyBezelRenderer.render(entity, matrices, consumer, width, height);
        } else {
            new CustomSizeDisplayBlockEntityRenderer(null).render(entity, 0, matrices, consumer, 0, 0);
        }
        consumer.finish();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pixels = stack.malloc(32 * 32 * 4);
            GL11.glReadPixels(0, 0, 32, 32, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            byte[] result = new byte[pixels.remaining()];
            pixels.get(result);
            return result;
        }
    }

    private static int loadAlphaTexture() throws IOException {
        BufferedImage image;
        try (InputStream input = DisplayBezelAlphaTest.class.getResourceAsStream("/bezel-alpha.png")) {
            if (input == null || (image = ImageIO.read(input)) == null) {
                throw new IOException("Missing alpha bezel resource fixture");
            }
        }
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pixels = stack.malloc(image.getWidth() * image.getHeight() * 4);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >> 24));
                }
            }
            pixels.flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(),
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        }
        return texture;
    }

    private static void expectColor(byte[] pixels, int red, int green, int blue, String message) {
        for (int i = 0; i < pixels.length; i += 4) {
            if (Math.abs((pixels[i] & 255) - red) <= 1
                    && Math.abs((pixels[i + 1] & 255) - green) <= 1
                    && Math.abs((pixels[i + 2] & 255) - blue) <= 1) {
                return;
            }
        }
        throw new AssertionError(message + ": expected color was not drawn");
    }

    /**
     * Like the existing occlusion check, fixed-function drawing isolates geometry
     * from Minecraft's shader/resource-manager setup. Both renderers use the same
     * alpha fixture and blending/depth state; the headless trace separately checks
     * every production texture/layer identity and call, not just visible pixels.
     */
    private static final class FixedFunctionConsumer implements VertexConsumerProvider, VertexConsumer {
        private boolean drawing;
        private double x;
        private double y;
        private double z;
        private float u;
        private float v;

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            finish();
            GL11.glBegin(GL11.GL_QUADS);
            drawing = true;
            return this;
        }

        void finish() {
            if (drawing) {
                GL11.glEnd();
                drawing = false;
            }
        }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
        @Override public VertexConsumer texture(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public void next() { GL11.glTexCoord2f(u, v); GL11.glVertex3d(x, y, z); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { throw unexpected(); }
        @Override public VertexConsumer overlay(int u, int v) { throw unexpected(); }
        @Override public VertexConsumer light(int u, int v) { throw unexpected(); }
        @Override public VertexConsumer normal(float x, float y, float z) { throw unexpected(); }
        @Override public void fixedColor(int r, int g, int b, int a) { throw unexpected(); }
        @Override public void unfixColor() { throw unexpected(); }
        private AssertionError unexpected() { return new AssertionError("Unexpected bezel vertex operation"); }
    }
}
