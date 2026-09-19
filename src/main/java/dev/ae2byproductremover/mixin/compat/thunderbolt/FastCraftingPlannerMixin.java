/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin.compat.thunderbolt;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingPlan;
import appeng.crafting.inv.ChildCraftingSimulationState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ae2byproductremover.compat.thunderbolt.FastPlanningContext;
import dev.ae2byproductremover.crafting.PlanOutputs;
import dev.ae2byproductremover.pattern.OutputPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.moakiee.thunderbolt.ae2.crafting.FastCraftingPlanner", remap = false)
public abstract class FastCraftingPlannerMixin {
    @WrapOperation(method = "buildGraph", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingService;getCraftingFor(Lappeng/api/stacks/AEKey;)Ljava/util/Collection;"))
    private static Collection<IPatternDetails> ae2byproductremover$selectOutputs(ICraftingService service, AEKey target,
            Operation<Collection<IPatternDetails>> original) {
        boolean reuse = FastPlanningContext.reuseOutputs();
        return original.call(service, target).stream()
                .map(pattern -> OutputPattern.isProcessing(pattern)
                        ? OutputPattern.forTarget(pattern, target, reuse) : pattern)
                .toList();
    }

    @Inject(method = "toAe2Plan", at = @At("RETURN"), cancellable = true)
    private static void ae2byproductremover$retainConsumedOutputs(AEKey output, long amount,
            @Coerce Object thunderboltPlan, boolean multiplePaths, boolean simulation,
            Map<?, ?> durability, Map<?, ?> patternSources, Set<AEKey> emittable,
            ChildCraftingSimulationState snapshot, @Coerce Object reservedStock,
            CallbackInfoReturnable<CraftingPlan> cir) {
        var usage = (CraftPlanAccessor) thunderboltPlan;
        var demand = usage.ae2byproductremover$grossDemand();
        var stock = usage.ae2byproductremover$usedStock();
        var missing = usage.ae2byproductremover$missing();
        // Gross demand includes consumption from the predicted-output pool. Stock and missing
        // quantities did not consume a produced output; all other demand did.
        cir.setReturnValue(PlanOutputs.retainRequiredOutputs(cir.getReturnValue(), key ->
                demand.getOrDefault(key, 0L) - stock.getOrDefault(key, 0L) - missing.getOrDefault(key, 0L) > 0));
    }
}
