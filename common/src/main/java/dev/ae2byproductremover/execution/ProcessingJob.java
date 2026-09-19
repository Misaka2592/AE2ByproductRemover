package dev.ae2byproductremover.execution;

/** Remembers whether the submitted job contains processing output views, even after its tasks are dispatched. */
public interface ProcessingJob {
    boolean ae2byproductremover$hasProcessingPlan();
}
