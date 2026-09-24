package team.minefed.mods.display.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** World-space, full-bright display surfaces with normal block occlusion. */
final class DisplayRenderLayers extends RenderLayer {
    private static final ShaderProgram PROGRAM = new ShaderProgram(() -> {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return GameRenderer.getPositionTexProgram();
    });
    private static final Map<Integer, RenderLayer> BROWSER_LAYERS = new HashMap<>();
    private static final Map<Identifier, RenderLayer> BEZEL_LAYERS = new HashMap<>();

    private DisplayRenderLayers() {
        super("minefed_display", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS,
                256, false, false, () -> {}, () -> {});
    }

    static RenderLayer browser(int textureId) {
        return BROWSER_LAYERS.computeIfAbsent(textureId, id -> create("minefed_display_browser",
                new RenderPhase.TextureBase(() -> RenderSystem.setShaderTexture(0, id), () -> {}), NO_TRANSPARENCY));
    }

    static RenderLayer bezel(Identifier texture) {
        return BEZEL_LAYERS.computeIfAbsent(texture, id -> create("minefed_display_bezel",
                new RenderPhase.Texture(id, false, false), TRANSLUCENT_TRANSPARENCY));
    }

    static void releaseBrowser(int textureId) {
        BROWSER_LAYERS.remove(textureId);
    }

    private static RenderLayer create(String name, RenderPhase.TextureBase texture, Transparency transparency) {
        // A block entity may run after a layer that disabled depth testing. Let the
        // vertex provider apply these phases when it draws, rather than inheriting
        // whichever GL state happens to be active while the entity is visited.
        return of(name, VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256,
                MultiPhaseParameters.builder()
                        .program(PROGRAM)
                        .texture(texture)
                        .transparency(transparency)
                        .depthTest(LEQUAL_DEPTH_TEST)
                        .writeMaskState(ALL_MASK)
                        .build(false));
    }
}
