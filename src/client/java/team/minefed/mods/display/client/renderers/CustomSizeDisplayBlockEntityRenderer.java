package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.AbstractTexture;
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
        renderBezel(entity, matrices, width, height);

        if (url == null || url.isEmpty() || "about:blank".equals(url)) {
            if (BROWSERS.containsKey(pos)) {
                BROWSERS.remove(pos).close();
                URLS.remove(pos);
                SIZES.remove(pos);
            }
            return;
        }

        MCEFBrowser browser = BROWSERS.get(pos);
        int[] cachedSize = SIZES.get(pos);

        boolean needsResize = cachedSize == null || cachedSize[0] != width || cachedSize[1] != height;

        if (browser == null) {
            browser = MCEF.createBrowser(url, false);
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
            renderBrowserContent(entity, matrices, width, height, textureId);
        }
    }

    private void renderBezel(CustomSizeDisplayBlockEntity entity, MatrixStack matrices, int width, int height) {
        matrices.push();
        // Same transform as TelevisionMonitorBlockEntityRenderer
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -entity.getCachedState().get(CustomSizeDisplayBlock.FACING).asRotation()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.translate(-0.5, -0.5, -0.001);

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Render each block position with appropriate texture
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                Identifier texture = getBezelTexture(dx, dy, width, height);
                bindTexture(texture);

                // After 180 degree X rotation, Y is flipped, so we adjust coordinates
                float left = dx;
                float right = dx + 1;
                float top = 1 + dy;
                float bottom = dy;

                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                bufferBuilder.vertex(matrix4f, left, top, 0).texture(0, 1).next();
                bufferBuilder.vertex(matrix4f, right, top, 0).texture(1, 1).next();
                bufferBuilder.vertex(matrix4f, right, bottom, 0).texture(1, 0).next();
                bufferBuilder.vertex(matrix4f, left, bottom, 0).texture(0, 0).next();
                Tessellator.getInstance().draw();
            }
        }

        RenderSystem.disableBlend();
        matrices.pop();
    }

    private Identifier getBezelTexture(int x, int y, int width, int height) {
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

    private void bindTexture(Identifier textureId) {
        AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
        RenderSystem.setShaderTexture(0, texture.getGlId());
    }

    private void renderBrowserContent(CustomSizeDisplayBlockEntity entity, MatrixStack matrices,
            int width, int height, int textureId) {
        matrices.push();
        // Same transform as TelevisionMonitorBlockEntityRenderer
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -entity.getCachedState().get(CustomSizeDisplayBlock.FACING).asRotation()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.translate(-0.5, -0.5, -0.002);

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureId);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

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
        Tessellator.getInstance().draw();

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
        BROWSERS.values().forEach(MCEFBrowser::close);
        BROWSERS.clear();
        URLS.clear();
        SIZES.clear();
    }

    public static void closeBrowser(BlockPos pos) {
        MCEFBrowser browser = BROWSERS.remove(pos);
        if (browser != null) {
            browser.close();
        }
        URLS.remove(pos);
        SIZES.remove(pos);
    }
}
