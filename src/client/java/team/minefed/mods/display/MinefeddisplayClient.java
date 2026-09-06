package team.minefed.mods.display;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;
import team.minefed.mods.display.blocks.TelevisionMonitorBlock;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;
import team.minefed.mods.display.client.gui.CustomSizeDisplayScreen;
import team.minefed.mods.display.client.gui.TelevisionMonitorScreen;
import team.minefed.mods.display.client.McefDisplayRendering;

public class MinefeddisplayClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (FabricLoader.getInstance().isModLoaded("mcef")) {
			McefDisplayRendering.initialize();
		} else {
			Minefeddisplay.LOGGER.warn("MCEF is not installed: display web rendering is disabled. "
					+ "Install MCEF 2.1.6 or later for Minecraft 1.20.4 on this client to enable it.");
		}

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) {
				var pos = hitResult.getBlockPos();
				var state = world.getBlockState(pos);

				if (state.getBlock() instanceof TelevisionMonitorBlock) {
					if (world.getBlockEntity(pos) instanceof TelevisionMonitorBlockEntity blockEntity) {
						MinecraftClient.getInstance().execute(() -> {
							MinecraftClient.getInstance()
									.setScreen(new TelevisionMonitorScreen(pos, blockEntity.getUrl()));
						});
					}
					return net.minecraft.util.ActionResult.SUCCESS;
				}

				if (state.getBlock() instanceof CustomSizeDisplayBlock) {
					// Find the main block entity
					if (state.get(CustomSizeDisplayBlock.IS_MAIN)) {
						if (world.getBlockEntity(pos) instanceof CustomSizeDisplayBlockEntity blockEntity) {
							MinecraftClient.getInstance().execute(() -> {
								MinecraftClient.getInstance().setScreen(new CustomSizeDisplayScreen(
										pos, blockEntity.getUrl(),
										blockEntity.getDisplayWidth(),
										blockEntity.getDisplayHeight()));
							});
						}
					} else {
						// Find main block by searching
						var facing = state.get(CustomSizeDisplayBlock.FACING);
						var left = facing.rotateYClockwise();

						for (int dx = 0; dx < CustomSizeDisplayBlock.MAX_SIZE; dx++) {
							for (int dy = 0; dy < CustomSizeDisplayBlock.MAX_SIZE; dy++) {
								var checkPos = pos.offset(left, dx).up(dy);
								var checkState = world.getBlockState(checkPos);

								if (checkState.getBlock() instanceof CustomSizeDisplayBlock &&
										checkState.get(CustomSizeDisplayBlock.IS_MAIN)) {
									if (world.getBlockEntity(
											checkPos) instanceof CustomSizeDisplayBlockEntity blockEntity) {
										var mainPos = checkPos;
										MinecraftClient.getInstance().execute(() -> {
											MinecraftClient.getInstance().setScreen(new CustomSizeDisplayScreen(
													mainPos, blockEntity.getUrl(),
													blockEntity.getDisplayWidth(),
													blockEntity.getDisplayHeight()));
										});
									}
									return net.minecraft.util.ActionResult.SUCCESS;
								}
							}
						}
					}
					return net.minecraft.util.ActionResult.SUCCESS;
				}
			}
			return net.minecraft.util.ActionResult.PASS;
		});
	}
}
