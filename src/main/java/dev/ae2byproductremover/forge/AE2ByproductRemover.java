/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.forge;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AE2ByproductRemover.MOD_ID)
public final class AE2ByproductRemover {
    public static final String MOD_ID = "ae2byproductremover";

    public AE2ByproductRemover() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ServerConfig::onLoad);
    }
}
