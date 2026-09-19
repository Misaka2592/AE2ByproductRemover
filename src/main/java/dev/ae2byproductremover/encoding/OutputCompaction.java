/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.encoding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** Removes empty output slots without merging, reordering, or changing their contents. */
public final class OutputCompaction {
    private OutputCompaction() {
    }

    public static <T> List<T> compact(int slotCount, IntFunction<T> outputAt) {
        var outputs = new ArrayList<T>(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            var output = outputAt.apply(slot);
            if (output != null) {
                outputs.add(output);
            }
        }
        return outputs;
    }
}
