/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.execution;

/** Remembers whether the submitted job contains processing output views, even after its tasks are dispatched. */
public interface ProcessingJob {
    boolean ae2byproductremover$hasProcessingPlan();
}
