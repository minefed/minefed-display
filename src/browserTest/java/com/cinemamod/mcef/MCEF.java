package com.cinemamod.mcef;

/** Test fixture for the initialization boundary in pinned MCEF 2.1.6. */
public final class MCEF {
    public static boolean initialized;
    public static int initializationChecks;
    public static final MCEFClient CLIENT = new MCEFClient();

    public static MCEFClient getClient() {
        initializationChecks++;
        if (!initialized) throw new IllegalStateException("MCEF is not initialized");
        return CLIENT;
    }
}
