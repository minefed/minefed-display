package team.minefed.mods.display.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import org.cef.CefApp;
import org.cef.handler.CefAppHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;
import team.minefed.mods.display.blocks.DisplayBlockEntityTypes;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;
import team.minefed.mods.display.client.renderers.CustomSizeDisplayBlockEntityRenderer;
import team.minefed.mods.display.client.renderers.TelevisionMonitorBlockEntityRenderer;

/** Keeps browser renderer classes unloaded until the optional MCEF mod is present. */
public final class McefDisplayRendering {
	private static final Logger LOGGER = LoggerFactory.getLogger("minefed-display");
	private static final int IDLE_CHECK_TICKS = 100;
	private static int ticks;

	/**
	 * MCEF 2.1.6 passes exactly these switches to Chromium. A preset application handler replaces them,
	 * so they are repeated. {@code --process-per-site} lets displays that show the same site share one
	 * renderer process instead of starting one per display; pages render the same.
	 */
	private static final String[] CHROMIUM_SWITCHES = {
			"--autoplay-policy=no-user-gesture-required",
			"--disable-web-security",
			"--enable-widevine-cdm",
			"--process-per-site",
	};

	private McefDisplayRendering() {
	}

	public static void initialize() {
		BlockEntityRendererRegistry.register(DisplayBlockEntityTypes.TELEVISION_MONITOR_BLOCK,
				TelevisionMonitorBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(DisplayBlockEntityTypes.CUSTOM_SIZE_DISPLAY_BLOCK,
				CustomSizeDisplayBlockEntityRenderer::new);

		registerChromiumSwitches();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> closeAll());
		// Browsers belong to the world they were shown in; the maps are keyed only by position
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> closeAll());
		// A removed or unloaded display cannot be drawn again; its page reloads if it comes back
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof CustomSizeDisplayBlockEntity) {
				CustomSizeDisplayBlockEntityRenderer.closeBrowser(blockEntity.getPos());
			} else if (blockEntity instanceof TelevisionMonitorBlockEntity) {
				TelevisionMonitorBlockEntityRenderer.closeBrowser(blockEntity.getPos());
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++ticks % IDLE_CHECK_TICKS == 0) {
				final long currentMillis = System.currentTimeMillis();
				CustomSizeDisplayBlockEntityRenderer.closeIdle(currentMillis);
				TelevisionMonitorBlockEntityRenderer.closeIdle(currentMillis);
			}
		});
	}

	private static void closeAll() {
		TelevisionMonitorBlockEntityRenderer.closeAll();
		CustomSizeDisplayBlockEntityRenderer.closeAll();
	}

	private static void registerChromiumSwitches() {
		try {
			CefApp.addAppHandler(new CefAppHandlerAdapter(CHROMIUM_SWITCHES) {
			});
		} catch (IllegalStateException | LinkageError exception) {
			LOGGER.warn("Could not add Chromium switches; MCEF keeps its defaults", exception);
		}
	}
}
