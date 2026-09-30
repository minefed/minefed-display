package team.minefed.mods.display.client.renderers;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;

/**
 * Preserved bezel algorithm from 8b4cd45ba84560051d021dcacdee747b5a795e03.
 * Only its entry-point name and static modifiers differ; do not optimize this oracle.
 */
final class LegacyBezelRenderer {
    // Bezel textures
    private static final Identifier TEX_LEFT_UPPER = new Identifier("minefed-display",
            "textures/block/television_monitor/front_left_upper.png");
    private static final Identifier TEX_CENTER_UPPER = new Identifier("minefed-display",
            "textures/block/television_monitor/front_center_upper.png");
    private static final Identifier TEX_RIGHT_UPPER = new Identifier("minefed-display",
            "textures/block/television_monitor/front_right_upper.png");
    private static final Identifier TEX_LEFT_LOWER = new Identifier("minefed-display",
            "textures/block/television_monitor/front_left_lower.png");
    private static final Identifier TEX_CENTER_LOWER = new Identifier("minefed-display",
            "textures/block/television_monitor/front_center_lower.png");
    private static final Identifier TEX_RIGHT_LOWER = new Identifier("minefed-display",
            "textures/block/television_monitor/front_right_lower.png");

    static void render(CustomSizeDisplayBlockEntity entity, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int width, int height) {
        matrices.push();
        // Same transform as TelevisionMonitorBlockEntityRenderer
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -entity.getCachedState().get(CustomSizeDisplayBlock.FACING).asRotation()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.translate(-0.5, -0.5, -0.001);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // The vertex provider draws the pending layer whenever another layer is
        // requested, so emit all quads of one texture together. The quads tile the
        // plane without overlapping, which makes their draw order irrelevant. Textures
        // are visited in first-use order; the bottom-right texture is unique to its
        // quad, so the layer left pending for the next caller is unchanged.
        Identifier[] textures = new Identifier[6];
        int textureCount = 0;
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                Identifier texture = getBezelTexture(dx, dy, width, height);
                if (indexOf(textures, textureCount, texture) < 0) {
                    textures[textureCount++] = texture;
                }
            }
        }

        // Render each block position with appropriate texture
        for (int i = 0; i < textureCount; i++) {
            Identifier texture = textures[i];
            VertexConsumer bufferBuilder = vertexConsumers.getBuffer(DisplayRenderLayers.bezel(texture));

            for (int dx = 0; dx < width; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    if (getBezelTexture(dx, dy, width, height) != texture) {
                        continue;
                    }

                    // After 180 degree X rotation, Y is flipped, so we adjust coordinates
                    float left = dx;
                    float right = dx + 1;
                    float top = 1 + dy;
                    float bottom = dy;

                    bufferBuilder.vertex(matrix4f, left, top, 0).texture(0, 1).next();
                    bufferBuilder.vertex(matrix4f, right, top, 0).texture(1, 1).next();
                    bufferBuilder.vertex(matrix4f, right, bottom, 0).texture(1, 0).next();
                    bufferBuilder.vertex(matrix4f, left, bottom, 0).texture(0, 0).next();
                }
            }
        }

        matrices.pop();
    }

    private static int indexOf(Identifier[] textures, int count, Identifier texture) {
        for (int i = 0; i < count; i++) {
            if (textures[i] == texture) {
                return i;
            }
        }
        return -1;
    }

    private static Identifier getBezelTexture(int x, int y, int width, int height) {
        boolean isLeft = (x == 0);
        boolean isRight = (x == width - 1);
        boolean isTop = (y == 0);
        boolean isBottom = (y == height - 1);

        // For single-width displays
        if (width == 1) {
            isLeft = true;
            isRight = true;
        }
        // For single-height displays
        if (height == 1) {
            isTop = true;
            isBottom = true;
        }

        if (isTop) {
            if (isLeft)
                return TEX_LEFT_UPPER;
            if (isRight)
                return TEX_RIGHT_UPPER;
            return TEX_CENTER_UPPER;
        } else if (isBottom) {
            if (isLeft)
                return TEX_LEFT_LOWER;
            if (isRight)
                return TEX_RIGHT_LOWER;
            return TEX_CENTER_LOWER;
        } else {
            // Middle rows
            if (isLeft)
                return TEX_LEFT_UPPER;
            if (isRight)
                return TEX_RIGHT_UPPER;
            return TEX_CENTER_UPPER;
        }
    }

}
