package dev.ae2byproductremover.mixin.compat.dataenergistics;

import java.util.List;

import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import dev.ae2byproductremover.encoding.OutputCompaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.fish_dan_.data_energistics.menu.patternencoding.source.PatternEncodingSourceHelper",
        remap = false)
public abstract class PatternEncodingSourceHelperMixin {
    @Inject(method = "normalizeProcessingPatternInventory", at = @At("RETURN"), cancellable = true)
    private static void ae2byproductremover$compactNormalizedOutputs(ConfigInventory inventory,
            String inventoryKind, CallbackInfoReturnable<List<GenericStack>> cir) {
        var normalized = cir.getReturnValue();
        if ("output".equals(inventoryKind) && normalized != null) {
            // Data Energistics re-encodes after AE2's menu method. Keep its custom-key
            // unwrapping, but remove output holes before its first-output validation.
            cir.setReturnValue(OutputCompaction.compact(normalized.size(), normalized::get));
        }
    }
}
