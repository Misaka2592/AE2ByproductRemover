package dev.ae2byproductremover.compat.thunderbolt;

import dev.ae2byproductremover.PlanningMode;

/** Carries the owning calculation's captured mode through Thunderbolt's static planner. */
public final class FastPlanningContext {
    private static final ThreadLocal<Boolean> REUSE_OUTPUTS = new ThreadLocal<>();

    private FastPlanningContext() {
    }

    public static Boolean enter(boolean reuse) {
        Boolean previous = REUSE_OUTPUTS.get();
        REUSE_OUTPUTS.set(reuse);
        return previous;
    }

    public static void restore(Boolean previous) {
        if (previous == null) {
            REUSE_OUTPUTS.remove();
        } else {
            REUSE_OUTPUTS.set(previous);
        }
    }

    public static boolean reuseOutputs() {
        Boolean captured = REUSE_OUTPUTS.get();
        return captured != null ? captured : PlanningMode.useByproducts();
    }
}
