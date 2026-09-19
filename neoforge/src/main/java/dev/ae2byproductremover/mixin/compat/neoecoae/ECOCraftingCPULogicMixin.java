/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin.compat.neoecoae;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.crafting.IPatternDetails;
import cn.dancingsnow.neoecoae.api.me.ExecutingCraftingJob;
import dev.ae2byproductremover.compat.AddonJobState;
import dev.ae2byproductremover.pattern.OutputPattern;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ECOCraftingCPULogic", remap = false)
public abstract class ECOCraftingCPULogicMixin {
    @Shadow private ExecutingCraftingJob job;

    @Shadow
    private void finishJob(boolean success) {
        throw new AssertionError();
    }

    @ModifyArg(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"),
            index = 0)
    private IPatternDetails ae2byproductremover$pushOriginalPattern(IPatternDetails pattern) {
        return OutputPattern.unwrap(pattern);
    }

    @Inject(method = "finishJob", at = @At("HEAD"), cancellable = true)
    private void ae2byproductremover$preservePlannedCrafts(boolean success, CallbackInfo callback) {
        if (success && job != null && ((AddonJobState) job).ae2byproductremover$hasProcessingPlan()
                && ((AddonJobState) job).ae2byproductremover$hasPendingCrafts()) {
            callback.cancel();
        }
    }

    @Inject(method = "executeCrafting", at = @At("RETURN"))
    private void ae2byproductremover$finishAfterLastPlannedCraft(CallbackInfoReturnable<Integer> callback) {
        if (job != null && ((AddonJobState) job).ae2byproductremover$hasProcessingPlan()
                && ((AddonJobState) job).ae2byproductremover$getRemainingAmount() <= 0
                && !((AddonJobState) job).ae2byproductremover$hasPendingCrafts()) {
            finishJob(true);
        }
    }
}
