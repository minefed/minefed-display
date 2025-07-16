package team.minefed.mods.display.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import team.minefed.mods.display.network.DisplayModMessages;
import team.minefed.mods.display.network.UpdateUrlPacket;

public class ClientDisplayModMessages {
    public static void sendToServer(UpdateUrlPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(DisplayModMessages.UPDATE_URL_PACKET_ID, buf);
    }
} 