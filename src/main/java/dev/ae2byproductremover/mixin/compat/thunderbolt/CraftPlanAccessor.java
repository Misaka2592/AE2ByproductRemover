/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.mixin.compat.thunderbolt;

import java.util.Map;

import appeng.api.stacks.AEKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.moakiee.thunderbolt.core.planner.CraftPlan", remap = false)
public interface CraftPlanAccessor {
    @Accessor("grossDemand")
    Map<AEKey, Long> ae2byproductremover$grossDemand();

    @Accessor("usedStock")
    Map<AEKey, Long> ae2byproductremover$usedStock();

    @Accessor("missing")
    Map<AEKey, Long> ae2byproductremover$missing();
}
