/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.me.service.helpers.NetworkCraftingProviders;
import appeng.menu.me.crafting.CraftingPlanSummary;
import dev.ae2byproductremover.pattern.OutputPattern;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Exercises Thunderbolt's installed planner and AE's actual confirmation summary after mixin application. */
class ThunderboltPlanningTest {
    private static final AEKey A = AEItemKey.of(Items.COBBLESTONE);
    private static final AEKey B = AEItemKey.of(Items.IRON_INGOT);
    private static final AEKey C = AEItemKey.of(Items.GOLD_INGOT);
    private static final AEKey D = AEItemKey.of(Items.DIAMOND);
    private static final AEKey E = AEItemKey.of(Items.REDSTONE);

    @AfterEach
    void restoreDefaultMode() {
        PlanningMode.loadWorld(false);
    }

    @Test
    void primaryAndAlternativeOrdersExcludeUnusedProductsFromActualConfirmationSummary() throws Exception {
        var fixture = new Fixture(List.of(pattern(List.of(stack(A)), List.of(stack(B), stack(C)))), Map.of(A, 10L));
        for (boolean reuse : new boolean[] { false, true }) {
            for (AEKey target : List.of(B, C)) {
                var plan = fixture.calculate(target, 1, reuse);
                var view = assertInstanceOf(OutputPattern.class, plan.patternTimes().keySet().iterator().next());
                assertEquals(List.of(stack(target)), view.getOutputs());
                var summary = CraftingPlanSummary.fromJob(fixture.grid, null, plan);
                assertEquals(2, summary.getEntries().size(), "Only the input and requested product belong in the preview");
                assertEquals(1, processingTimes(plan, A));
            }
        }
    }

    @Test
    void independentModeKeepsTwoOperationsAndReuseModePlansOne() throws Exception {
        var fixture = chain(Map.of(A, 10L));
        var lite = fixture.calculate(D, 1, false);
        assertEquals(2, lite.usedItems().get(A));
        assertEquals(2, processingTimes(lite, A));
        assertEquals(2, lite.patternTimes().keySet().stream()
                .filter(details -> details.getInputs()[0].getPossibleInputs()[0].what().equals(A)).count());
        var reuse = fixture.calculate(D, 1, true);
        assertEquals(1, reuse.usedItems().get(A));
        assertEquals(1, processingTimes(reuse, A));
        assertTrue(reuse.patternTimes().keySet().stream()
                .anyMatch(details -> details.getOutputs().stream().anyMatch(output -> output.what().equals(C))));
    }

    @Test
    void existingStockStillSatisfiesIndependentDemand() throws Exception {
        var fixture = chain(Map.of(A, 10L, C, 1L));
        for (boolean reuse : new boolean[] { false, true }) {
            assertEquals(1, fixture.calculate(D, 1, reuse).usedItems().get(A));
        }
    }

    @Test
    void alternativeOutputRetainsBatchYield() throws Exception {
        var fixture = new Fixture(List.of(pattern(List.of(stack(A)),
                List.of(new GenericStack(B, 2), new GenericStack(C, 3)))), Map.of(A, 10L));
        var plan = fixture.calculate(C, 5, false);
        assertEquals(2, plan.usedItems().get(A));
        assertEquals(2, processingTimes(plan, A));
        assertEquals(List.of(new GenericStack(C, 3)), plan.patternTimes().keySet().iterator().next().getOutputs());
    }

    @Test
    void sameTargetBatchRemainderIsSharedAcrossBranches() throws Exception {
        var fixture = new Fixture(List.of(
                pattern(List.of(stack(A)), List.of(new GenericStack(B, 2), stack(C))),
                pattern(List.of(stack(B)), List.of(stack(E))),
                pattern(List.of(stack(B), stack(E)), List.of(stack(D)))), Map.of(A, 10L));
        var plan = fixture.calculate(D, 1, false);
        assertEquals(1, plan.usedItems().get(A));
        assertEquals(1, processingTimes(plan, A));
    }

    @Test
    void fluidOutputCanBeRequestedIndependently() throws Exception {
        var water = AEFluidKey.of(Fluids.WATER);
        var fixture = new Fixture(List.of(pattern(List.of(stack(A)),
                List.of(stack(B), new GenericStack(water, 1000)))), Map.of(A, 10L));
        var plan = fixture.calculate(water, 1500, false);
        assertEquals(2, plan.usedItems().get(A));
        assertEquals(List.of(new GenericStack(water, 1000)), plan.patternTimes().keySet().iterator().next().getOutputs());
    }

    @Test
    void fastPlannerUsesModeCapturedByCalculation() throws Exception {
        var fixture = chain(Map.of(A, 10L));
        for (boolean reuse : new boolean[] { false, true }) {
            PlanningMode.loadWorld(reuse);
            var calculation = fixture.newCalculation(D, 1);
            PlanningMode.loadWorld(!reuse);
            assertEquals(reuse ? 1 : 2, runFastAttempt(calculation, 1).usedItems().get(A));
            assertEquals(reuse ? 2 : 1, runFastAttempt(fixture.newCalculation(D, 1), 1).usedItems().get(A));
        }
    }

