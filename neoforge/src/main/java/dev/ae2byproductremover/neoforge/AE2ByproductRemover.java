package dev.ae2byproductremover.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(AE2ByproductRemover.MOD_ID)
public final class AE2ByproductRemover {
    public static final String MOD_ID = "ae2byproductremover";

    public AE2ByproductRemover(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modBus.addListener(ServerConfig::onLoad);
    }
}
