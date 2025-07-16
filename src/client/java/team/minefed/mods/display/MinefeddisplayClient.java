package team.minefed.mods.display;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import team.minefed.mods.display.blocks.TelevisionMonitorBlock;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;
import team.minefed.mods.display.client.gui.TelevisionMonitorScreen;
import team.minefed.mods.display.client.network.ClientDisplayModMessages;

public class MinefeddisplayClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) {
				var pos = hitResult.getBlockPos();
				var state = world.getBlockState(pos);
				if (state.getBlock() instanceof TelevisionMonitorBlock) {
					if (world.getBlockEntity(pos) instanceof TelevisionMonitorBlockEntity blockEntity) {
						MinecraftClient.getInstance().execute(() -> {
							MinecraftClient.getInstance().setScreen(new TelevisionMonitorScreen(pos, blockEntity.getUrl()));
						});
					}
					return net.minecraft.util.ActionResult.SUCCESS;
				}
			}
			return net.minecraft.util.ActionResult.PASS;
		});
	}
}