package team.minefed.mods.display.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import team.minefed.mods.display.Minefeddisplay;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlock;
import team.minefed.mods.display.blocks.CustomSizeDisplayBlockEntity;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;

public class DisplayModMessages {

    public static final Identifier UPDATE_URL_PACKET_ID = new Identifier(Minefeddisplay.MOD_ID, "update_url");
    public static final Identifier UPDATE_CUSTOM_DISPLAY_PACKET_ID = new Identifier(Minefeddisplay.MOD_ID,
            "update_custom_display");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_URL_PACKET_ID,
                (server, player, handler, buf, responseSender) -> {
                    UpdateUrlPacket packet = new UpdateUrlPacket(buf);
                    server.execute(() -> {
                        if (player.getWorld()
                                .getBlockEntity(packet.pos()) instanceof TelevisionMonitorBlockEntity blockEntity) {
                            blockEntity.setUrl(packet.url());
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_CUSTOM_DISPLAY_PACKET_ID,
                (server, player, handler, buf, responseSender) -> {
                    UpdateCustomDisplayPacket packet = new UpdateCustomDisplayPacket(buf);
                    server.execute(() -> {
                        var world = player.getWorld();
                        var blockState = world.getBlockState(packet.pos());

                        if (blockState.getBlock() instanceof CustomSizeDisplayBlock block) {
                            // First resize the structure if dimensions changed
                            if (world
                                    .getBlockEntity(packet.pos()) instanceof CustomSizeDisplayBlockEntity blockEntity) {
                                int currentWidth = blockEntity.getDisplayWidth();
                                int currentHeight = blockEntity.getDisplayHeight();

                                if (currentWidth != packet.width() || currentHeight != packet.height()) {
                                    block.resizeDisplay(world, packet.pos(), packet.width(), packet.height());
                                }

                                // Update URL (get fresh reference after potential resize)
                                if (world.getBlockEntity(
                                        packet.pos()) instanceof CustomSizeDisplayBlockEntity updatedEntity) {
                                    updatedEntity.setUrl(packet.url());
                                }
                            }
                        }
                    });
                });
    }
}
