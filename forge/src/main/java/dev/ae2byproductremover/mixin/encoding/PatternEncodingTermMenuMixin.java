package dev.ae2byproductremover.mixin.encoding;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.util.ConfigInventory;
import dev.ae2byproductremover.encoding.OutputCompaction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {
    @Shadow
    @Final
    private ConfigInventory encodedInputsInv;

    @Shadow
    @Final
    private ConfigInventory encodedOutputsInv;

    @Inject(method = "encodeProcessingPattern", at = @At("HEAD"), cancellable = true)
    private void ae2byproductremover$encodeCompactedOutputs(CallbackInfoReturnable<ItemStack> cir) {
        var inputs = new GenericStack[encodedInputsInv.size()];
        boolean hasInput = false;
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = encodedInputsInv.getStack(slot);
            hasInput |= inputs[slot] != null;
        }

        var outputs = OutputCompaction.compact(encodedOutputsInv.size(), encodedOutputsInv::getStack);

        if (!hasInput || outputs.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }

        // Preserve distinct slots and their amounts; only remove empty output slots.
        cir.setReturnValue(PatternDetailsHelper.encodeProcessingPattern(inputs,
                outputs.toArray(GenericStack[]::new)));
    }
}
