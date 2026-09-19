/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.forge;

import dev.ae2byproductremover.PlanningMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

final class ServerConfig {
    static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue USE_BYPRODUCTS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        USE_BYPRODUCTS = builder.comment(
                "Allow other predicted processing outputs to satisfy ingredients in new crafting plans.",
                "False uses independent outputs (Lite); true reuses predicted outputs (V2).",
                "Reload the world or restart the server to apply. Existing plans keep their behavior.")
                .worldRestart().define("useByproducts", false);
        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER && event.getConfig().getSpec() == SPEC) {
            PlanningMode.loadWorld(USE_BYPRODUCTS.get());
        }
    }
}
