/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeNode;
import dev.ae2byproductremover.crafting.CalculationMode;
import dev.ae2byproductremover.pattern.OutputPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = CraftingTreeNode.class, remap = false)
public abstract class CraftingTreeNodeMixin {
    @Shadow @Final private AEKey what;
    @Shadow @Final private CraftingCalculation job;

    @ModifyArg(method = "buildChildPatterns", at = @At(value = "INVOKE",
            target = "Lappeng/crafting/CraftingTreeProcess;<init>(Lappeng/api/networking/crafting/ICraftingService;Lappeng/crafting/CraftingCalculation;Lappeng/api/crafting/IPatternDetails;Lappeng/crafting/CraftingTreeNode;)V"), index = 2)
    private IPatternDetails ae2byproductremover$selectOutput(IPatternDetails pattern) {
        if (!OutputPattern.isProcessing(pattern)) {
            return pattern;
        }
        return OutputPattern.forTarget(pattern, what, ((CalculationMode) job).ae2byproductremover$reuseOtherOutputs());
    }
}
