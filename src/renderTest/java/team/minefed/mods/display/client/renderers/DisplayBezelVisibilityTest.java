package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.MCEFRenderer;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/** Uses Minecraft's frustum and the real renderer without opening a game or browser. */
public final class DisplayBezelVisibilityTest {
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static int cases;
    private static long vertices;

    public static void main(String[] args) throws Exception {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        for (Direction facing : FACINGS) {
            for (int[] size : new int[][] {{1, 1}, {1, 32}, {32, 1}, {8, 5}, {32, 32}, {40, 65}, {4097, 3}}) {
                for (BlockPos pos : new BlockPos[] {BlockPos.ORIGIN, new BlockPos(-590, 70, 52),
                        new BlockPos(29_999_000, -64, -29_999_000)}) {
                    ProbeEntity entity = new ProbeEntity(pos, facing, size[0], size[1]);
                    Box bounds = BrowserVisibility.bezelBounds(pos, facing, size[0], size[1]);
                    double cx = (bounds.minX + bounds.maxX) * 0.5;
                    double cy = (bounds.minY + bounds.maxY) * 0.5;
                    double cz = (bounds.minZ + bounds.maxZ) * 0.5;
                    // Camera inside the complete bounds, plus the no-frustum fallback.
                    render(entity, cube(cx, cy, cz), true, bounds);
                    render(entity, null, true, bounds);
                    // All six view boundaries: intersecting the edge must draw; fully outside must not.
                    for (int axis = 0; axis < 3; axis++) {
                        for (int sign : new int[] {-1, 1}) {
                            double edge = switch (axis) {
                                case 0 -> sign < 0 ? bounds.minX : bounds.maxX;
                                case 1 -> sign < 0 ? bounds.minY : bounds.maxY;
                                default -> sign < 0 ? bounds.minZ : bounds.maxZ;
                            };
                            for (boolean intersects : new boolean[] {true, false}) {
                                double[] camera = {cx, cy, cz};
                                camera[axis] = edge + sign * (intersects ? 0.995 : 1.05);
                                render(entity, cube(camera[0], camera[1], camera[2]), intersects, bounds);
                            }
                        }
                    }
                    // Only the top 0.05 blocks of bezel are in view. Browser content starts
                    // 0.1 blocks inward, so content-only bounds would incorrectly discard this.
                    render(entity, cube(cx, pos.getY() + 1.95, cz), true, bounds);
                }
            }
            for (int[] size : new int[][] {{0, 3}, {3, 0}, {-1, 3}, {3, -1}, {Integer.MIN_VALUE, 0}}) {
                render(new ProbeEntity(BlockPos.ORIGIN, facing, size[0], size[1]), null, false, null);
            }
        }
        verifyHiddenBlankUrlCleanup();
        System.out.println("Display bezel visibility passed: " + cases + " cases, " + vertices
                + " vertices; all facings, six frustum edges, bezel-only edge, camera inside, large NBT sizes,"
                + " distant world coordinates, empty geometry, fallback, hidden blank-URL browser cleanup,"
                + " and unchanged matrix stacks.");
    }

