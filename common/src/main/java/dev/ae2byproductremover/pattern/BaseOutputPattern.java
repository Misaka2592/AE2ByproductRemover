package dev.ae2byproductremover.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.AEProcessingPattern;

/** A calculation/execution view of a processing pattern; machines receive its original delegate. */
public abstract class BaseOutputPattern implements IPatternDetails {
    private final IPatternDetails delegate;
    private final AEKey target;
    private final GenericStack[] outputs;

    protected BaseOutputPattern(IPatternDetails delegate, AEKey target, GenericStack[] outputs) {
        this.delegate = unwrap(Objects.requireNonNull(delegate));
        this.target = Objects.requireNonNull(target);
        this.outputs = outputs.clone();
        if (outputs.length == 0 || Arrays.stream(outputs).noneMatch(output -> target.equals(output.what()))) {
            throw new IllegalArgumentException("The output view must contain its requested product");
        }
    }

    public IPatternDetails delegate() {
        return delegate;
    }

    public AEKey target() {
        return target;
    }

    public static IPatternDetails unwrap(IPatternDetails details) {
        return details instanceof BaseOutputPattern view ? view.delegate : details;
    }

    public static boolean isProcessing(IPatternDetails details) {
        return unwrap(details) instanceof AEProcessingPattern;
    }

    public static OutputPattern indexed(IPatternDetails details, AEKey target) {
        return forTarget(details, target, false);
    }

    public static OutputPattern forTarget(IPatternDetails details, AEKey target, boolean reuseOtherOutputs) {
        var original = unwrap(details);
        GenericStack selected = null;
        for (var output : original.getOutputs()) {
            if (target.equals(output.what())) {
                selected = output;
                break;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("Pattern does not produce target");
        }
        if (!reuseOtherOutputs) {
            return new OutputPattern(original, target, new GenericStack[] { selected });
        }
        var outputs = new ArrayList<GenericStack>();
        outputs.add(selected);
        for (var output : original.getOutputs()) {
            if (!target.equals(output.what())) {
                outputs.add(output);
            }
        }
        return new OutputPattern(original, target, outputs.toArray(GenericStack[]::new));
    }

    /** Keep the original index entry, adding one searchable view per other output. */
    public static void addAlternativeOutputs(Collection<IPatternDetails> patterns) {
        for (var pattern : new ArrayList<>(patterns)) {
            if (!isProcessing(pattern) || pattern instanceof OutputPattern) {
                continue;
            }
            for (var output : pattern.getOutputs()) {
                if (!output.what().equals(pattern.getPrimaryOutput().what())) {
                    patterns.add(indexed(pattern, output.what()));
                }
            }
        }
    }

    @Override
    public AEItemKey getDefinition() {
        return delegate.getDefinition();
    }

    @Override
    public IInput[] getInputs() {
        return delegate.getInputs();
    }

    @Override
    public GenericStack getPrimaryOutput() {
        for (var output : outputs) {
            if (target.equals(output.what())) {
                return output;
            }
        }
        throw new IllegalStateException("Missing requested output");
    }

    public GenericStack[] visibleOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return delegate.supportsPushInputsToExternalInventory();
    }

    @Override
    public void pushInputsToExternalInventory(KeyCounter[] inputHolder, PatternInputSink inputSink) {
        delegate.pushInputsToExternalInventory(inputHolder, inputSink);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BaseOutputPattern view && delegate.equals(view.delegate)
                && target.equals(view.target) && Arrays.equals(outputs, view.outputs);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(delegate, target) + Arrays.hashCode(outputs);
    }
}
