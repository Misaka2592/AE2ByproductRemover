/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;
import java.util.List;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exercises the encoder that Data Energistics uses to replace AE2's initial encoding result. */
class DataEnergisticsEncodingTest {
    private static Class<?> helper;
    private static Method encode;

    @BeforeAll
    static void findOptionalEncoder() throws Exception {
        String helperName = "com.fish_dan_.data_energistics.menu.patternencoding.source.PatternEncodingSourceHelper";
        helper = Class.forName(helperName);
        encode = helper.getMethod("encodeProcessingPattern", ConfigInventory.class, ConfigInventory.class);
    }

    @Test
    void acceptsTheFirstOutputInAnySlot() throws Exception {
        var fixture = new Fixture();
        var output = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 3);
        fixture.outputs.setStack(8, output);

        assertEquals(List.of(output), decode(fixture.encode()).getSparseOutputs());
        assertNull(fixture.outputs.getStack(0));
        assertSame(output, fixture.outputs.getStack(8), "Encoding must not modify the draft inventory");
    }

    @Test
    void removesInteriorAndTrailingHolesWhileKeepingItemAndFluidOrder() throws Exception {
        var fixture = new Fixture();
        var iron = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2);
        var water = new GenericStack(AEFluidKey.of(Fluids.WATER), 1_000);
        var gold = new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 5);
        fixture.outputs.setStack(0, iron);
        fixture.outputs.setStack(3, water);
        fixture.outputs.setStack(6, gold);

        assertEquals(List.of(iron, water, gold), decode(fixture.encode()).getSparseOutputs());
    }

    @Test
    void preservesDuplicateSlotsAndTheirAmounts() throws Exception {
        var fixture = new Fixture();
        var first = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2);
        var second = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 5);
        fixture.outputs.setStack(0, first);
        fixture.outputs.setStack(4, second);

        assertEquals(List.of(first, second), decode(fixture.encode()).getSparseOutputs());
    }

    @Test
    void retainsDataEnergisticsCustomKeyUnwrappingAndAmountMultiplication() throws Exception {
        var fixture = new Fixture();
        var dataKey = (AEKey) Class.forName("com.fish_dan_.data_energistics.ae2.key.DataKey")
                .getMethod("of").invoke(null);
        var wrappedKey = AEItemKey.of(GenericStack.wrapInItemStack(dataKey, 7));
        fixture.outputs.setStack(2, new GenericStack(wrappedKey, 3));

        assertEquals(List.of(new GenericStack(dataKey, 21)), decode(fixture.encode()).getSparseOutputs());
    }

    @Test
    void leavesSparseInputNormalizationUnchanged() throws Exception {
        var fixture = new Fixture();
        var normalize = helper.getDeclaredMethod("normalizeProcessingPatternInventory",
                ConfigInventory.class, String.class);
        normalize.setAccessible(true);
        var normalized = (List<?>) normalize.invoke(null, fixture.inputs, "input");

        assertEquals(fixture.inputs.size(), normalized.size());
        assertNull(normalized.get(0));
        assertSame(fixture.inputs.getStack(1), normalized.get(1));
        assertNull(normalized.get(2));
    }

    @Test
    void rejectsAllEmptyOutputsAndMissingInputs() throws Exception {
        var fixture = new Fixture();
        assertNull(fixture.encode());

        fixture.outputs.setStack(2, new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1));
        fixture.inputs.clear();
        assertNull(fixture.encode());
    }

    private static AEProcessingPattern decode(ItemStack encoded) {
        assertNotNull(encoded, "The final Data Energistics encoding path must accept non-first outputs");
        return new AEProcessingPattern(AEItemKey.of(encoded));
    }

    private static final class Fixture {
        private final ConfigInventory inputs = ConfigInventory.configStacks(81).allowOverstacking(true).build();
        private final ConfigInventory outputs = ConfigInventory.configStacks(27).allowOverstacking(true).build();

        private Fixture() {
            inputs.setStack(1, new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1));
        }

        private ItemStack encode() throws Exception {
            return (ItemStack) DataEnergisticsEncodingTest.encode.invoke(null, inputs, outputs);
        }
    }
}
