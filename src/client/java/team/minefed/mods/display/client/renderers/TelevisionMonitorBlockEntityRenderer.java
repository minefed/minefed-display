package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import team.minefed.mods.display.blocks.TelevisionMonitorBlock;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class TelevisionMonitorBlockEntityRenderer implements BlockEntityRenderer<TelevisionMonitorBlockEntity> {

    private static final Map<BlockPos, MCEFBrowser> BROWSERS = new HashMap<>();
    private static final Map<BlockPos, String> URLS = new HashMap<>();
    private static final Map<BlockPos, Long> LAST_IN_VIEW = new HashMap<>();

    public TelevisionMonitorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(TelevisionMonitorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        String url = entity.getUrl();
        BlockPos pos = entity.getPos();

        if (url == null || url.isEmpty() || "about:blank".equals(url)) {
            if (BROWSERS.containsKey(pos)) {
                closeBrowser(BROWSERS.remove(pos));
                URLS.remove(pos);
            }
            return;
        }

        // A monitor outside the view frustum draws nothing; do not create or keep a browser for it
        if (!BrowserVisibility.isInView(pos, 3)) {
            return;
        }
        LAST_IN_VIEW.put(pos, System.currentTimeMillis());

        MCEFBrowser browser = BROWSERS.get(pos);
        if (browser == null) {
            browser = MCEF.createBrowser(url, false);

            browser.resize(1280, 805);
            BROWSERS.put(pos, browser);
            URLS.put(pos, url);
        } else {
            String currentUrl = URLS.get(pos);

            if (!url.equals(currentUrl)) {
                browser.loadURL(url);
                URLS.put(pos, url);
            }
        }

        int textureId = browser.getRenderer().getTextureID();

        if (textureId != 0) {
            matrices.push();
            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getCachedState().get(TelevisionMonitorBlock.FACING).asRotation()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            matrices.translate(-0.5, -0.5, -0.501);

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            VertexConsumer bufferBuilder = vertexConsumers.getBuffer(DisplayRenderLayers.browser(textureId));
            bufferBuilder.vertex(matrix4f, 0.15f, 1.85f, 0.25f).texture(0, 1).next();
            bufferBuilder.vertex(matrix4f, 2.85f, 1.85f, 0.25f).texture(1, 1).next();
            bufferBuilder.vertex(matrix4f, 2.85f, 0.15f, 0.25f).texture(1, 0).next();
            bufferBuilder.vertex(matrix4f, 0.15f, 0.15f, 0.25f).texture(0, 0).next();
            matrices.pop();
        }
    }

    public static void closeAll() {
        BROWSERS.values().forEach(TelevisionMonitorBlockEntityRenderer::closeBrowser);
        BROWSERS.clear();
        URLS.clear();
        LAST_IN_VIEW.clear();
    }

    public static void closeBrowser(BlockPos pos) {
        MCEFBrowser browser = BROWSERS.remove(pos);
        if (browser != null) {
            closeBrowser(browser);
        }
        URLS.remove(pos);
        LAST_IN_VIEW.remove(pos);
    }

    /** Closes browsers of monitors that were not in view for a while; they reload when seen again. */
    public static void closeIdle(long currentMillis) {
        BROWSERS.keySet().removeIf(pos -> {
            final Long lastInView = LAST_IN_VIEW.get(pos);
            if (lastInView != null && currentMillis - lastInView <= BrowserVisibility.IDLE_MILLIS) {
                return false;
            }
            closeBrowser(BROWSERS.get(pos));
            URLS.remove(pos);
            LAST_IN_VIEW.remove(pos);
            return true;
        });
    }

    private static void closeBrowser(MCEFBrowser browser) {
        DisplayRenderLayers.releaseBrowser(browser.getRenderer().getTextureID());
        browser.close();
    }
}
