/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin;

import java.util.List;

import appeng.api.crafting.IPatternDetails;
import dev.ae2byproductremover.pattern.OutputPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "appeng.me.service.helpers.NetworkCraftingProviders$ProviderState", remap = false)
public abstract class ProviderStateMixin {
    @Shadow @Final private List<IPatternDetails> patterns;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2byproductremover$indexAllOutputs(CallbackInfo ci) {
        OutputPattern.addAlternativeOutputs(patterns);
    }
}
