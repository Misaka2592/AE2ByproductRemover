/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin;

import appeng.crafting.CraftingCalculation;
import dev.ae2byproductremover.PlanningMode;
import dev.ae2byproductremover.crafting.CalculationMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingCalculation.class, remap = false)
public abstract class CraftingCalculationMixin implements CalculationMode {
    @Unique private boolean ae2byproductremover$reuse;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2byproductremover$captureMode(CallbackInfo ci) {
        ae2byproductremover$reuse = PlanningMode.useByproducts();
    }

    @Override
    public boolean ae2byproductremover$reuseOtherOutputs() {
        return ae2byproductremover$reuse;
    }
}
