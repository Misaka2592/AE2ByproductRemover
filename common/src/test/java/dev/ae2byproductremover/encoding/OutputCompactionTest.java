package dev.ae2byproductremover.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OutputCompactionTest {
    @Test
    void acceptsAnOutputAfterAnEmptyFirstSlot() {
        var slots = Arrays.asList(null, "B", null);

        assertEquals(List.of("B"), OutputCompaction.compact(slots.size(), slots::get));
    }

    @Test
    void removesLeadingMiddleAndTrailingHolesInOrder() {
        var slots = Arrays.asList(null, "B", null, "C", null, null, "D", null);

        assertEquals(List.of("B", "C", "D"), OutputCompaction.compact(slots.size(), slots::get));
        assertEquals(Arrays.asList(null, "B", null, "C", null, null, "D", null), slots);
    }

    @Test
    void keepsRepeatedOutputsAndTheirOriginalAmounts() {
        var firstBatch = Map.entry("B", 2L);
        var secondBatch = Map.entry("B", 5L);
        var fluid = Map.entry("water", 1_000L);
        var slots = Arrays.asList(null, firstBatch, null, secondBatch, fluid);

        var compacted = OutputCompaction.compact(slots.size(), slots::get);

        assertEquals(List.of(firstBatch, secondBatch, fluid), compacted);
        assertSame(firstBatch, compacted.get(0));
        assertSame(secondBatch, compacted.get(1));
        assertSame(fluid, compacted.get(2));
    }

    @Test
    void allEmptySlotsProvideNoEncodableOutputs() {
        var slots = Arrays.asList(null, null, null);

        assertTrue(OutputCompaction.compact(slots.size(), slots::get).isEmpty());
        assertTrue(OutputCompaction.compact(0, index -> {
            throw new AssertionError("An empty inventory has no readable slots");
        }).isEmpty());
    }
}
