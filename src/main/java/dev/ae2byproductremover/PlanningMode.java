/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
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

    /** Applies the world's SERVER configuration to future calculations, preserving existing plans. */
    public static void loadWorld(boolean enabled) {
        useByproducts = enabled;
    }
}
