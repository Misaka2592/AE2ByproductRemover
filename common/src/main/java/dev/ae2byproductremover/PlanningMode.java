package dev.ae2byproductremover;

/** World-scoped server setting, copied into each new crafting calculation at its creation. */
public final class PlanningMode {
    private static volatile boolean useByproducts;

    private PlanningMode() {
    }

    /** False means independent outputs; true allows other predicted outputs to satisfy ingredients. */
    public static boolean useByproducts() {
        return useByproducts;
    }

    /** Called only when the world's SERVER configuration is loaded, never on a live config reload. */
    public static void loadWorld(boolean enabled) {
        useByproducts = enabled;
    }
}
