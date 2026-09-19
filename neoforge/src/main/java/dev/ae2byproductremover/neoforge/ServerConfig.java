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