    private static Fixture chain(Map<AEKey, Long> stock) {
        return new Fixture(List.of(pattern(List.of(stack(A)), List.of(stack(B), stack(C))),
                pattern(List.of(stack(B), stack(C)), List.of(stack(D)))), stock);
    }

    private static GenericStack stack(AEKey key) {
        return new GenericStack(key, 1);
    }

    private static IPatternDetails pattern(List<GenericStack> inputs, List<GenericStack> outputs) {
        return new AEProcessingPattern(AEItemKey.of(PatternDetailsHelper.encodeProcessingPattern(inputs, outputs)));
    }

    private static long processingTimes(CraftingPlan plan, AEKey input) {
        return plan.patternTimes().entrySet().stream()
                .filter(entry -> entry.getKey().getInputs()[0].getPossibleInputs()[0].what().equals(input))
                .mapToLong(Map.Entry::getValue).sum();
    }

    private static CraftingPlan runFastAttempt(CraftingCalculation calculation, long amount) throws Exception {
        // Enable the real addon injector, then fail if it silently falls back to AE's native planner.
        calculation.getClass().getMethod("ae2lt$setFastPlanningEnabled", boolean.class).invoke(calculation, true);
        var pauses = CraftingCalculation.class.getDeclaredField("incTime");
        pauses.setAccessible(true);
        pauses.setInt(calculation, -100_000);
        var attempt = CraftingCalculation.class.getDeclaredMethod("runCraftAttempt", boolean.class, long.class);
        attempt.setAccessible(true);
        try {
            var result = (CraftingPlan) attempt.invoke(calculation, false, amount);
            assertNotNull(result);
            assertEquals(1, counter(calculation, "thunderbolt$fastHandledAttempts"));
            assertEquals(0, counter(calculation, "thunderbolt$fastFallbackAttempts"));
            assertEquals(0, counter(calculation, "thunderbolt$fastFailures"));
            return result;
        } catch (InvocationTargetException error) {
            throw new AssertionError("Thunderbolt planning failed", error.getCause());
        }
    }

    private static int counter(CraftingCalculation calculation, String name) throws Exception {
        var field = CraftingCalculation.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(calculation);
    }

    private static final class Fixture {
        private final NetworkCraftingProviders providers = new NetworkCraftingProviders();
        private final IGrid grid;
        private final IGridNode node;

        private Fixture(List<IPatternDetails> patterns, Map<AEKey, Long> stock) {
            var available = new KeyCounter();
            stock.forEach(available::add);
            var storage = proxy(IStorageService.class,
                    (method, args) -> method.getName().equals("getCachedInventory") ? available : null);
            var crafting = proxy(ICraftingService.class, (method, args) -> switch (method.getName()) {
                case "getCraftingFor" -> providers.getCraftingFor((AEKey) args[0]);
                case "canEmitFor" -> false;
                case "getFuzzyCraftable" -> null;
                default -> null;
            });
            grid = proxy(IGrid.class, (method, args) -> switch (method.getName()) {
                case "getCraftingService" -> crafting;
                case "getStorageService" -> storage;
                default -> null;
            });
            var provider = proxy(ICraftingProvider.class, (method, args) -> switch (method.getName()) {
                case "getAvailablePatterns" -> patterns;
                case "getEmitableItems" -> Set.of();
                case "getPatternPriority" -> 0;
                default -> null;
            });
            node = proxy(IGridNode.class, (method, args) -> switch (method.getName()) {
                case "getService" -> args[0] == ICraftingProvider.class ? provider : null;
                case "getGrid" -> grid;
                default -> null;
            });
            providers.addProvider(node);
        }

        private CraftingCalculation newCalculation(AEKey output, long amount) {
            var requester = new ICraftingSimulationRequester() {
                @Override public appeng.api.networking.security.IActionSource getActionSource() { return null; }
                @Override public IGridNode getGridNode() { return node; }
            };
            return new CraftingCalculation(null, grid, requester,
                    new GenericStack(output, amount), CalculationStrategy.REPORT_MISSING_ITEMS);
        }

        private CraftingPlan calculate(AEKey output, long amount, boolean reuse) throws Exception {
            PlanningMode.loadWorld(reuse);
            return runFastAttempt(newCalculation(output, amount), amount);
        }
    }

    private static <T> T proxy(Class<T> type, BiFunction<Method, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (object, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> object == args[0];
                    case "hashCode" -> System.identityHashCode(object);
                    case "toString" -> "Thunderbolt fixture " + type.getSimpleName();
                    default -> null;
                };
            }
            var result = handler.apply(method, args);
            if (result != null) return result;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            if (method.getReturnType() == Optional.class) return Optional.empty();
            return null;
        }));
    }
}
