/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.crafting;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingPlan;
import dev.ae2byproductremover.pattern.OutputPattern;

public final class PlanOutputs {
    private PlanOutputs() {
    }

    /**
     * Remove unused output kinds from the execution plan. Quantities retain AE2's batch-yield semantics.
     * Keeping every producer of a needed kind preserves dependencies between intermediate recipes.
     */
    public static CraftingPlan forExecution(CraftingPlan plan, ToLongFunction<AEKey> leftover) {
        var produced = new KeyCounter();
        for (var entry : plan.patternTimes().entrySet()) {
            for (var output : entry.getKey().getOutputs()) {
                produced.add(output.what(), output.amount() * entry.getValue());
            }
        }
        return retainRequiredOutputs(plan, key -> produced.get(key) > leftover.applyAsLong(key));
    }

    /** Adapts an external planner using its own accounting of which output kinds were consumed. */
    public static CraftingPlan retainRequiredOutputs(CraftingPlan plan, Predicate<AEKey> required) {
        var patterns = new LinkedHashMap<IPatternDetails, Long>();
        for (var entry : plan.patternTimes().entrySet()) {
            IPatternDetails details = entry.getKey();
            if (details instanceof OutputPattern view) {
                var outputs = Arrays.stream(view.visibleOutputs())
                        .filter(output -> output.what().equals(view.target())
                                || required.test(output.what()))
                        .toArray(GenericStack[]::new);
                details = new OutputPattern(view.delegate(), view.target(), outputs);
            }
            patterns.merge(details, entry.getValue(), Long::sum);
        }
        return new CraftingPlan(plan.finalOutput(), plan.bytes(), plan.simulation(), plan.multiplePaths(),
                plan.usedItems(), plan.emittedItems(), plan.missingItems(), patterns);
    }
}
