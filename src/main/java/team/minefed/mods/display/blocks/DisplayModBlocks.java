package team.minefed.mods.display.blocks;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import team.minefed.mods.display.Minefeddisplay;
import team.minefed.mods.display.items.DisplayModItems;

public class DisplayModBlocks {

    public static final Block TELEVISION_MONITOR = register(
            new TelevisionMonitorBlock(),
            "television_monitor",
            true
    );

    public static Block register(Block block, String name, boolean shouldRegisterItem) {
        Identifier id = new Identifier(Minefeddisplay.MOD_ID, name);

        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, new Item.Settings());

            Registry.register(Registries.ITEM, id, blockItem);
        }

        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(DisplayModItems.CUSTOM_ITEM_GROUP_KEY).register((itemGroup) -> {
            itemGroup.add(DisplayModBlocks.TELEVISION_MONITOR.asItem());
        });
    }
}
