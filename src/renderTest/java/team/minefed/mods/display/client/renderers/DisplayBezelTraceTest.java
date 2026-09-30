package team.minefed.mods.display.client.renderers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;
import team.minefed.mods.display.blocks.DisplayModBlocks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bit-exact, ordered comparisons through the production renderer's public entry point. */
public final class DisplayBezelTraceTest {
    private static final Direction[] FACINGS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final String[] EMPTY_URLS = {"", null, "about:blank"};
    private static final CustomSizeDisplayBlockEntityRenderer RENDERER =
            new CustomSizeDisplayBlockEntityRenderer(null);
    private static int cases;
    private static long vertices;

    public static void main(String[] args) throws Exception {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        RenderLayer pending = DisplayRenderLayers.browser(-1);
        for (Direction facing : FACINGS) {
            ProbeEntity entity = new ProbeEntity(facing);
            for (int width = 0; width <= 40; width++) {
                for (int height = 0; height <= 40; height++) {
                    entity.width = width;
                    entity.height = height;
                    entity.url = EMPTY_URLS[(width + height) % EMPTY_URLS.length];
                    compare(entity, false, pending);
                    compare(entity, true, pending);
                }
            }
            for (int[] size : new int[][] {
                    {-1, 4}, {4, -1}, {-1, -1}, {Integer.MIN_VALUE, 0}, {0, Integer.MIN_VALUE},
                    {Integer.MIN_VALUE, 3}, {3, Integer.MIN_VALUE},
                    {0, Integer.MAX_VALUE}
            }) {
                entity.width = size[0];
                entity.height = size[1];
                compare(entity, true, pending);
            }
            // readNbt intentionally has no upper clamp: exercise real entities,
            // rather than the size setter (which clamps to 32).
            for (int[] size : new int[][] {
                    {41, 1}, {1, 65}, {2, 129}, {129, 2}, {65, 97}, {257, 129}, {4097, 3}, {3, 4097}
            }) {
                CustomSizeDisplayBlockEntity loaded = new CustomSizeDisplayBlockEntity(BlockPos.ORIGIN,
                        state(facing));
                NbtCompound nbt = new NbtCompound();
                nbt.putString("url", "about:blank");
                nbt.putInt("displayWidth", size[0]);
                nbt.putInt("displayHeight", size[1]);
                loaded.readNbt(nbt);
                if (loaded.getDisplayWidth() != size[0] || loaded.getDisplayHeight() != size[1]) {
                    throw new AssertionError("The large-size test must exercise uncapped NBT dimensions");
                }
                compare(loaded, false, pending);
                compare(loaded, true, pending);
            }
        }
        verifyIntegerCoordinateBoundaries(pending);
        DisplayRenderLayers.releaseBrowser(-1);
        System.out.println("Display bezel trace passed: " + cases + " cases, " + vertices
                + " vertices; exact layer requests, vertex/UV raw bits, call order, pending layer, and matrices.");
    }

    private static void compare(CustomSizeDisplayBlockEntity entity, boolean transformed, RenderLayer pending) {
        int width = entity.getDisplayWidth();
        int height = entity.getDisplayHeight();
        String label = width + "x" + height + " " + entity.getCachedState().get(CustomSizeDisplayBlock.FACING)
                + (transformed ? " transformed" : " identity");
        MatrixStack baselineMatrices = matrices(transformed);
        MatrixStack actualMatrices = matrices(transformed);
        Matrix4f positionBefore = new Matrix4f(actualMatrices.peek().getPositionMatrix());
        Matrix3f normalBefore = new Matrix3f(actualMatrices.peek().getNormalMatrix());
        Trace baseline = new Trace(pending);
        Trace actual = new Trace(pending);
        LegacyBezelRenderer.render(entity, baselineMatrices, baseline, width, height);
        RENDERER.render(entity, 0.375F, actualMatrices, actual, 0x123456, 0x654321);
        actual.assertMatches(baseline, label);
        assertMatrixRestored(actualMatrices, positionBefore, normalBefore, label);
        assertMatrixRestored(baselineMatrices, positionBefore, normalBefore, label + " baseline");
        long expectedVertices = width > 0 && height > 0 ? 4L * width * height : 0;
        if (actual.vertexCount != expectedVertices) {
            throw new AssertionError(label + ": expected " + expectedVertices + " vertices, got " + actual.vertexCount);
        }
        int expectedGroups = width <= 0 || height <= 0 ? 0
                : Math.min(width, 3) * (height == 1 ? 1 : 2);
        if (actual.layers.size() != expectedGroups) {
            throw new AssertionError(label + ": incorrect texture-group count");
        }
        cases++;
        vertices += actual.vertexCount;
    }

