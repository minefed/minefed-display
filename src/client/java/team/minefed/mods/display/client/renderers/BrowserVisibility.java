package team.minefed.mods.display.client.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
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
		final Frustum frustum = getFrustum();
		return frustum == null || frustum.isVisible(new Box(pos).expand(reach + 1));
	}

	static Frustum getFrustum() {
		final MinecraftClient client = MinecraftClient.getInstance();
		return client == null || client.worldRenderer == null ? null
				: ((WorldRendererFrustumAccessor) client.worldRenderer).minefed$getFrustum();
	}

	/** Bounds of the entire bezel, including its outer edge rather than just browser content. */
	static Box bezelBounds(BlockPos pos, Direction facing, int width, int height) {
		// The renderer rotates the local [0,width] x [0,height] rectangle about (0.5,0.5,0.5).
		// Its 180-degree X rotation puts the top at y=1 and the bottom at y=1-height.
		double minX, maxX, minZ, maxZ;
		switch (facing) {
			case NORTH -> { minX = 1.0 - width; maxX = 1; minZ = maxZ = 0.499; }
			case SOUTH -> { minX = 0; maxX = width; minZ = maxZ = 0.501; }
			case WEST -> { minX = maxX = 0.499; minZ = 0; maxZ = width; }
			case EAST -> { minX = maxX = 0.501; minZ = 1.0 - width; maxZ = 1; }
			default -> throw new IllegalArgumentException("Display facing must be horizontal");
		}
		// Cardinal rotations still use float matrices. Keep a conservative margin for their
		// roundoff, also for old NBT dimensions beyond the normal 32-block size limit.
		double margin = 0.01 + 4.0 * Math.ulp((float) Math.max(width, height));
		return new Box(pos.getX() + minX - margin, pos.getY() + 1.0 - height - margin, pos.getZ() + minZ - margin,
				pos.getX() + maxX + margin, pos.getY() + 1.0 + margin, pos.getZ() + maxZ + margin);
	}
}
