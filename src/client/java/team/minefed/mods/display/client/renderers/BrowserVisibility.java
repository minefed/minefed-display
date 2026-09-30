package team.minefed.mods.display.client.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import team.minefed.mods.display.mixin.client.WorldRendererFrustumAccessor;

/**
 * Display renderers render outside their bounding box, so the game calls them for every loaded display within
 * 256 blocks, visible or not. Browsers are only created for displays in the view frustum and are closed when
 * a display was not in view for {@link #IDLE_MILLIS}.
 */
final class BrowserVisibility {

	static final long IDLE_MILLIS = 30_000;

	private BrowserVisibility() {
	}

	/**
	 * @param reach how many blocks the display extends from its origin block in any direction
	 * @return whether a box that contains the whole display is in the current view frustum
	 */
	static boolean isInView(BlockPos pos, int reach) {
		final MinecraftClient client = MinecraftClient.getInstance();
		final Frustum frustum = client.worldRenderer == null ? null : ((WorldRendererFrustumAccessor) client.worldRenderer).minefed$getFrustum();
		return frustum == null || frustum.isVisible(new Box(pos).expand(reach + 1));
	}
}