    private static MatrixStack matrices(boolean transformed) {
        MatrixStack matrices = new MatrixStack();
        if (transformed) {
            matrices.translate(-1234.567890123, 98.7654321, -0.00003125);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(31.25F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-17.75F));
            matrices.scale(-1.125F, 0.875F, 1.0625F);
        }
        return matrices;
    }

    private static void assertMatrixRestored(MatrixStack matrices, Matrix4f position, Matrix3f normal, String label) {
        if (!matrices.isEmpty()
                || !rawEquals(position.get(new float[16]), matrices.peek().getPositionMatrix().get(new float[16]))
                || !rawEquals(normal.get(new float[9]), matrices.peek().getNormalMatrix().get(new float[9]))) {
            throw new AssertionError(label + ": matrix stack was not restored exactly");
        }
    }

    private static boolean rawEquals(float[] first, float[] second) {
        for (int i = 0; i < first.length; i++) {
            if (Float.floatToRawIntBits(first[i]) != Float.floatToRawIntBits(second[i])) {
                return false;
            }
        }
        return true;
    }

    private static void verifyIntegerCoordinateBoundaries(RenderLayer pending) throws Exception {
        // Full INT_MAX-sized grids are not practical to enumerate in a regression
        // test. Probe small ranges of the actual production emitter where changing
        // int addition into float addition would change vertex bits.
        Method group = CustomSizeDisplayBlockEntityRenderer.class.getDeclaredMethod("renderBezelGroup",
                Matrix4f.class, VertexConsumerProvider.class, int.class, int.class, int.class, int.class,
                Identifier.class);
        group.setAccessible(true);
        Identifier texture = new Identifier("minefed-display", "textures/block/television_monitor/front_left_upper.png");
        for (int coordinate : new int[] {16_777_215, 16_777_216, 16_777_217, Integer.MAX_VALUE - 2}) {
            for (boolean transformed : new boolean[] {false, true}) {
                Matrix4f matrix = matrices(transformed).peek().getPositionMatrix();
                Trace baseline = new Trace(pending);
                Trace actual = new Trace(pending);
                VertexConsumer buffer = baseline.getBuffer(DisplayRenderLayers.bezel(texture));
                for (int dx = coordinate; dx < coordinate + 2; dx++) {
                    for (int dy = coordinate; dy < coordinate + 2; dy++) {
                        // Original emitter, including integer addition before conversion.
                        float left = dx;
                        float right = dx + 1;
                        float top = 1 + dy;
                        float bottom = dy;
                        buffer.vertex(matrix, left, top, 0).texture(0, 1).next();
                        buffer.vertex(matrix, right, top, 0).texture(1, 1).next();
                        buffer.vertex(matrix, right, bottom, 0).texture(1, 0).next();
                        buffer.vertex(matrix, left, bottom, 0).texture(0, 0).next();
                    }
                }
                group.invoke(null, matrix, actual, coordinate, coordinate + 2, coordinate, coordinate + 2, texture);
                actual.assertMatches(baseline, "integer boundary " + coordinate + " transformed=" + transformed);
                cases++;
                vertices += actual.vertexCount;
            }
        }
    }

    static BlockState state(Direction facing) {
        return DisplayModBlocks.CUSTOM_SIZE_DISPLAY.getDefaultState().with(CustomSizeDisplayBlock.FACING, facing);
    }

    /** Only bypasses the size setter's clamp to cover zero/negative renderer inputs. */
    private static final class ProbeEntity extends CustomSizeDisplayBlockEntity {
        int width;
        int height;
        String url = "";

        ProbeEntity(Direction facing) {
            super(BlockPos.ORIGIN, state(facing));
        }

        @Override public int getDisplayWidth() { return width; }
        @Override public int getDisplayHeight() { return height; }
        @Override public String getUrl() { return url; }
    }

    /**
     * Captures every call in order, not a set/hash/sample of geometry. The real
     * VertexConsumer default method applies each production position matrix before
     * vertex(double,double,double) records the exact float bits received by a buffer.
     */
    private static final class Trace implements VertexConsumerProvider, VertexConsumer {
        private final IntArrayList calls = new IntArrayList();
        private final List<RenderLayer> layers = new ArrayList<>();
        private RenderLayer pending;
        private int vertexCount;

        Trace(RenderLayer pending) {
            this.pending = pending;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            calls.add(1);
            calls.add(layers.size());
            layers.add(layer);
            pending = layer;
            return this;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            calls.add(2);
            calls.add(Float.floatToRawIntBits((float) x));
            calls.add(Float.floatToRawIntBits((float) y));
            calls.add(Float.floatToRawIntBits((float) z));
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            calls.add(3);
            calls.add(Float.floatToRawIntBits(u));
            calls.add(Float.floatToRawIntBits(v));
            return this;
        }

        @Override
        public void next() {
            calls.add(4);
            vertexCount++;
        }

        void assertMatches(Trace expected, String label) {
            if (layers.size() != expected.layers.size()) {
                throw new AssertionError(label + ": layer-request count changed");
            }
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i) != expected.layers.get(i)) {
                    throw new AssertionError(label + ": texture/layer identity changed at request " + i);
                }
            }
            int difference = Arrays.mismatch(calls.elements(), 0, calls.size(),
                    expected.calls.elements(), 0, expected.calls.size());
            if (difference >= 0) {
                throw new AssertionError(label + ": command/raw-bit mismatch at element " + difference);
            }
            if (pending != expected.pending) {
                throw new AssertionError(label + ": final pending layer changed");
            }
        }

        @Override public VertexConsumer color(int r, int g, int b, int a) { throw unexpected(); }
        @Override public VertexConsumer overlay(int u, int v) { throw unexpected(); }
        @Override public VertexConsumer light(int u, int v) { throw unexpected(); }
        @Override public VertexConsumer normal(float x, float y, float z) { throw unexpected(); }
        @Override public void fixedColor(int r, int g, int b, int a) { throw unexpected(); }
        @Override public void unfixColor() { throw unexpected(); }

        private AssertionError unexpected() {
            return new AssertionError("Unexpected vertex operation in the bezel stream");
        }
    }
}
