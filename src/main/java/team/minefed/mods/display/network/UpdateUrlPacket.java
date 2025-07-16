package team.minefed.mods.display.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public record UpdateUrlPacket(BlockPos pos, String url) {
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(url);
    }

    public UpdateUrlPacket(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readString());
    }
} 