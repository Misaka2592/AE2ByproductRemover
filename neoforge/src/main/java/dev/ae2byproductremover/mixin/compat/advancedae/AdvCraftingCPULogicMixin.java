package dev.ae2byproductremover.mixin.compat.advancedae;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import dev.ae2byproductremover.compat.AddonJobState;
import dev.ae2byproductremover.pattern.OutputPattern;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.logic.ExecutingCraftingJob;

@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic", remap = false)
public abstract class AdvCraftingCPULogicMixin {
    @Shadow private ExecutingCraftingJob job;
    @Shadow @Final private AdvCraftingCPU cpu;

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

    @ModifyArg(method = { "insert", "readFromNBT" }, at = @At(value = "INVOKE",
            target = "Lnet/pedroksl/advanced_ae/common/cluster/AdvCraftingCPU;updateOutput(Lappeng/api/stacks/GenericStack;)V"),
            index = 0)
    private GenericStack ae2byproductremover$keepActiveJobVisible(GenericStack display) {
        if ((display == null || display.amount() <= 0) && job != null
                && ((AddonJobState) job).ae2byproductremover$hasProcessingPlan()
                && ((AddonJobState) job).ae2byproductremover$hasPendingCrafts()) {
            return ((AddonJobState) job).ae2byproductremover$getFinalOutput();
        }
        return display;
    }

    @Inject(method = "executeCrafting", at = @At("RETURN"))
    private void ae2byproductremover$finishAfterLastPlannedCraft(CallbackInfoReturnable<Integer> callback) {
        if (job != null && ((AddonJobState) job).ae2byproductremover$hasProcessingPlan()
                && ((AddonJobState) job).ae2byproductremover$getRemainingAmount() <= 0
                && !((AddonJobState) job).ae2byproductremover$hasPendingCrafts()) {
            finishJob(true);
            cpu.updateOutput(null);
            cpu.markDirty();
        }
    }
}
