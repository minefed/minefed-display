package team.minefed.mods.display.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import team.minefed.mods.display.Minefeddisplay;

public record UpdateCustomDisplayPacket(BlockPos pos, String url, int width, int height) implements FabricPacket {

    public static final PacketType<UpdateCustomDisplayPacket> TYPE = PacketType.create(
            new Identifier(Minefeddisplay.MOD_ID, "update_custom_display"),
            UpdateCustomDisplayPacket::new);

    public UpdateCustomDisplayPacket(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readString(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(url);
        buf.writeInt(width);
        buf.writeInt(height);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
