package team.minefed.mods.display;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.minefed.mods.display.items.DisplayItems;

public class Minefeddisplay implements ModInitializer {
	public static final String MOD_ID = "minefed-display";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		DisplayItems.initialize();
	}
}