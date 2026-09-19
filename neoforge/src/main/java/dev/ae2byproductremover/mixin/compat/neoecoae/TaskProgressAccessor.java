package dev.ae2byproductremover.mixin.compat.neoecoae;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.api.me.ExecutingCraftingJob$TaskProgress", remap = false)
public interface TaskProgressAccessor {
    @Accessor("value")
    long ae2byproductremover$getRemainingCrafts();
}
