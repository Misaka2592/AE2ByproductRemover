package dev.ae2byproductremover.compat;

import java.util.Map;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import dev.ae2byproductremover.execution.ProcessingJob;

/** The copied AE2 CPU implementations retain the same task/output state. */
public interface AddonJobState extends ProcessingJob {
    Map<IPatternDetails, ?> ae2byproductremover$getTasks();

    long ae2byproductremover$getRemainingAmount();

    GenericStack ae2byproductremover$getFinalOutput();

    boolean ae2byproductremover$hasPendingCrafts();
}
