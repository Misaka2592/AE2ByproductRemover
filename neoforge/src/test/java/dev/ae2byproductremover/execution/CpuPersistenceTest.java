package dev.ae2byproductremover.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.crafting.CraftingMonitorBlockEntity;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.crafting.CraftingLink;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.me.service.CraftingService;

import dev.ae2byproductremover.mixin.execution.ExecutingCraftingJobAccessor;
import dev.ae2byproductremover.mixin.execution.TaskProgressAccessor;
import dev.ae2byproductremover.pattern.OutputPattern;

/** Uses the real AE2 CPU, NBT codecs and applied mixins in NeoForge's unit-test runtime. */
class CpuPersistenceTest {
    private static final IPatternDetailsDecoder NO_WORLD_PROCESSING_DECODER = new IPatternDetailsDecoder() {
        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return AEItems.PROCESSING_PATTERN.isSameAs(stack);
        }

        @Override
        public IPatternDetails decodePattern(AEItemKey key, Level level) {
            // AEPatternDecoder requires a world even for processing patterns. These tests have no world;
            // use the same registered item's decoder, whose processing format is independent of a level.
            if (level == null && key != null && key.getItem() == AEItems.PROCESSING_PATTERN.asItem()) {
                return ((EncodedPatternItem<?>) key.getItem()).decode(key, null);
            }
            return null;
        }
    };

    @BeforeAll
    static void allowProcessingDecodingWithoutCreatingAWorld() {
        PatternDetailsHelper.registerDecoder(NO_WORLD_PROCESSING_DECODER);
    }

    @AfterAll
    static void removeWorldlessDecoder() throws Exception {
        ((List<?>) field(PatternDetailsHelper.class, null, "DECODERS")).remove(NO_WORLD_PROCESSING_DECODER);
    }

    @Test
    void dispatchesTheRemainingConfirmedCraftAfterTheRequestedOutputHasArrived() throws Exception {
        var original = pattern();
        var view = OutputPattern.indexed(original, AEItemKey.of(Items.GOLD_INGOT));
        var cpu = cpuWith(Map.of(view, 2L));
        var monitor = attachMonitor(cpu);
        var pushed = new AtomicInteger();
        var energy = (IEnergyService) Proxy.newProxyInstance(IEnergyService.class.getClassLoader(),
                new Class<?>[] { IEnergyService.class }, (proxy, method, args) -> {
                    if (method.getName().equals("extractAEPower")) {
                        return args[0];
                    }
                    throw new AssertionError("Unexpected energy call: " + method);
                });
        var storage = (IStorageService) Proxy.newProxyInstance(IStorageService.class.getClassLoader(),
                new Class<?>[] { IStorageService.class }, (proxy, method, args) -> {
                    if (method.getName().equals("addGlobalStorageProvider")) {
                        return null;
                    }
                    throw new AssertionError("Unexpected storage call: " + method);
                });
        var service = new CraftingService(null, storage, energy);
        service.addGlobalCraftingProvider(new ICraftingProvider() {
            @Override
            public List<IPatternDetails> getAvailablePatterns() {
                return List.of(original);
            }

            @Override
            public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
                assertSame(original, details, "Machines must receive their own original pattern");
                assertEquals(1, inputs[0].get(AEItemKey.of(Items.IRON_INGOT)));
                pushed.incrementAndGet();
                return true;
            }

            @Override
            public boolean isBusy() {
                return false;
            }
        });
        cpu.getInventory().insert(AEItemKey.of(Items.IRON_INGOT), 2, Actionable.MODULATE);

        assertEquals(1, cpu.executeCrafting(1, service, energy, null));
        assertEquals(1, pushed.get());
        assertEquals(1L, pendingCrafts(cpu).get(view));
        assertEquals(0, cpu.getWaitingFor(AEItemKey.of(Items.COPPER_INGOT)));
        cpu.insert(AEItemKey.of(Items.GOLD_INGOT), 1, Actionable.MODULATE);
        assertTrue(cpu.hasJob());
        assertNotNull(monitor.getJobProgress());

        assertEquals(1, cpu.executeCrafting(1, service, energy, null));

        assertEquals(2, pushed.get());
        assertEquals(0, cpu.getStored(AEItemKey.of(Items.IRON_INGOT)));
        assertFalse(cpu.hasJob());
        assertNull(monitor.getJobProgress());
    }

    @Test
    void preservesSeparateTargetsAndCraftCountsFromTheSamePattern() throws Exception {
        var original = pattern();
        var gold = OutputPattern.indexed(original, AEItemKey.of(Items.GOLD_INGOT));
        var copper = OutputPattern.indexed(original, AEItemKey.of(Items.COPPER_INGOT));
        var crafts = new LinkedHashMap<IPatternDetails, Long>();
        crafts.put(gold, 2L);
        crafts.put(copper, 5L);
        var cpu = cpuWith(crafts);

        var saved = save(cpu);
        var restored = restore(saved);

        assertEquals(crafts, pendingCrafts(restored));
        assertTrue(((ProcessingJob) currentJob(restored)).ae2byproductremover$hasProcessingPlan());
        assertTrue(saved.getCompound("job").getBoolean("ae2byproductremover:processingPlan"));
        assertEquals(2, pendingCrafts(restored).size());
        for (var details : pendingCrafts(restored).keySet()) {
            var view = assertInstanceOf(OutputPattern.class, details);
            assertEquals(1, view.getOutputs().size());
            assertEquals(original.getDefinition(), view.getDefinition());
        }
    }

    @Test
    void preservesOnlyTheOutputsActuallyUsedByAReusePlan() throws Exception {
        var original = pattern();
        var gold = new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 2);
        var copper = new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 3);
        var view = new OutputPattern(original, gold.what(), new GenericStack[] { gold, copper });
        var cpu = cpuWith(Map.of(view, 4L));

        var restored = restore(save(cpu));
        var restoredView = assertInstanceOf(OutputPattern.class, pendingCrafts(restored).keySet().iterator().next());

        assertEquals(List.of(gold, copper), restoredView.getOutputs());
        assertEquals(4L, pendingCrafts(restored).get(restoredView));
        assertEquals(0, restored.getPendingOutputs(AEItemKey.of(Items.REDSTONE)));
        assertEquals(8, restored.getPendingOutputs(gold.what()));
        assertEquals(12, restored.getPendingOutputs(copper.what()));
    }

    @Test
    void earlyRequestedOutputDoesNotDiscardPlannedCraftsAndUnusedReturnsDoNotBlockCompletion() throws Exception {
        var view = OutputPattern.indexed(pattern(), AEItemKey.of(Items.GOLD_INGOT));
        var cpu = cpuWith(Map.of(view, 2L));
        var job = currentJob(cpu);
        var waiting = (ListCraftingInventory) field(ExecutingCraftingJob.class, job, "waitingFor");
        waiting.insert(AEItemKey.of(Items.GOLD_INGOT), 1, Actionable.MODULATE);
        waiting.insert(AEItemKey.of(Items.COPPER_INGOT), 99, Actionable.MODULATE);
        var monitor = attachMonitor(cpu);

        cpu.insert(AEItemKey.of(Items.GOLD_INGOT), 1, Actionable.MODULATE);

        assertTrue(cpu.hasJob());
        assertSame(job, currentJob(cpu));
        assertEquals(2L, pendingCrafts(cpu).get(view));
        assertEquals(0, ((ExecutingCraftingJobAccessor) job).ae2byproductremover$getRemainingAmount());
        assertNotNull(monitor.getJobProgress());
        assertEquals(AEItemKey.of(Items.GOLD_INGOT), monitor.getJobProgress().what());

        // All confirmed operations have now been dispatched; surplus deliveries are still outstanding.
        ((ExecutingCraftingJobAccessor) job).ae2byproductremover$getTasks().clear();
        assertEquals(0, cpu.executeCrafting(1, null, null, null));
        assertFalse(cpu.hasJob());
        assertNull(monitor.getJobProgress());
        assertEquals(0, cpu.getWaitingFor(AEItemKey.of(Items.COPPER_INGOT)));
    }

    @Test
    void explicitlyCancellingStillCancelsPendingProcessingCrafts() throws Exception {
        var view = OutputPattern.indexed(pattern(), AEItemKey.of(Items.GOLD_INGOT));
        var cpu = cpuWith(Map.of(view, 2L));
        var link = cpu.getLastLink();

        cpu.cancel();

        assertFalse(cpu.hasJob());
        assertTrue(link.isCanceled());
    }

    @Test
    void remembersProcessingSemanticsAfterTheLastTaskWasDispatchedBeforeSaving() throws Exception {
        var view = OutputPattern.indexed(pattern(), AEItemKey.of(Items.GOLD_INGOT));
        var cpu = cpuWith(Map.of(view, 1L));
        var job = currentJob(cpu);
        ((ExecutingCraftingJobAccessor) job).ae2byproductremover$getTasks().clear();
        setField(ExecutingCraftingJob.class, job, "remainingAmount", 0L);

        var restored = restore(save(cpu));

        assertTrue(((ProcessingJob) currentJob(restored)).ae2byproductremover$hasProcessingPlan());
        assertTrue(pendingCrafts(restored).isEmpty());
        restored.executeCrafting(1, null, null, null);
        assertFalse(restored.hasJob());
    }

    @Test
    void leavesJobsSavedWithoutOutputMetadataAsOriginalPatterns() throws Exception {
        var original = pattern();
        var cpu = cpuWith(Map.of(original, 3L));

        var saved = save(cpu);
        var restored = restore(saved);

        assertFalse(saved.getCompound("job").contains("ae2byproductremover:processingPlan"));
        assertFalse(((ProcessingJob) currentJob(restored)).ae2byproductremover$hasProcessingPlan());
        assertEquals(Map.of(original, 3L), pendingCrafts(restored));
        assertInstanceOf(AEProcessingPattern.class, pendingCrafts(restored).keySet().iterator().next());
    }

    private static AEProcessingPattern pattern() {
        var encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)),
                List.of(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 2),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 3),
                        new GenericStack(AEItemKey.of(Items.REDSTONE), 4)));
        return new AEProcessingPattern(AEItemKey.of(encoded));
    }

    private static CraftingCpuLogic cpuWith(Map<IPatternDetails, Long> crafts) throws Exception {
        var cpu = emptyCpu();
        var plan = new CraftingPlan(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1), 1,
                false, false, new KeyCounter(), new KeyCounter(), new KeyCounter(), crafts);
        var listenerType = Class.forName("appeng.crafting.execution.ExecutingCraftingJob$CraftingDifferenceListener");
        var listener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] { listenerType },
                (proxy, method, args) -> null);
        var constructor = ExecutingCraftingJob.class.getDeclaredConstructor(
                ICraftingPlan.class, listenerType, CraftingLink.class, Integer.class);
        constructor.setAccessible(true);
        var cluster = (CraftingCPUCluster) field(CraftingCpuLogic.class, cpu, "cluster");
        var link = new CraftingLink(CraftingCpuHelper.generateLinkData(UUID.randomUUID(), true, false), cluster);
        var job = constructor.newInstance(plan, listener, link, null);
        setField(CraftingCpuLogic.class, cpu, "job", job);
        return cpu;
    }

    private static CraftingCpuLogic emptyCpu() throws Exception {
        var cluster = new CraftingCPUCluster(BlockPos.ZERO, BlockPos.ZERO);
        var core = new CraftingBlockEntity(AEBlockEntities.CRAFTING_UNIT.get(), BlockPos.ZERO,
                AEBlocks.CRAFTING_UNIT.block().defaultBlockState());
        setField(CraftingCPUCluster.class, cluster, "machineSrc", new MachineSource(core));
        return cluster.craftingLogic;
    }

    @SuppressWarnings("unchecked")
    private static CraftingMonitorBlockEntity attachMonitor(CraftingCpuLogic cpu) throws Exception {
        var cluster = (CraftingCPUCluster) field(CraftingCpuLogic.class, cpu, "cluster");
        var monitor = new CraftingMonitorBlockEntity(AEBlockEntities.CRAFTING_MONITOR.get(), BlockPos.ZERO,
                AEBlocks.CRAFTING_MONITOR.block().defaultBlockState());
        ((List<CraftingMonitorBlockEntity>) field(CraftingCPUCluster.class, cluster, "status")).add(monitor);
        return monitor;
    }

    private static CompoundTag save(CraftingCpuLogic cpu) {
        var saved = new CompoundTag();
        cpu.writeToNBT(saved, registries());
        return saved;
    }

    private static CraftingCpuLogic restore(CompoundTag saved) throws Exception {
        var cpu = emptyCpu();
        cpu.readFromNBT(saved, registries());
        return cpu;
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static Map<IPatternDetails, Long> pendingCrafts(CraftingCpuLogic cpu) throws Exception {
        var counts = new LinkedHashMap<IPatternDetails, Long>();
        var job = (ExecutingCraftingJobAccessor) currentJob(cpu);
        job.ae2byproductremover$getTasks().forEach((details, progress) -> counts.put(details,
                ((TaskProgressAccessor) progress).ae2byproductremover$getRemainingCrafts()));
        return counts;
    }

    private static ExecutingCraftingJob currentJob(CraftingCpuLogic cpu) throws Exception {
        return (ExecutingCraftingJob) field(CraftingCpuLogic.class, cpu, "job");
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
}
