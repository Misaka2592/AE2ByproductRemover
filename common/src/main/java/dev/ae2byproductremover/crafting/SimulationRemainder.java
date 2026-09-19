package dev.ae2byproductremover.crafting;

import appeng.api.stacks.AEKey;

/** Final inventory remaining after a successful simulation, excluding unreserved network stock. */
public interface SimulationRemainder {
    long ae2byproductremover$leftover(AEKey key);
}
