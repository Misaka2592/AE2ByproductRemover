package dev.ae2byproductremover.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.me.service.helpers.NetworkCraftingProviders;
import dev.ae2byproductremover.pattern.OutputPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = NetworkCraftingProviders.class, remap = false)
public abstract class NetworkCraftingProvidersMixin {
    @ModifyVariable(method = "getMediums", at = @At("HEAD"), argsOnly = true)
    private IPatternDetails ae2byproductremover$findOriginalProvider(IPatternDetails pattern) {
        return OutputPattern.unwrap(pattern);
    }
}
