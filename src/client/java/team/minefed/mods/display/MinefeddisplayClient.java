package team.minefed.mods.display;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;
import team.minefed.mods.display.blocks.DisplayBlockEntityTypes;
import team.minefed.mods.display.blocks.TelevisionMonitorBlock;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;
import team.minefed.mods.display.client.gui.CustomSizeDisplayScreen;
import team.minefed.mods.display.client.gui.TelevisionMonitorScreen;
import team.minefed.mods.display.client.renderers.CustomSizeDisplayBlockEntityRenderer;
import team.minefed.mods.display.client.renderers.TelevisionMonitorBlockEntityRenderer;

public class MinefeddisplayClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRendererRegistry.register(DisplayBlockEntityTypes.TELEVISION_MONITOR_BLOCK,
				TelevisionMonitorBlockEntityRenderer::new);
		BlockEntityRendererRegistry.register(DisplayBlockEntityTypes.CUSTOM_SIZE_DISPLAY_BLOCK,
				CustomSizeDisplayBlockEntityRenderer::new);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			TelevisionMonitorBlockEntityRenderer.closeAll();
			CustomSizeDisplayBlockEntityRenderer.closeAll();
		});

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