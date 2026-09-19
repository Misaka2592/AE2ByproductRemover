/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin.execution;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;

import dev.ae2byproductremover.execution.ProcessingJob;
import dev.ae2byproductremover.pattern.OutputPattern;

@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class CraftingCpuLogicMixin {
    @Shadow
    private ExecutingCraftingJob job;

    @Shadow
    @Final
    private CraftingCPUCluster cluster;

    @Shadow
    private void finishJob(boolean success) {
        throw new AssertionError();
    }

    @ModifyArg(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"),
            index = 0)
    private IPatternDetails ae2byproductremover$pushOriginalPattern(IPatternDetails pattern) {
        // Providers own the decoded pattern; the restricted outputs belong to this CPU's plan only.
        return OutputPattern.unwrap(pattern);
    }

    @Inject(method = "finishJob", at = @At("HEAD"), cancellable = true)
    private void ae2byproductremover$preservePlannedCrafts(boolean success, CallbackInfo callback) {
        if (success && job != null && ((ProcessingJob) job).ae2byproductremover$hasProcessingPlan()
                && ae2byproductremover$hasPendingCrafts()) {
            callback.cancel();
        }
    }

    @ModifyArg(method = { "insert", "readFromNBT" }, at = @At(value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;updateOutput(Lappeng/api/stacks/GenericStack;)V"),
            index = 0)
    private GenericStack ae2byproductremover$keepActiveJobVisible(GenericStack display) {
        // insert() clears the monitor after finishJob(true), even when our guard deferred completion.
        if ((display == null || display.amount() <= 0) && job != null
                && ((ProcessingJob) job).ae2byproductremover$hasProcessingPlan()
                && ae2byproductremover$hasPendingCrafts()) {
            return ((ExecutingCraftingJobAccessor) job).ae2byproductremover$getFinalOutput();
        }
        return display;
    }

    @Inject(method = "executeCrafting", at = @At("RETURN"))
    private void ae2byproductremover$finishAfterLastPlannedCraft(CallbackInfoReturnable<Integer> callback) {
        if (job != null
                && ((ProcessingJob) job).ae2byproductremover$hasProcessingPlan()
                && ((ExecutingCraftingJobAccessor) job).ae2byproductremover$getRemainingAmount() <= 0
                && !ae2byproductremover$hasPendingCrafts()) {
            // The requested result may have arrived before the final planned operation was sent.
            // Unused physical outputs must not keep the job alive.
            finishJob(true);
            cluster.updateOutput(null);
            cluster.markDirty();
        }
    }

    @Unique
    private boolean ae2byproductremover$hasPendingCrafts() {
        for (var progress : ((ExecutingCraftingJobAccessor) job).ae2byproductremover$getTasks().values()) {
            if (((TaskProgressAccessor) progress).ae2byproductremover$getRemainingCrafts() > 0) {
                return true;
            }
        }
        return false;
    }
}
