/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover;

import java.util.Arrays;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Lives only in the smoke source set, never in the published mod jar. */
@EventBusSubscriber(modid = "ae2byproductremover", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientMixinSmoke {
    private ClientMixinSmoke() {
    }

    @SubscribeEvent
    public static void verifyClientMixins(FMLClientSetupEvent event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("ae2byproductremover.clientSmoke")) {
            return;
        }
        var panel = Class.forName("appeng.client.gui.me.items.ProcessingEncodingPanel", false,
                ClientMixinSmoke.class.getClassLoader());
        var methods = Arrays.stream(panel.getDeclaredMethods()).map(method -> method.getName()).toList();
        if (methods.stream().noneMatch(name -> name.contains("ae2byproductremover$presentEqualOutputs"))
                || methods.stream().noneMatch(name -> name.contains("ae2byproductremover$hideOutputRoleTooltips"))) {
            throw new AssertionError("Processing encoding panel mixin handlers were not applied: " + methods);
        }
        var menu = Class.forName("appeng.menu.me.items.PatternEncodingTermMenu", false,
                ClientMixinSmoke.class.getClassLoader());
        if (Arrays.stream(menu.getDeclaredMethods())
                .noneMatch(method -> method.getName().contains("ae2byproductremover$encodeCompactedOutputs"))) {
            throw new AssertionError("Processing menu encoding mixin was not applied");
        }
        LogUtils.getLogger().info("AE2BYPRODUCTREMOVER_CLIENT_MIXIN_SMOKE_PASSED");
        NeoForge.EVENT_BUS.addListener(ClientMixinSmoke::finishOnceReady);
    }

    private static void finishOnceReady(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        // Let the initial resource reload finish before closing its texture manager.
        if (minecraft.screen != null && minecraft.getOverlay() == null) {
            minecraft.stop();
        }
    }
}
