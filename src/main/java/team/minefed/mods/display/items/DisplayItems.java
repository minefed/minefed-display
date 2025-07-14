package team.minefed.mods.display.items;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import team.minefed.mods.display.Minefeddisplay;

public class DisplayModItems {

    public static final RegistryKey<ItemGroup> CUSTOM_ITEM_GROUP_KEY =
            RegistryKey.of(Registries.ITEM_GROUP.getKey(), new Identifier(Minefeddisplay.MOD_ID, "item_group"));
    public static final ItemGroup CUSTOM_ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(DisplayModItems.TELEVISION_MONITOR))
            .displayName(Text.translatable("itemGroup.minefed-display"))
            .build();

    public static final Item TELEVISION_MONITOR = register(
            new Item(new FabricItemSettings()),
            "television_monitor"
    );

    public static Item register(Item item, String id) {
        Identifier itemId = new Identifier(Minefeddisplay.MOD_ID, id);

        return Registry.register(Registries.ITEM, itemId, item);
    }

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_ITEM_GROUP_KEY, CUSTOM_ITEM_GROUP);

        ItemGroupEvents.modifyEntriesEvent(CUSTOM_ITEM_GROUP_KEY)
                .register((itemGroup) -> {
                    itemGroup.add(DisplayModItems.TELEVISION_MONITOR);
                });
    }
}
