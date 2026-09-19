package dev.ae2byproductremover.mixin.execution;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.crafting.execution.ExecutingCraftingJob$TaskProgress", remap = false)
public interface TaskProgressAccessor {
    @Accessor("value")
    long ae2byproductremover$getRemainingCrafts();
}
