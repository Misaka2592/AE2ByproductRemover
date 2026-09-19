/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.crafting.CraftingLink;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.me.service.CraftingService;
import appeng.me.service.helpers.NetworkCraftingProviders;

import dev.ae2byproductremover.execution.ProcessingJob;
import dev.ae2byproductremover.mixin.execution.ExecutingCraftingJobAccessor;
import dev.ae2byproductremover.mixin.execution.TaskProgressAccessor;
import dev.ae2byproductremover.pattern.OutputPattern;

@GameTestHolder("ae2byproductremover")
@PrefixGameTestTemplate(false)
public final class ForgeCpuGameTest {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restoresSeparateOutputViewsAndExactCounts(GameTestHelper helper) throws Exception {
        var original = pattern();
        var b = OutputPattern.indexed(original, AEItemKey.of(Items.GOLD_INGOT));
        var c = OutputPattern.indexed(original, AEItemKey.of(Items.COPPER_INGOT));
        var counts = Map.<IPatternDetails, Long>of(b, 2L, c, 5L);
        var cpu = cpuWith(helper, counts);
        var saved = new CompoundTag();
        cpu.writeToNBT(saved);
        var restored = emptyCpu(helper);

        restored.readFromNBT(saved);

        check(counts.equals(pending(restored)), "NBT must preserve both views and their separate craft counts");
        check(((ProcessingJob) job(restored)).ae2byproductremover$hasProcessingPlan(), "Processing flag must persist");
        check(restored.getPendingOutputs(AEItemKey.of(Items.GOLD_INGOT)) == 4, "Expected two batches of 2 B");
        check(restored.getPendingOutputs(AEItemKey.of(Items.COPPER_INGOT)) == 15, "Expected five batches of 3 C");
        check(restored.getPendingOutputs(AEItemKey.of(Items.REDSTONE)) == 0, "Unused output must stay hidden");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void stillDispatchesTheSecondCraftAfterRequestedItemsArrive(GameTestHelper helper) throws Exception {
        var original = pattern();
        var view = OutputPattern.indexed(original, AEItemKey.of(Items.GOLD_INGOT));
        var cpu = cpuWith(helper, Map.of(view, 2L));
        var sent = new AtomicInteger();
        var energy = (IEnergyService) Proxy.newProxyInstance(IEnergyService.class.getClassLoader(),
                new Class<?>[] { IEnergyService.class }, (proxy, method, args) -> {
                    if (method.getName().equals("extractAEPower")) return args[0];
                    throw new AssertionError("Unexpected energy method: " + method);
                });
        var storage = (IStorageService) Proxy.newProxyInstance(IStorageService.class.getClassLoader(),
                new Class<?>[] { IStorageService.class }, (proxy, method, args) -> {
                    if (method.getName().equals("addGlobalStorageProvider")) return null;
                    throw new AssertionError("Unexpected storage method: " + method);
                });
        var service = new CraftingService(null, storage, energy);
        var provider = new ICraftingProvider() {
            @Override public List<IPatternDetails> getAvailablePatterns() { return List.of(original); }
            @Override public boolean isBusy() { return false; }
            @Override public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
                check(details == original, "Provider must receive the original pattern");
                check(inputs[0].get(AEItemKey.of(Items.IRON_INGOT)) == 1, "Each operation must consume 1 A");
                sent.incrementAndGet();
                return true;
            }
        };
        var node = (IGridNode) Proxy.newProxyInstance(IGridNode.class.getClassLoader(),
                new Class<?>[] { IGridNode.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getService" -> args[0] == ICraftingProvider.class ? provider : null;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
        ((NetworkCraftingProviders) field(CraftingService.class, service, "craftingProviders")).addProvider(node);
        cpu.getInventory().insert(AEItemKey.of(Items.IRON_INGOT), 2, Actionable.MODULATE);

        check(cpu.executeCrafting(1, service, energy, helper.getLevel()) == 1, "First operation must dispatch");
        check(cpu.getWaitingFor(AEItemKey.of(Items.COPPER_INGOT)) == 0, "Unused C must not be awaited");
        cpu.insert(AEItemKey.of(Items.GOLD_INGOT), 1, Actionable.MODULATE);
        check(cpu.hasJob(), "Early arrival must not remove the remaining operation");
        check(cpu.executeCrafting(1, service, energy, helper.getLevel()) == 1, "Second operation must dispatch");

        check(sent.get() == 2, "Exactly two confirmed operations must execute");
        check(cpu.getStored(AEItemKey.of(Items.IRON_INGOT)) == 0, "Both units of A must be consumed");
        check(!cpu.hasJob(), "Unused physical outputs must not block completion");
        helper.succeed();
    }

    private static AEProcessingPattern pattern() {
        return new AEProcessingPattern(AEItemKey.of(PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 2),
                        new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 3),
                        new GenericStack(AEItemKey.of(Items.REDSTONE), 4) })));
    }

    private static CraftingCpuLogic cpuWith(GameTestHelper helper, Map<IPatternDetails, Long> counts) throws Exception {
        var cpu = emptyCpu(helper);
        var plan = new CraftingPlan(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1), 1,
                false, false, new KeyCounter(), new KeyCounter(), new KeyCounter(), counts);
        var listenerClass = Class.forName("appeng.crafting.execution.ExecutingCraftingJob$CraftingDifferenceListener");
        var listener = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class<?>[] { listenerClass },
                (proxy, method, args) -> null);
        var constructor = ExecutingCraftingJob.class.getDeclaredConstructor(
                ICraftingPlan.class, listenerClass, CraftingLink.class, Integer.class);
        constructor.setAccessible(true);
        var cluster = (CraftingCPUCluster) field(CraftingCpuLogic.class, cpu, "cluster");
        var link = new CraftingLink(CraftingCpuHelper.generateLinkData(UUID.randomUUID(), true, false), cluster);
        setField(CraftingCpuLogic.class, cpu, "job", constructor.newInstance(plan, listener, link, null));
        return cpu;
    }

    private static CraftingCpuLogic emptyCpu(GameTestHelper helper) throws Exception {
        var position = helper.absolutePos(BlockPos.ZERO);
        var cluster = new CraftingCPUCluster(position, position);
        var core = new CraftingBlockEntity(AEBlockEntities.CRAFTING_UNIT, position,
                AEBlocks.CRAFTING_UNIT.block().defaultBlockState());
        core.setLevel(helper.getLevel());
        setField(CraftingCPUCluster.class, cluster, "machineSrc", new MachineSource(core));
        return cluster.craftingLogic;
    }

    private static Map<IPatternDetails, Long> pending(CraftingCpuLogic cpu) throws Exception {
        var counts = new LinkedHashMap<IPatternDetails, Long>();
        ((ExecutingCraftingJobAccessor) job(cpu)).ae2byproductremover$getTasks().forEach((details, progress) ->
                counts.put(details, ((TaskProgressAccessor) progress).ae2byproductremover$getRemainingCrafts()));
        return counts;
    }

    private static ExecutingCraftingJob job(CraftingCpuLogic cpu) throws Exception {
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

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