    private static void verifyHiddenBlankUrlCleanup() throws Exception {
        // Allocate a tracking subtype without MCEF's native/browser constructor. Only
        // its overridden texture getter and close callback are used by this regression.
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Map<BlockPos, MCEFBrowser> browsers = rendererMap("BROWSERS");
        Map<BlockPos, String> urls = rendererMap("URLS");
        Map<BlockPos, int[]> sizes = rendererMap("SIZES");
        Map<BlockPos, Long> lastInView = rendererMap("LAST_IN_VIEW");
        for (String blank : new String[] {"", null, "about:blank"}) {
            TrackingBrowser browser = (TrackingBrowser) unsafe.allocateInstance(TrackingBrowser.class);
            ProbeEntity entity = new ProbeEntity(BlockPos.ORIGIN, Direction.SOUTH, 32, 32);
            entity.url = blank;
            browsers.put(BlockPos.ORIGIN, browser);
            urls.put(BlockPos.ORIGIN, "https://example.test/previous");
            sizes.put(BlockPos.ORIGIN, new int[] {32, 32});
            lastInView.put(BlockPos.ORIGIN, 123L);
            RenderLayer previousLayer = DisplayRenderLayers.browser(-5151);
            render(entity, cube(0, 0, 1000), false,
                    BrowserVisibility.bezelBounds(BlockPos.ORIGIN, Direction.SOUTH, 32, 32));
            check(browser.closes == 1, "hidden blank-URL browser was not closed exactly once");
            check(!browsers.containsKey(BlockPos.ORIGIN) && !urls.containsKey(BlockPos.ORIGIN)
                    && !sizes.containsKey(BlockPos.ORIGIN), "hidden blank-URL browser caches were retained");
            check(DisplayRenderLayers.browser(-5151) != previousLayer, "browser render layer was retained");
            // The pre-existing blank-URL branch does not update/remove the last-view timestamp.
            check(lastInView.get(BlockPos.ORIGIN) == 123L, "blank-URL last-view semantics changed");
            DisplayRenderLayers.releaseBrowser(-5151);
            CustomSizeDisplayBlockEntityRenderer.closeAll();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<BlockPos, T> rendererMap(String name) throws Exception {
        Field field = CustomSizeDisplayBlockEntityRenderer.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<BlockPos, T>) field.get(null);
    }

    private static final class TrackingBrowser extends MCEFBrowser {
        private static final MCEFRenderer RENDERER = new MCEFRenderer(false) {
            @Override public int getTextureID() { return -5151; }
        };
        int closes;

        private TrackingBrowser() { super(null, "", false); }
        @Override public MCEFRenderer getRenderer() { return RENDERER; }
        @Override public void close() { closes++; }
        @Override protected void finalize() { /* No native state was allocated. */ }
    }

    private static Frustum cube(double x, double y, double z) {
        Frustum frustum = new Frustum(new Matrix4f(), new Matrix4f().ortho(-1, 1, -1, 1, -1, 1));
        frustum.setPosition(x, y, z);
        return frustum;
    }

    private static void render(ProbeEntity entity, Frustum frustum, boolean visible, Box bounds) {
        CustomSizeDisplayBlockEntityRenderer renderer = new CustomSizeDisplayBlockEntityRenderer(null, () -> frustum);
        MatrixStack matrices = new MatrixStack();
        // Minecraft renders with camera-relative float matrices. Keep a fractional camera
        // offset, even near the world border, then restore world coordinates in the recorder.
        double cameraX = entity.getPos().getX() + 50.125;
        double cameraY = entity.getPos().getY() - 10.5;
        double cameraZ = entity.getPos().getZ() + 80.25;
        matrices.translate(-50.125, 10.5, -80.25);
        Matrix4f beforePosition = new Matrix4f(matrices.peek().getPositionMatrix());
        Matrix3f beforeNormal = new Matrix3f(matrices.peek().getNormalMatrix());
        BoundsRecorder recorder = new BoundsRecorder(bounds, cameraX, cameraY, cameraZ);
        renderer.render(entity, 0.5F, matrices, recorder, 0, 0);
        long expected = visible ? 4L * entity.width * entity.height : 0;
        check(recorder.vertices == expected, "unexpected emitted vertex count: " + recorder.vertices + " != " + expected);
        check(visible || recorder.layers == 0, "hidden bezel requested a vertex buffer");
        check(matrices.isEmpty()
                        && Arrays.equals(beforePosition.get(new float[16]), matrices.peek().getPositionMatrix().get(new float[16]))
                        && Arrays.equals(beforeNormal.get(new float[9]), matrices.peek().getNormalMatrix().get(new float[9])),
                "matrix stack changed");
        cases++;
        vertices += recorder.vertices;
    }

    private static final class ProbeEntity extends CustomSizeDisplayBlockEntity {
        final int width;
        final int height;
        String url = "about:blank";

        ProbeEntity(BlockPos pos, Direction facing, int width, int height) {
            super(pos, DisplayBezelTraceTest.state(facing));
            this.width = width;
            this.height = height;
        }

        @Override public int getDisplayWidth() { return width; }
        @Override public int getDisplayHeight() { return height; }
        @Override public String getUrl() { return url; }
    }

    private static final class BoundsRecorder implements VertexConsumerProvider, VertexConsumer {
        final Box bounds;
        final double cameraX;
        final double cameraY;
        final double cameraZ;
        long vertices;
        int layers;

        BoundsRecorder(Box bounds, double cameraX, double cameraY, double cameraZ) {
            this.bounds = bounds;
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
        }

        @Override public VertexConsumer getBuffer(RenderLayer layer) { layers++; return this; }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            x += cameraX;
            y += cameraY;
            z += cameraZ;
            if (bounds == null || x < bounds.minX || x > bounds.maxX
                    || y < bounds.minY || y > bounds.maxY || z < bounds.minZ || z > bounds.maxZ) {
                throw new AssertionError("emitted vertex lies outside the culling bounds: "
                        + x + ", " + y + ", " + z + " vs " + bounds);
            }
            return this;
        }

        @Override public VertexConsumer texture(float u, float v) { return this; }
        @Override public void next() { vertices++; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { throw new AssertionError(); }
        @Override public VertexConsumer overlay(int u, int v) { throw new AssertionError(); }
        @Override public VertexConsumer light(int u, int v) { throw new AssertionError(); }
        @Override public VertexConsumer normal(float x, float y, float z) { throw new AssertionError(); }
        @Override public void fixedColor(int r, int g, int b, int a) { throw new AssertionError(); }
        @Override public void unfixColor() { throw new AssertionError(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
