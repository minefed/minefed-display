package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class CustomSizeDisplayBlockEntityRenderer implements BlockEntityRenderer<CustomSizeDisplayBlockEntity> {

    private static final Map<BlockPos, MCEFBrowser> BROWSERS = new HashMap<>();
    private static final Map<BlockPos, String> URLS = new HashMap<>();
    private static final Map<BlockPos, int[]> SIZES = new HashMap<>();
    private static final Map<BlockPos, Long> LAST_IN_VIEW = new HashMap<>();

    // Pixels per block for browser resolution
    private static final int PIXELS_PER_BLOCK = 400;

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

    public CustomSizeDisplayBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(CustomSizeDisplayBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        String url = entity.getUrl();
        BlockPos pos = entity.getPos();
        int width = entity.getDisplayWidth();
        int height = entity.getDisplayHeight();

        // Always render bezel, even without URL
        renderBezel(entity, matrices, vertexConsumers, width, height);

        if (url == null || url.isEmpty() || "about:blank".equals(url)) {
            if (BROWSERS.containsKey(pos)) {
                closeBrowser(BROWSERS.remove(pos));
                URLS.remove(pos);
                SIZES.remove(pos);
            }
            return;
        }

        // A display outside the view frustum draws nothing; do not create or keep a browser for it
        if (!BrowserVisibility.isInView(pos, Math.max(width, height))) {
            return;
        }
        LAST_IN_VIEW.put(pos, System.currentTimeMillis());

        MCEFBrowser browser = BROWSERS.get(pos);
        int[] cachedSize = SIZES.get(pos);

        boolean needsResize = cachedSize == null || cachedSize[0] != width || cachedSize[1] != height;

        if (browser == null) {
            browser = WorldDisplayBrowsers.create(url);
            browser.resize(width * PIXELS_PER_BLOCK, height * PIXELS_PER_BLOCK);
            BROWSERS.put(pos, browser);
            URLS.put(pos, url);
            SIZES.put(pos, new int[] { width, height });
        } else {
            String currentUrl = URLS.get(pos);

            if (!url.equals(currentUrl)) {
                browser.loadURL(url);
                URLS.put(pos, url);
            }

            if (needsResize) {
                browser.resize(width * PIXELS_PER_BLOCK, height * PIXELS_PER_BLOCK);
                SIZES.put(pos, new int[] { width, height });
            }
        }

        int textureId = browser.getRenderer().getTextureID();

        if (textureId != 0) {
            renderBrowserContent(entity, matrices, vertexConsumers, width, height, textureId);
        }
    }

    private void renderBezel(CustomSizeDisplayBlockEntity entity, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int width, int height) {
        matrices.push();
        // Same transform as TelevisionMonitorBlockEntityRenderer
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -entity.getCachedState().get(CustomSizeDisplayBlock.FACING).asRotation()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.translate(-0.5, -0.5, -0.001);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Visit the existing texture groups directly, in their original first-use
        // order. Keep every quad and the x-outer/y-inner order within each group,
        // including the final layer left pending in the vertex provider.
        if (width > 0 && height > 0) {
            renderBezelColumn(matrix4f, vertexConsumers, 0, 1, height, TEX_LEFT_UPPER, TEX_LEFT_LOWER);
            if (width > 2) {
                renderBezelColumn(matrix4f, vertexConsumers, 1, width - 1, height,
                        TEX_CENTER_UPPER, TEX_CENTER_LOWER);
            }
            if (width > 1) {
                renderBezelColumn(matrix4f, vertexConsumers, width - 1, width, height,
                        TEX_RIGHT_UPPER, TEX_RIGHT_LOWER);
            }
        }

        matrices.pop();
    }

    private static void renderBezelColumn(Matrix4f matrix, VertexConsumerProvider vertexConsumers,
            int startX, int endX, int height, Identifier upper, Identifier lower) {
        // A single row uses the upper texture; a single column uses the left pair.
        int upperEndY = height == 1 ? 1 : height - 1;
        renderBezelGroup(matrix, vertexConsumers, startX, endX, 0, upperEndY, upper);
        if (height > 1) {
            renderBezelGroup(matrix, vertexConsumers, startX, endX, height - 1, height, lower);
        }
    }

    private static void renderBezelGroup(Matrix4f matrix, VertexConsumerProvider vertexConsumers,
            int startX, int endX, int startY, int endY, Identifier texture) {
        VertexConsumer bufferBuilder = vertexConsumers.getBuffer(DisplayRenderLayers.bezel(texture));
        for (int dx = startX; dx < endX; dx++) {
            for (int dy = startY; dy < endY; dy++) {
                // Preserve integer addition before converting coordinates to float.
                float left = dx;
                float right = dx + 1;
                float top = 1 + dy;
                float bottom = dy;

                bufferBuilder.vertex(matrix, left, top, 0).texture(0, 1).next();
                bufferBuilder.vertex(matrix, right, top, 0).texture(1, 1).next();
                bufferBuilder.vertex(matrix, right, bottom, 0).texture(1, 0).next();
                bufferBuilder.vertex(matrix, left, bottom, 0).texture(0, 0).next();
            }
        }
    }

    private void renderBrowserContent(CustomSizeDisplayBlockEntity entity, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int width, int height, int textureId) {
        matrices.push();
        // Same transform as TelevisionMonitorBlockEntityRenderer
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -entity.getCachedState().get(CustomSizeDisplayBlock.FACING).asRotation()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.translate(-0.5, -0.5, -0.002);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        VertexConsumer bufferBuilder = vertexConsumers.getBuffer(DisplayRenderLayers.browser(textureId));

        // Calculate render bounds with margin for bezel
        float margin = 0.1f;
        float left = margin;
        float right = width - margin;
        // After 180 degree X rotation, coordinates are adjusted
        float top = height - margin;
        float bottom = margin;

        // Use same texture coordinate pattern as TV renderer
        bufferBuilder.vertex(matrix4f, left, top, 0).texture(0, 1).next();
        bufferBuilder.vertex(matrix4f, right, top, 0).texture(1, 1).next();
        bufferBuilder.vertex(matrix4f, right, bottom, 0).texture(1, 0).next();
        bufferBuilder.vertex(matrix4f, left, bottom, 0).texture(0, 0).next();

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(CustomSizeDisplayBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }

    public static void closeAll() {
        BROWSERS.values().forEach(CustomSizeDisplayBlockEntityRenderer::closeBrowser);
        BROWSERS.clear();
        URLS.clear();
        SIZES.clear();
        LAST_IN_VIEW.clear();
    }

    /** Closes browsers of displays that were not in view for a while; they reload when seen again. */
    public static void closeIdle(long currentMillis) {
        BROWSERS.keySet().removeIf(pos -> {
            final Long lastInView = LAST_IN_VIEW.get(pos);
            if (lastInView != null && currentMillis - lastInView <= BrowserVisibility.IDLE_MILLIS) {
                return false;
            }
            closeBrowser(BROWSERS.get(pos));
            URLS.remove(pos);
            SIZES.remove(pos);
            LAST_IN_VIEW.remove(pos);
            return true;
        });
    }

    public static void closeBrowser(BlockPos pos) {
        MCEFBrowser browser = BROWSERS.remove(pos);
        if (browser != null) {
            closeBrowser(browser);
        }
        URLS.remove(pos);
        SIZES.remove(pos);
        LAST_IN_VIEW.remove(pos);
    }

    private static void closeBrowser(MCEFBrowser browser) {
        DisplayRenderLayers.releaseBrowser(browser.getRenderer().getTextureID());
        browser.close();
    }
}
