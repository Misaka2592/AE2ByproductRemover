/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.compat;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEItems;
import appeng.crafting.CraftingLink;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.me.service.CraftingService;
import cn.dancingsnow.neoecoae.api.me.ECOCraftingCPU;
import dev.ae2byproductremover.PlanningMode;
import dev.ae2byproductremover.pattern.OutputPattern;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Runs each addon's actual CPU dispatch, completion and NBT entry points after its mixins load. */
class AddonCpuCompatibilityTest {
    private static final AEKey INPUT = AEItemKey.of(Items.IRON_INGOT);
    private static final AEKey TARGET = AEItemKey.of(Items.GOLD_INGOT);
    private static final AEKey OTHER = AEItemKey.of(Items.COPPER_INGOT);
    private static final IPatternDetailsDecoder DECODER = new IPatternDetailsDecoder() {
        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return AEItems.PROCESSING_PATTERN.isSameAs(stack);
        }

        @Override
        public IPatternDetails decodePattern(AEItemKey key, Level level) {
            if (level == null && key != null && key.getItem() == AEItems.PROCESSING_PATTERN.asItem()) {
                return ((EncodedPatternItem<?>) key.getItem()).decode(key, null);
            }
            return null;
        }
    };

    enum CpuType {
        ADVANCED_AE("net.pedroksl.advanced_ae.common.logic.ExecutingCraftingJob"),
        NEO_ECO_AE("cn.dancingsnow.neoecoae.api.me.ExecutingCraftingJob");

        final String jobClass;
        CpuType(String jobClass) { this.jobClass = jobClass; }
    }

    @BeforeAll
    static void allowWorldlessProcessingDecode() {
        PatternDetailsHelper.registerDecoder(DECODER);
    }

    @AfterAll
    static void restoreDecoderAndMode() throws Exception {
        ((List<?>) field(PatternDetailsHelper.class, null, "DECODERS")).remove(DECODER);
        PlanningMode.loadWorld(false);
    }

    @ParameterizedTest
    @EnumSource(CpuType.class)
    void dispatchesEveryPlannedCraftWithOriginalPatternAndOnlyWaitsForSelectedOutput(CpuType type)
            throws Exception {
        var original = pattern();
        var view = OutputPattern.indexed(original, TARGET);
        var harness = new Harness(type);
        harness.install(Map.of(view, 2L));
        var pushed = new AtomicInteger();
        harness.service.addGlobalCraftingProvider(new ICraftingProvider() {
            @Override public List<IPatternDetails> getAvailablePatterns() { return List.of(original); }
            @Override public boolean isBusy() { return false; }
            @Override public boolean pushPattern(IPatternDetails received, KeyCounter[] inputs) {
                assertSame(original, received);
                assertEquals(1, inputs[0].get(INPUT));
                pushed.incrementAndGet();
                return true;
            }
        });
        harness.inventory().insert(INPUT, 2, Actionable.MODULATE);

        assertEquals(1, harness.execute());
        assertEquals(0L, harness.call("getWaitingFor", new Class<?>[] { AEKey.class }, OTHER));
        // AdvancedAE's standalone link leaves the final item for the caller to store;
        // ECO instead accepts it into its final-output buffer and delivers it to storage itself.
        assertEquals(type == CpuType.ADVANCED_AE ? 0L : 1L, harness.call("insert",
                new Class<?>[] { AEKey.class, long.class, Actionable.class }, TARGET, 1L, Actionable.MODULATE));
        assertTrue((boolean) harness.call("hasJob"));
        assertTrue(harness.job().ae2byproductremover$hasPendingCrafts());
        assertEquals(0, harness.job().ae2byproductremover$getRemainingAmount());
        if (harness.cpu instanceof AdvCraftingCPU advanced) {
            assertNotNull(advanced.finalOutput);
        }

        assertEquals(1, harness.execute());
        assertEquals(2, pushed.get());
        assertFalse((boolean) harness.call("hasJob"));
        assertEquals(0, harness.inventory().list.get(INPUT));
    }

    @ParameterizedTest
    @EnumSource(CpuType.class)
    void roundTripsDistinctOutputViewsAndCountsAfterConfigurationChanges(CpuType type) throws Exception {
        var original = pattern();
        var first = OutputPattern.indexed(original, TARGET);
        var second = OutputPattern.indexed(original, OTHER);
        var expected = new LinkedHashMap<IPatternDetails, Long>();
        expected.put(first, 2L);
        expected.put(second, 5L);
        var harness = new Harness(type);
        harness.install(expected);
        var saved = harness.save();
        PlanningMode.loadWorld(true);
        var restored = new Harness(type);
        restored.restore(saved);

        assertTrue(restored.job().ae2byproductremover$hasProcessingPlan());
        assertEquals(expected, counts(restored.job()));
        assertEquals(4L, restored.call("getPendingOutputs", new Class<?>[] { AEKey.class }, TARGET));
        assertEquals(15L, restored.call("getPendingOutputs", new Class<?>[] { AEKey.class }, OTHER));
        PlanningMode.loadWorld(false);
    }

    @ParameterizedTest
    @EnumSource(CpuType.class)
    void restoresReuseOutputMaskWithoutAddingUnusedProducts(CpuType type) throws Exception {
        var original = pattern();
        var view = new OutputPattern(original, TARGET,
                new GenericStack[] { new GenericStack(TARGET, 2), new GenericStack(OTHER, 3) });
        var harness = new Harness(type);
        harness.install(Map.of(view, 4L));
        var restored = new Harness(type);
        restored.restore(harness.save());

        assertEquals(Map.of(view, 4L), counts(restored.job()));
        assertEquals(0L, restored.call("getPendingOutputs", new Class<?>[] { AEKey.class },
                AEItemKey.of(Items.REDSTONE)));
    }

    @ParameterizedTest
    @EnumSource(CpuType.class)
    void keepsProcessingMarkerAfterLastTaskAndFinishesRestoredJobWithoutSurplus(CpuType type) throws Exception {
        var harness = new Harness(type);
        harness.install(Map.of(OutputPattern.indexed(pattern(), TARGET), 1L));
        harness.job().ae2byproductremover$getTasks().clear();
        setField(harness.job().getClass(), harness.job(), "remainingAmount", 0L);
        var restored = new Harness(type);
        restored.restore(harness.save());

        assertTrue(restored.job().ae2byproductremover$hasProcessingPlan());
        assertEquals(0, restored.execute());
        assertFalse((boolean) restored.call("hasJob"));
    }

    @ParameterizedTest
    @EnumSource(CpuType.class)
    void cancellationStillDiscardsPendingTasks(CpuType type) throws Exception {
        var harness = new Harness(type);
        harness.install(Map.of(OutputPattern.indexed(pattern(), TARGET), 2L));
        var link = (CraftingLink) harness.call("getLastLink");
        harness.call("cancel");
        assertFalse((boolean) harness.call("hasJob"));
        assertTrue(link.isCanceled());
    }

    @ParameterizedTest
    @EnumSource(CpuType.class)
    void oldUnmarkedJobsKeepTheirOriginalPatterns(CpuType type) throws Exception {
        var original = pattern();
        var harness = new Harness(type);
        harness.install(Map.of(original, 3L));
        var saved = harness.save();
        var restored = new Harness(type);
        restored.restore(saved);
        assertFalse(saved.getCompound("job").contains("ae2byproductremover:processingPlan"));
        assertFalse(restored.job().ae2byproductremover$hasProcessingPlan());
        assertEquals(Map.of(original, 3L), counts(restored.job()));
    }

    private static AEProcessingPattern pattern() {
        return new AEProcessingPattern(AEItemKey.of(PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(INPUT, 1)),
                List.of(new GenericStack(TARGET, 2), new GenericStack(OTHER, 3),
                        new GenericStack(AEItemKey.of(Items.REDSTONE), 4)))));
    }

    private static Map<IPatternDetails, Long> counts(AddonJobState job) throws Exception {
        var result = new LinkedHashMap<IPatternDetails, Long>();
        for (var entry : job.ae2byproductremover$getTasks().entrySet()) {
            result.put(entry.getKey(), (long) field(entry.getValue().getClass(), entry.getValue(), "value"));
        }
        return result;
    }

    private static final class Harness {
        final CpuType type;
        final ICraftingCPU cpu;
        final Object logic;
        final IEnergyService energy = proxy(IEnergyService.class, (name, args) ->
                name.equals("extractAEPower") ? args[0] : null);
        final MEStorage inventory = proxy(MEStorage.class, (name, args) ->
                name.equals("insert") ? args[1] : null);
        final IStorageService storage = proxy(IStorageService.class, (name, args) ->
                name.equals("getInventory") ? inventory : null);
        final CraftingService service = new CraftingService(null, storage, energy);
        final IGrid grid = proxy(IGrid.class, (name, args) -> switch (name) {
            case "getStorageService" -> storage;
            case "getCraftingService" -> service;
            case "getMachines" -> Set.of();
            default -> null;
        });

        Harness(CpuType type) {
            this.type = type;
            if (type == CpuType.ADVANCED_AE) {
                var advanced = new AdvCraftingCPU(null, UUID.randomUUID(), 1024) {
                    @Override public IGrid getGrid() { return grid; }
                    @Override public Level getLevel() { return null; }
                    @Override public void markDirty() { }
                };
                cpu = advanced;
                logic = advanced.craftingLogic;
            } else {
                var eco = new ECOCraftingCPU(null, 1024, null) {
                    @Override public IGrid getGrid() { return grid; }
                    @Override public Level getLevel() { return null; }
                    @Override public void markDirty() { }
                    @Override public appeng.api.networking.security.IActionSource getActionSource() { return null; }
                };
                cpu = eco;
                logic = eco.getLogic();
            }
        }

        void install(Map<IPatternDetails, Long> tasks) throws Exception {
            var plan = new CraftingPlan(new GenericStack(TARGET, 1), 1, false, false,
                    new KeyCounter(), new KeyCounter(), new KeyCounter(), tasks);
            var jobClass = Class.forName(type.jobClass);
            var listenerType = Class.forName(type.jobClass + "$CraftingDifferenceListener");
            var listener = Proxy.newProxyInstance(listenerType.getClassLoader(),
                    new Class<?>[] { listenerType }, (proxy, method, args) -> null);
            var ctor = jobClass.getDeclaredConstructor(ICraftingPlan.class, listenerType,
                    CraftingLink.class, Integer.class);
            ctor.setAccessible(true);
            var link = new CraftingLink(CraftingCpuHelper.generateLinkData(UUID.randomUUID(), true, false), cpu);
            setField(logic.getClass(), logic, "job", ctor.newInstance(plan, listener, link, null));
        }

        AddonJobState job() throws Exception {
            return (AddonJobState) field(logic.getClass(), logic, "job");
        }

        ListCraftingInventory inventory() throws Exception {
            return (ListCraftingInventory) call("getInventory");
        }

        int execute() throws Exception {
            return (int) call("executeCrafting",
                    new Class<?>[] { int.class, CraftingService.class, IEnergyService.class, Level.class },
                    1, service, energy, null);
        }

        CompoundTag save() throws Exception {
            var tag = new CompoundTag();
            call("writeToNBT", new Class<?>[] { CompoundTag.class, HolderLookup.Provider.class }, tag, registries());
            return tag;
        }

        void restore(CompoundTag tag) throws Exception {
            call("readFromNBT", new Class<?>[] { CompoundTag.class, HolderLookup.Provider.class }, tag, registries());
        }

        Object call(String name) throws Exception { return call(name, new Class<?>[0]); }
        Object call(String name, Class<?>[] types, Object... args) throws Exception {
            try {
                return logic.getClass().getMethod(name, types).invoke(logic, args);
            } catch (InvocationTargetException error) {
                throw new AssertionError("Addon CPU entry point failed: " + name, error.getCause());
            }
        }
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static Object field(Class<?> owner, Object object, String name) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void setField(Class<?> owner, Object object, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (object, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(object);
                    if (method.getName().equals("equals")) return object == args[0];
                    var result = calls.apply(method.getName(), args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                }));
    }
}
