package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
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

    public TelevisionMonitorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(TelevisionMonitorBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        String url = entity.getUrl();
        BlockPos pos = entity.getPos();

        if (url == null || url.isEmpty() || "about:blank".equals(url)) {
            if (BROWSERS.containsKey(pos)) {
                BROWSERS.remove(pos).close();
                URLS.remove(pos);
            }
            return;
        }

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

        // Assuming getTextureID() provides the OpenGL texture ID
        int textureId = browser.getRenderer().getTextureID();

        if (textureId != 0) {
            matrices.push();
            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getCachedState().get(TelevisionMonitorBlock.FACING).asRotation()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            matrices.translate(-0.5, -0.5, -0.501);

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, textureId);

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            bufferBuilder.vertex(matrix4f, 0.15f, 1.85f, 0).texture(0, 1).next();
            bufferBuilder.vertex(matrix4f, 2.85f, 1.85f, 0).texture(1, 1).next();
            bufferBuilder.vertex(matrix4f, 2.85f, 0.15f, 0).texture(1, 0).next();
            bufferBuilder.vertex(matrix4f, 0.15f, 0.15f, 0).texture(0, 0).next();
            Tessellator.getInstance().draw();

            matrices.pop();
        }
    }

    public static void closeAll() {
        BROWSERS.values().forEach(MCEFBrowser::close);
        BROWSERS.clear();
        URLS.clear();
    }
} 