package dev.ae2byproductremover;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
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
        var providers = new NetworkCraftingProviders();
        var stock = new KeyCounter();
        stock.add(a, 10);
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
        var grid = proxy(IGrid.class, (method, args) -> switch (method.getName()) {
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
        var node = proxy(IGridNode.class, (method, args) -> switch (method.getName()) {
            case "getService" -> args[0] == ICraftingProvider.class ? provider : null;
            case "getGrid" -> grid;
            default -> null;
        });
        providers.addProvider(node);
        check(providers.getCraftingFor(b).size() == 1, "First output must remain craftable");
        check(providers.getCraftingFor(c).size() == 1, "Other output must become craftable");
        var requester = new ICraftingSimulationRequester() {
            @Override public IActionSource getActionSource() { return IActionSource.empty(); }
            @Override public IGridNode getGridNode() { return node; }
        };
        try {
            for (boolean reuse : new boolean[] { false, true }) {
                PlanningMode.loadWorld(reuse);
                var plan = plan(grid, requester, d);
                check(plan.usedItems().get(a) == (reuse ? 1 : 2), "Lite needs 2A; reuse needs 1A");
                var onlyB = plan(grid, requester, b);
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
            providers.removeProvider(node);
            check(providers.getCraftingFor(c).isEmpty(), "Removing a provider must remove alternative outputs");
            helper.succeed();
        } finally {
            PlanningMode.loadWorld(false);
        }
    }

    private static IPatternDetails pattern(AEKey[] inputs, AEKey[] outputs) {
        var inputStacks = java.util.Arrays.stream(inputs).map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
        var outputStacks = java.util.Arrays.stream(outputs).map(key -> new GenericStack(key, 1)).toArray(GenericStack[]::new);
        return new AEProcessingPattern(AEItemKey.of(PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks)));
    }

    private static CraftingPlan plan(IGrid grid, ICraftingSimulationRequester requester, AEKey output) throws Exception {
        var calculation = new CraftingCalculation(null, grid, requester, new GenericStack(output, 1),
                CalculationStrategy.REPORT_MISSING_ITEMS);
        var pauseCounter = CraftingCalculation.class.getDeclaredField("incTime");
        pauseCounter.setAccessible(true);
        pauseCounter.setInt(calculation, -100_000);
        var method = CraftingCalculation.class.getDeclaredMethod("runCraftAttempt", boolean.class, long.class);
        method.setAccessible(true);
        try {
            var result = (CraftingPlan) method.invoke(calculation, false, 1L);
            check(result != null, "Stock must satisfy the plan");
            return result;
        } catch (InvocationTargetException error) {
            throw new AssertionError("AE2 planning failed", error.getCause());
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
