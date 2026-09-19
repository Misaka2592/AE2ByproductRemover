package dev.ae2byproductremover.mixin.compat.thunderbolt;

import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ae2byproductremover.compat.thunderbolt.FastPlanningContext;
import dev.ae2byproductremover.crafting.CalculationMode;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CraftingCalculation.class, remap = false)
public abstract class CraftingCalculationContextMixin {
    @WrapMethod(method = "runCraftAttempt")
    private CraftingPlan ae2byproductremover$carryCapturedMode(boolean simulate, long amount,
            Operation<CraftingPlan> original) {
        Boolean previous = FastPlanningContext.enter(((CalculationMode) this).ae2byproductremover$reuseOtherOutputs());
        try {
            return original.call(simulate, amount);
        } finally {
            FastPlanningContext.restore(previous);
        }
    }
}
