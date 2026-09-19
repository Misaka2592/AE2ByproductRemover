package dev.ae2byproductremover.mixin.client;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.me.items.ProcessingEncodingPanel;
import appeng.client.gui.widgets.ActionButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ProcessingEncodingPanel.class, remap = false)
public abstract class ProcessingEncodingPanelMixin {
    @Shadow
    @Final
    private ActionButton cycleOutputBtn;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2byproductremover$presentEqualOutputs(PatternEncodingTermScreen<?> screen,
            WidgetContainer widgets, CallbackInfo ci) {
        screen.getMenu().getProcessingOutputSlots()[0].setIcon(null);
        cycleOutputBtn.setMessage(Component.translatable("gui.ae2byproductremover.cycle_outputs")
                .append("\n")
                .append(Component.translatable("gui.ae2byproductremover.cycle_outputs_hint")));
    }

    @ModifyArg(method = "updateTooltipVisibility", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/WidgetContainer;setTooltipAreaEnabled(Ljava/lang/String;Z)V"),
            index = 1, require = 4)
    private boolean ae2byproductremover$hideOutputRoleTooltips(boolean enabled) {
        return false;
    }
}
