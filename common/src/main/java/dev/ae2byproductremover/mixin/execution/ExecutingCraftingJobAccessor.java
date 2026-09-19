package dev.ae2byproductremover.mixin.execution;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.ExecutingCraftingJob;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public interface ExecutingCraftingJobAccessor {
    @Accessor("tasks")
    Map<IPatternDetails, ?> ae2byproductremover$getTasks();

    @Accessor("remainingAmount")
    long ae2byproductremover$getRemainingAmount();

    @Accessor("finalOutput")
    GenericStack ae2byproductremover$getFinalOutput();
}
