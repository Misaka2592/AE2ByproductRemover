package dev.ae2byproductremover.mixin.persistence;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;

import dev.ae2byproductremover.execution.PatternTaskPersistence;
import dev.ae2byproductremover.execution.ProcessingJob;
import dev.ae2byproductremover.mixin.execution.ExecutingCraftingJobAccessor;
import dev.ae2byproductremover.pattern.OutputPattern;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public abstract class ExecutingCraftingJobMixin implements ProcessingJob {
    @Unique
    private CompoundTag ae2byproductremover$readingTask;

    @Unique
    private HolderLookup.Provider ae2byproductremover$registries;

    @Unique
    private boolean ae2byproductremover$processingPlan;

    @Override
    public boolean ae2byproductremover$hasProcessingPlan() {
        return ae2byproductremover$processingPlan;
    }

    @Inject(method = { "<init>(Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/CraftingLink;Ljava/lang/Integer;)V", "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/execution/CraftingCpuLogic;)V" }, at = @At("RETURN"))
    private void ae2byproductremover$identifyProcessingPlan(CallbackInfo callback) {
        ae2byproductremover$processingPlan |= ((ExecutingCraftingJobAccessor) this)
                .ae2byproductremover$getTasks().keySet().stream().anyMatch(OutputPattern.class::isInstance);
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/execution/CraftingCpuLogic;)V",
            at = @At("RETURN"))
    private void ae2byproductremover$restoreProcessingPlan(CompoundTag tag, HolderLookup.Provider registries,
            @Coerce Object listener, CraftingCpuLogic cpu, CallbackInfo callback) {
        ae2byproductremover$processingPlan |= tag.getBoolean("ae2byproductremover:processingPlan");
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void ae2byproductremover$saveOutputViews(HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> callback) {
        PatternTaskPersistence.writeViews(callback.getReturnValue(),
                ((ExecutingCraftingJobAccessor) this).ae2byproductremover$getTasks(),
                stack -> GenericStack.writeTag(registries, stack));
        if (ae2byproductremover$processingPlan) {
            callback.getReturnValue().putBoolean("ae2byproductremover:processingPlan", true);
        }
    }

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/execution/CraftingCpuLogic;)V", at = @At(value = "INVOKE",
            target = "Lappeng/api/stacks/AEItemKey;fromTag(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lappeng/api/stacks/AEItemKey;"))
    private AEItemKey ae2byproductremover$rememberTask(HolderLookup.Provider registries, CompoundTag tag) {
        ae2byproductremover$readingTask = tag;
        ae2byproductremover$registries = registries;
        return AEItemKey.fromTag(registries, tag);
    }

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lappeng/crafting/execution/ExecutingCraftingJob$CraftingDifferenceListener;Lappeng/crafting/execution/CraftingCpuLogic;)V", at = @At(value = "INVOKE",
            target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"))
    private IPatternDetails ae2byproductremover$restoreOutputView(AEItemKey pattern, Level level) {
        CompoundTag savedTask = ae2byproductremover$readingTask;
        HolderLookup.Provider registries = ae2byproductremover$registries;
        ae2byproductremover$readingTask = null;
        ae2byproductremover$registries = null;
        return PatternTaskPersistence.readView(PatternDetailsHelper.decodePattern(pattern, level),
                savedTask, tag -> GenericStack.readTag(registries, tag));
    }
}
