package team.minefed.mods.display.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CustomSizeDisplayBlockEntity extends BlockEntity {

    private String url = "";
    private int displayWidth = 1;
    private int displayHeight = 1;

    public CustomSizeDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(DisplayBlockEntityTypes.CUSTOM_SIZE_DISPLAY_BLOCK, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.url = nbt.getString("url");
        this.displayWidth = nbt.getInt("displayWidth");
        this.displayHeight = nbt.getInt("displayHeight");

        // Ensure valid values
        if (this.displayWidth < CustomSizeDisplayBlock.MIN_SIZE) {
            this.displayWidth = CustomSizeDisplayBlock.MIN_SIZE;
        }
        if (this.displayHeight < CustomSizeDisplayBlock.MIN_SIZE) {
            this.displayHeight = CustomSizeDisplayBlock.MIN_SIZE;
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("url", this.url);
        nbt.putInt("displayWidth", this.displayWidth);
        nbt.putInt("displayHeight", this.displayHeight);
        super.writeNbt(nbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        markDirty();
        notifyUpdate();
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplaySize(int width, int height) {
        this.displayWidth = Math.max(CustomSizeDisplayBlock.MIN_SIZE,
                Math.min(CustomSizeDisplayBlock.MAX_SIZE, width));
        this.displayHeight = Math.max(CustomSizeDisplayBlock.MIN_SIZE,
                Math.min(CustomSizeDisplayBlock.MAX_SIZE, height));
        markDirty();
        notifyUpdate();
    }

    private void notifyUpdate() {
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }
}
