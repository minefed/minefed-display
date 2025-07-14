package team.minefed.mods.display.enums;

import net.minecraft.util.StringIdentifiable;

public enum TvPart implements StringIdentifiable {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    private final String name;
    TvPart(String name) { this.name = name; }
    @Override public String asString() { return name; }
}