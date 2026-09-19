package dev.ae2byproductremover.mixin;

import appeng.api.stacks.KeyCounter;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.crafting.inv.CraftingSimulationState;
import dev.ae2byproductremover.crafting.PlanOutputs;
import dev.ae2byproductremover.crafting.SimulationRemainder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingSimulationState.class, remap = false)
public abstract class CraftingSimulationStateMixin implements SimulationRemainder {
    @Shadow @Final private KeyCounter modifiableCache;
    @Shadow @Final private KeyCounter unmodifiedCache;
    @Shadow @Final private KeyCounter requiredExtract;

    @Override
    public long ae2byproductremover$leftover(AEKey key) {
        return Math.max(0, modifiableCache.get(key) - unmodifiedCache.get(key) + requiredExtract.get(key));
    }

    @Inject(method = "buildCraftingPlan", at = @At("RETURN"), cancellable = true)
    private static void ae2byproductremover$pruneUnusedOutputs(CraftingSimulationState state,
            CraftingCalculation calculation, long amount, CallbackInfoReturnable<CraftingPlan> cir) {
        var inventory = (SimulationRemainder) state;
        cir.setReturnValue(PlanOutputs.forExecution(cir.getReturnValue(), inventory::ae2byproductremover$leftover));
    }
}
