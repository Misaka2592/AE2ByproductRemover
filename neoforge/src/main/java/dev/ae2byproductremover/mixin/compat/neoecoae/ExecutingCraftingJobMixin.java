package dev.ae2byproductremover.mixin.compat.neoecoae;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
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
import dev.ae2byproductremover.compat.AddonJobState;
import dev.ae2byproductremover.execution.PatternTaskPersistence;
import dev.ae2byproductremover.pattern.OutputPattern;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ExecutingCraftingJob", remap = false)
public abstract class ExecutingCraftingJobMixin implements AddonJobState {
    @Shadow @Final private Map<IPatternDetails, Object> tasks;
    @Shadow private long remainingAmount;
    @Shadow private GenericStack finalOutput;
    @Unique private boolean ae2byproductremover$processingPlan;
    @Unique private CompoundTag ae2byproductremover$readingTask;
    @Unique private HolderLookup.Provider ae2byproductremover$registries;

    @Override
    public Map<IPatternDetails, ?> ae2byproductremover$getTasks() {
        return tasks;
    }

    @Override
    public long ae2byproductremover$getRemainingAmount() {
        return remainingAmount;
    }

    @Override
    public GenericStack ae2byproductremover$getFinalOutput() {
        return finalOutput;
    }

    @Override
    public boolean ae2byproductremover$hasProcessingPlan() {
        return ae2byproductremover$processingPlan;
    }

    @Override
    public boolean ae2byproductremover$hasPendingCrafts() {
        return tasks.values().stream().anyMatch(progress ->
                ((TaskProgressAccessor) progress).ae2byproductremover$getRemainingCrafts() > 0);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2byproductremover$identifyProcessingPlan(CallbackInfo callback) {
        ae2byproductremover$processingPlan |= tasks.keySet().stream().anyMatch(OutputPattern.class::isInstance);
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob$CraftingDifferenceListener;Lcn/dancingsnow/neoecoae/api/me/ECOCraftingCPULogic;)V", at = @At("RETURN"))
    private void ae2byproductremover$restoreProcessingPlan(CompoundTag data, HolderLookup.Provider registries,
            @Coerce Object listener, @Coerce Object cpu, CallbackInfo callback) {
        ae2byproductremover$processingPlan |= data.getBoolean("ae2byproductremover:processingPlan");
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void ae2byproductremover$saveOutputViews(HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> callback) {
        PatternTaskPersistence.writeViews(callback.getReturnValue(), tasks,
                stack -> GenericStack.writeTag(registries, stack));
        if (ae2byproductremover$processingPlan) {
            callback.getReturnValue().putBoolean("ae2byproductremover:processingPlan", true);
        }
    }

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob$CraftingDifferenceListener;Lcn/dancingsnow/neoecoae/api/me/ECOCraftingCPULogic;)V", at = @At(value = "INVOKE",
            target = "Lappeng/api/stacks/AEItemKey;fromTag(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lappeng/api/stacks/AEItemKey;"))
    private AEItemKey ae2byproductremover$rememberTask(HolderLookup.Provider registries, CompoundTag tag) {
        ae2byproductremover$readingTask = tag;
        ae2byproductremover$registries = registries;
        return AEItemKey.fromTag(registries, tag);
    }

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Lcn/dancingsnow/neoecoae/api/me/ExecutingCraftingJob$CraftingDifferenceListener;Lcn/dancingsnow/neoecoae/api/me/ECOCraftingCPULogic;)V", at = @At(value = "INVOKE",
            target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lappeng/api/stacks/AEItemKey;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"))
    private IPatternDetails ae2byproductremover$restoreOutputView(AEItemKey pattern, Level level) {
        var savedTask = ae2byproductremover$readingTask;
        var registries = ae2byproductremover$registries;
        ae2byproductremover$readingTask = null;
        ae2byproductremover$registries = null;
        return PatternTaskPersistence.readView(PatternDetailsHelper.decodePattern(pattern, level),
                savedTask, tag -> GenericStack.readTag(registries, tag));
    }
}
