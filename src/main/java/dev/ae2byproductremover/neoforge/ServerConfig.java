/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.neoforge;

import dev.ae2byproductremover.PlanningMode;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

final class ServerConfig {
    static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue USE_BYPRODUCTS;

    static {
        var builder = new ModConfigSpec.Builder();
        USE_BYPRODUCTS = builder.comment(
                "Allow other predicted processing outputs to satisfy ingredients in new crafting plans.",
                "False uses independent outputs (Lite); true reuses predicted outputs (V2).",
                "Saving this file applies to new calculations. Existing previews and jobs keep their plans.")
                .define("useByproducts", false);
        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    static void onLoad(ModConfigEvent.Loading event) {
        apply(event);
    }

    static void onReload(ModConfigEvent.Reloading event) {
        apply(event);
    }

    private static void apply(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER && event.getConfig().getSpec() == SPEC) {
            PlanningMode.loadWorld(USE_BYPRODUCTS.get());
        }
    }
}
