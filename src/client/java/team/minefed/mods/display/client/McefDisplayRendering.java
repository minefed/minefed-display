package team.minefed.mods.display.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import team.minefed.mods.display.blocks.DisplayBlockEntityTypes;
import team.minefed.mods.display.client.renderers.CustomSizeDisplayBlockEntityRenderer;
import team.minefed.mods.display.client.renderers.TelevisionMonitorBlockEntityRenderer;

/** Keeps browser renderer classes unloaded until the optional MCEF mod is present. */
public final class McefDisplayRendering {
	private McefDisplayRendering() {
	}

	public static void initialize() {
		BlockEntityRendererRegistry.register(DisplayBlockEntityTypes.TELEVISION_MONITOR_BLOCK,
				TelevisionMonitorBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(DisplayBlockEntityTypes.CUSTOM_SIZE_DISPLAY_BLOCK,
				CustomSizeDisplayBlockEntityRenderer::new);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			TelevisionMonitorBlockEntityRenderer.closeAll();
			CustomSizeDisplayBlockEntityRenderer.closeAll();
		});
	}
}
