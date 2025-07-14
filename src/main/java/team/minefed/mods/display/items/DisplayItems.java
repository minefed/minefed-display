package team.minefed.mods.display.items;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import team.minefed.mods.display.Minefeddisplay;

public class DisplayItems {

    public static final RegistryKey<ItemGroup> CUSTOM_ITEM_GROUP_KEY =
            RegistryKey.of(Registries.ITEM_GROUP.getKey(), new Identifier(Mine))

    public static final Item TELEVISION_MONITOR = register(
            new Item(new FabricItemSettings()),
            "television_monitor"
    );

    public static Item register(Item item, String id) {
        Identifier itemId = new Identifier(Minefeddisplay.MOD_ID, id);

        return Registry.register(Registries.ITEM, itemId, item);
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> itemGroup.add(DisplayItems.TELEVISION_MONITOR));
    }
}
