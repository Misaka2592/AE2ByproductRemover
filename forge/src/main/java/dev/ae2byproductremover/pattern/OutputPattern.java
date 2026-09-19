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
