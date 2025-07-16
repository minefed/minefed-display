package team.minefed.mods.display.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import team.minefed.mods.display.Minefeddisplay;

public class DisplayBlockEntityTypes {

    public static final BlockEntityType<TelevisionMonitorBlockEntity> TELEVISION_MONITOR_BLOCK = register(
            "television_monitor",
            FabricBlockEntityTypeBuilder.create(TelevisionMonitorBlockEntity::new, DisplayModBlocks.TELEVISION_MONITOR).build()
    );

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Minefeddisplay.MOD_ID, path), blockEntityType);
    }

    public static void initialize() {

    }
}
