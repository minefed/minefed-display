package team.minefed.mods.display;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.minefed.mods.display.blocks.DisplayBlockEntityTypes;
import team.minefed.mods.display.blocks.DisplayModBlocks;
import team.minefed.mods.display.items.DisplayModItems;
import team.minefed.mods.display.network.DisplayModMessages;

public class Minefeddisplay implements ModInitializer {
	public static final String MOD_ID = "minefed-display";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		DisplayModItems.initialize();
		DisplayModBlocks.initialize();
		DisplayBlockEntityTypes.initialize();
		DisplayModMessages.registerC2SPackets();
	}
}