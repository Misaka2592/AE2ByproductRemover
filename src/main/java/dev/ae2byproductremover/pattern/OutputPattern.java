/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

public final class OutputPattern extends BaseOutputPattern {
    public OutputPattern(IPatternDetails delegate, AEKey target, GenericStack[] outputs) {
        super(delegate, target, outputs);
    }

    @Override
    public GenericStack[] getOutputs() {
        return visibleOutputs();
    }
}
