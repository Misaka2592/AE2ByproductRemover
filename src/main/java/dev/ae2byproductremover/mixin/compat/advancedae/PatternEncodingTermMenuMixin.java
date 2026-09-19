/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin.compat.advancedae;

import java.util.HashMap;

import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder;
import net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {
    @Shadow @Final private RestrictedInputSlot encodedPatternSlot;

    @WrapMethod(method = "encodeProcessingPattern")
    private ItemStack ae2byproductremover$preserveAdvancedPattern(Operation<ItemStack> original) {
        var encoded = original.call();
        if (encoded == null || !encoded.is(AEItems.PROCESSING_PATTERN.asItem())) {
            return encoded;
        }

        var previous = encodedPatternSlot.getItem();
        if (!previous.is(AAEItems.ADV_PROCESSING_PATTERN.asItem())) {
            return encoded;
        }

        // The early output-compaction return bypasses AdvancedAE's original RETURN injection.
        // Use the already validated encoding so input quantities and compacted outputs stay intact.
        var updated = new AEProcessingPattern(AEItemKey.of(encoded));
        var inputs = updated.getSparseInputs();
        var directions = new HashMap<>(new AdvProcessingPattern(AEItemKey.of(previous)).getDirectionMap());
        directions.keySet().removeIf(key -> inputs.stream()
                .noneMatch(input -> input != null && key.equals(input.what())));

        // Keep the advanced type even when every old input was replaced; new inputs have no direction.
        return AdvPatternDetailsEncoder.encodeProcessingPattern(inputs, updated.getSparseOutputs(), directions);
    }
}
