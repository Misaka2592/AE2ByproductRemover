/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.me.service.helpers.NetworkCraftingProviders;
import dev.ae2byproductremover.execution.ProcessingJob;
import dev.ae2byproductremover.mixin.execution.ExecutingCraftingJobAccessor;
import dev.ae2byproductremover.pattern.OutputPattern;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Dedicated GameTest source set: these checks are excluded from the distributed mod jar. */
@GameTestHolder("ae2byproductremover")
@PrefixGameTestTemplate(false)
public final class ForgeCraftingGameTest {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void processingPlansAndMixinLoading(GameTestHelper helper) throws Exception {
        var a = AEItemKey.of(Items.COBBLESTONE);
        var b = AEItemKey.of(Items.IRON_INGOT);
        var c = AEItemKey.of(Items.GOLD_INGOT);
        var d = AEItemKey.of(Items.DIAMOND);
        var patterns = List.of(pattern(new AEKey[] { a }, new AEKey[] { b, c }),
                pattern(new AEKey[] { b, c }, new AEKey[] { d }));
        var fixture = new Fixture(patterns, Map.of(a, 10L));
        check(fixture.providers.getCraftingFor(b).size() == 1, "First output must remain craftable");
        check(fixture.providers.getCraftingFor(c).size() == 1, "Other output must become craftable");
        try {
            for (boolean reuse : new boolean[] { false, true }) {
                PlanningMode.loadWorld(reuse);
                var plan = fixture.plan(d, 1);
                check(plan.usedItems().get(a) == (reuse ? 1 : 2), "Lite needs 2A; reuse needs 1A");
                var onlyB = fixture.plan(b, 1);
                for (var details : onlyB.patternTimes().keySet()) {
                    check(details.getOutputs().length == 1 && details.getOutputs()[0].what().equals(b),
                            "An unused other output must not be pending in the execution plan");
                }
            }
            // Force application of the execution/NBT mixins in Forge's real transform environment.
            check(ProcessingJob.class.isAssignableFrom(ExecutingCraftingJob.class), "Job mixin must be applied");
            check(ExecutingCraftingJobAccessor.class.isAssignableFrom(ExecutingCraftingJob.class),
                    "Job accessor must be applied");
            Class.forName(CraftingCpuLogic.class.getName());
            fixture.providers.removeProvider(fixture.nodes.get(0));
            check(fixture.providers.getCraftingFor(c).isEmpty(), "Removing a provider must remove alternative outputs");
            helper.succeed();
        } finally {
            PlanningMode.loadWorld(false);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void liteChecksOnlyTheRequestedOutputForEitherOutputOrder(GameTestHelper helper) throws Exception {
        var a = AEItemKey.of(Items.COBBLESTONE);
        var b = AEItemKey.of(Items.IRON_INGOT);
        var c = AEItemKey.of(Items.GOLD_INGOT);
        try {
            PlanningMode.loadWorld(false);
            for (var outputs : new AEKey[][] { { b, c }, { c, b } }) {
                var source = pattern(new AEKey[] { a }, outputs);
                var efficient = pattern(new GenericStack[] { new GenericStack(b, 1) },
                        new GenericStack[] { new GenericStack(c, 10) });
                var fixture = new Fixture(List.of(source), Map.of(a, 1L));
                fixture.addPatterns(List.of(efficient), 10);

                var plan = fixture.plan(c, 10);

                check(plan.usedItems().get(a) == 1, "Either output order must craft 10 C from 1 A in Lite mode");
                check(plan.patternTimes().size() == 2, "The plan must use both steps of A -> B -> 10 C");
                check(Long.valueOf(1).equals(plan.patternTimes().get(OutputPattern.indexed(source, b))),
                        "The source must be planned once for B");
                check(Long.valueOf(1).equals(plan.patternTimes().get(OutputPattern.indexed(efficient, c))),
                        "The high-priority B -> 10 C pattern must be planned once");
            }
            helper.succeed();
        } finally {
            PlanningMode.loadWorld(false);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void reuseKeepsConservativeRecursionChecksForEitherOutputOrder(GameTestHelper helper) throws Exception {
        var a = AEItemKey.of(Items.COBBLESTONE);
        var b = AEItemKey.of(Items.IRON_INGOT);
        var c = AEItemKey.of(Items.GOLD_INGOT);
        try {
            PlanningMode.loadWorld(true);
            for (var outputs : new AEKey[][] { { b, c }, { c, b } }) {
                var fixture = new Fixture(List.of(pattern(new AEKey[] { a }, outputs)), Map.of(a, 1L));
                fixture.addPatterns(List.of(pattern(new GenericStack[] { new GenericStack(b, 1) },
                        new GenericStack[] { new GenericStack(c, 10) })), 10);

                check(fixture.tryPlan(c, 10) == null,
                        "Reuse must consistently reject the ancestor C output with either output order");
            }
            helper.succeed();
        } finally {
            PlanningMode.loadWorld(false);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void trueInputCyclesRemainRejectedInBothModes(GameTestHelper helper) throws Exception {
        var a = AEItemKey.of(Items.COBBLESTONE);
        var b = AEItemKey.of(Items.IRON_INGOT);
        try {
            for (boolean reuse : new boolean[] { false, true }) {
                PlanningMode.loadWorld(reuse);
                var fixture = new Fixture(List.of(pattern(new AEKey[] { a }, new AEKey[] { b }),
                        pattern(new AEKey[] { b }, new AEKey[] { a })), Map.of());

                check(fixture.tryPlan(b, 1) == null, "A -> B -> A without stock must remain uncraftable");
            }
            helper.succeed();
        } finally {
            PlanningMode.loadWorld(false);
        }
    }

    private static IPatternDetails pattern(AEKey[] inputs, AEKey[] outputs) {
        var inputStacks = java.util.Arrays.stream(inputs).map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
        var outputStacks = java.util.Arrays.stream(outputs).map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
        return pattern(inputStacks, outputStacks);
    }

    private static IPatternDetails pattern(GenericStack[] inputs, GenericStack[] outputs) {
        return new AEProcessingPattern(AEItemKey.of(PatternDetailsHelper.encodeProcessingPattern(inputs, outputs)));
    }

    private static CraftingPlan tryPlan(IGrid grid, ICraftingSimulationRequester requester, AEKey output, long amount)
            throws Exception {
        var calculation = new CraftingCalculation(null, grid, requester, new GenericStack(output, amount),
                CalculationStrategy.REPORT_MISSING_ITEMS);
        var pauseCounter = CraftingCalculation.class.getDeclaredField("incTime");
        pauseCounter.setAccessible(true);
        pauseCounter.setInt(calculation, -100_000);
        var method = CraftingCalculation.class.getDeclaredMethod("runCraftAttempt", boolean.class, long.class);
        method.setAccessible(true);
        try {
            return (CraftingPlan) method.invoke(calculation, false, amount);
        } catch (InvocationTargetException error) {
            throw new AssertionError("AE2 planning failed", error.getCause());
        }
    }

    private static final class Fixture {
        private final NetworkCraftingProviders providers = new NetworkCraftingProviders();
        private final List<IGridNode> nodes = new ArrayList<>();
        private final IGrid grid;
        private final ICraftingSimulationRequester requester;

        private Fixture(List<IPatternDetails> patterns, Map<AEKey, Long> initialStock) {
            var stock = new KeyCounter();
            initialStock.forEach(stock::add);
            var inventory = proxy(MEStorage.class, (method, args) -> method.getName().equals("extract")
                    ? Math.min(stock.get((AEKey) args[0]), (long) args[1]) : null);
            var storage = proxy(IStorageService.class, (method, args) -> switch (method.getName()) {
                case "getCachedInventory" -> stock;
                case "getInventory" -> inventory;
                default -> null;
            });
            var crafting = proxy(ICraftingService.class, (method, args) -> switch (method.getName()) {
                case "getCraftingFor" -> providers.getCraftingFor((AEKey) args[0]);
                case "canEmitFor" -> false;
                default -> null;
            });
            grid = proxy(IGrid.class, (method, args) -> switch (method.getName()) {
                case "getCraftingService" -> crafting;
                case "getStorageService" -> storage;
                default -> null;
            });
            addPatterns(patterns, 0);
            requester = new ICraftingSimulationRequester() {
                @Override public IActionSource getActionSource() { return IActionSource.empty(); }
                @Override public IGridNode getGridNode() { return nodes.get(0); }
            };
        }

        private void addPatterns(List<IPatternDetails> patterns, int priority) {
            var provider = proxy(ICraftingProvider.class, (method, args) -> switch (method.getName()) {
                case "getAvailablePatterns" -> patterns;
                case "getEmitableItems" -> Set.of();
                case "getPatternPriority" -> priority;
                default -> null;
            });
            var node = proxy(IGridNode.class, (method, args) -> switch (method.getName()) {
                case "getService" -> args[0] == ICraftingProvider.class ? provider : null;
                case "getGrid" -> grid;
                default -> null;
            });
            nodes.add(node);
            providers.addProvider(node);
        }

        private CraftingPlan tryPlan(AEKey output, long amount) throws Exception {
            return ForgeCraftingGameTest.tryPlan(grid, requester, output, amount);
        }

        private CraftingPlan plan(AEKey output, long amount) throws Exception {
            var result = tryPlan(output, amount);
            check(result != null, "Stock must satisfy the plan");
            return result;
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static <T> T proxy(Class<T> type, BiFunction<Method, Object[], Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (object, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> object == args[0];
                    case "hashCode" -> System.identityHashCode(object);
                    case "toString" -> "GameTest " + type.getSimpleName();
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
