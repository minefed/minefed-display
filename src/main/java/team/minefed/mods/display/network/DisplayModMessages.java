package team.minefed.mods.display.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import team.minefed.mods.display.Minefeddisplay;
import team.minefed.mods.display.blocks.TelevisionMonitorBlockEntity;

public class DisplayModMessages {

    public static final Identifier UPDATE_URL_PACKET_ID = new Identifier(Minefeddisplay.MOD_ID, "update_url");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_URL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            UpdateUrlPacket packet = new UpdateUrlPacket(buf);
            server.execute(() -> {
                if (player.getWorld().getBlockEntity(packet.pos()) instanceof TelevisionMonitorBlockEntity blockEntity) {
                    blockEntity.setUrl(packet.url());
                }
            });
        });
    }
} 