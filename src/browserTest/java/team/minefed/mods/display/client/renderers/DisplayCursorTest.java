package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarFile;

/** Exercises production browser setup at the MCEF callback boundary, with no native window. */
public final class DisplayCursorTest {
    private static final String FACTORY = "team/minefed/mods/display/client/renderers/WorldDisplayBrowsers";

    public static void main(String[] args) throws Exception {
        verifyPinnedMcefCursorSideEffect();
        for (String renderer : new String[] { "CustomSizeDisplayBlockEntityRenderer", "TelevisionMonitorBlockEntityRenderer" }) {
            try (InputStream stream = DisplayCursorTest.class.getResourceAsStream(renderer + ".class")) {
                check(stream != null, "missing compiled renderer: " + renderer);
                int[] calls = countCalls(stream.readAllBytes(), null, FACTORY, "create");
                check(calls[0] == 1, renderer + " must use the passive factory exactly once");
                try (InputStream duplicate = DisplayCursorTest.class.getResourceAsStream(renderer + ".class")) {
                    check(countCalls(duplicate.readAllBytes(), null, "com/cinemamod/mcef/MCEF", "createBrowser")[0] == 0,
                            renderer + " bypasses passive setup");
                }
            }
        }

        // Negative control: unmodified MCEF callbacks release the captured pointer.
        MCEFBrowser interactive = new MCEFBrowser(MCEF.CLIENT, "https://example.test", false);
        interactive.setCloseAllowed();
        interactive.createImmediately();
        check(MCEFBrowser.windowCursorMode == MCEFBrowser.NORMAL, "negative control did not release the cursor");

        // Initialization failures still propagate; the factory must not invent a second init path.
        try {
            WorldDisplayBrowsers.create("https://example.test");
            throw new AssertionError("uninitialized MCEF unexpectedly succeeded");
        } catch (IllegalStateException expected) {
            check(MCEF.initializationChecks == 1, "factory skipped MCEF's initialization boundary");
        }
        MCEF.initialized = true;
        for (int initialMode : new int[] { MCEFBrowser.DISABLED, MCEFBrowser.NORMAL, MCEFBrowser.HIDDEN }) {
            MCEFBrowser.windowCursorMode = initialMode;
            MCEFBrowser.cursorWrites = 0;
            MCEFBrowser display = WorldDisplayBrowsers.create("https://example.test/display");
            check(display.client == MCEF.CLIENT && !display.transparent, "browser client or opacity changed");
            check(display.closeAllowed && display.creations == 1, "browser lifecycle changed");
            check(display.url.equals("https://example.test/display"), "initial URL changed");
            display.resize(1280, 805);
            display.loadURL("https://example.test/next");
            for (int cursor = 0; cursor < 50; cursor++) display.emitCursor(cursor);
            check(MCEFBrowser.windowCursorMode == initialMode && MCEFBrowser.cursorWrites == 0,
                    "world display modified cursor mode during creation, resize, navigation, or a callback");
            check(display.width == 1280 && display.height == 805, "resize changed");
            check(display.url.equals("https://example.test/next"), "navigation changed");
        }
        // The fix is local to world-display instances; interactive browsers keep their own listener.
        interactive.emitCursor(43);
        check(MCEFBrowser.windowCursorMode == MCEFBrowser.HIDDEN && MCEFBrowser.cursorWrites == 1,
                "interactive browser cursor handling changed globally");
        System.out.println("Display cursor regression passed: both renderers, creation callback, navigation, resize, all cursor IDs, interactive isolation.");
    }

    private static void verifyPinnedMcefCursorSideEffect() throws Exception {
        for (String path : System.getProperty("minefed.mcefClasspath").split(File.pathSeparator)) {
            if (!path.endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(path)) {
                var entry = jar.getJarEntry("com/cinemamod/mcef/MCEFBrowser.class");
                if (entry == null) continue;
                try (InputStream stream = jar.getInputStream(entry)) {
                    check(countCalls(stream.readAllBytes(), "setCursor", "org/lwjgl/glfw/GLFW", "glfwSetInputMode")[0] == 2,
                            "review fixture: pinned MCEF cursor behavior changed");
                }
                return;
            }
        }
        throw new AssertionError("pinned MCEF compile dependency was not found");
    }

    private static int[] countCalls(byte[] bytes, String method, String owner, String name) {
        int[] count = { 0 };
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                if (method != null && !method.equals(methodName)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String callOwner, String callName, String callDescriptor, boolean isInterface) {
                        if (callOwner.equals(owner) && callName.equals(name)) count[0]++;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return count;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
