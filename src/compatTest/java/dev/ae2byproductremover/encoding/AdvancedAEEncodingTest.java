/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder;
import net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sun.reflect.ReflectionFactory;

/** Exercises AdvancedAE type and direction preservation through AE2's transformed encoding menu. */
class AdvancedAEEncodingTest {
    private static final AEKey IRON = AEItemKey.of(Items.IRON_INGOT);
    private static final AEKey COPPER = AEItemKey.of(Items.COPPER_INGOT);
    private static final AEKey GOLD = AEItemKey.of(Items.GOLD_INGOT);
    private static final AEKey DIAMOND = AEItemKey.of(Items.DIAMOND);
    private static final AEKey EMERALD = AEItemKey.of(Items.EMERALD);
    private static final GenericStack OUTPUT = new GenericStack(AEItemKey.of(Items.REDSTONE), 4);
    private static final List<GenericStack> ORIGINAL_INPUTS = List.of(
            new GenericStack(IRON, 2), new GenericStack(COPPER, 3), new GenericStack(GOLD, 1));
    private static final IPatternDetailsDecoder DECODER = new IPatternDetailsDecoder() {
        @Override public boolean isEncodedPattern(ItemStack stack) {
            return stack.is(AAEItems.ADV_PROCESSING_PATTERN.asItem());
        }

        @Override public IPatternDetails decodePattern(AEItemKey key, Level level) {
            return key != null && key.getItem() == AAEItems.ADV_PROCESSING_PATTERN.asItem()
                    ? new AdvProcessingPattern(key) : null;
        }
    };

    @BeforeAll
    static void allowWorldlessAdvancedPatternDecode() {
        PatternDetailsHelper.registerDecoder(DECODER);
    }

    @AfterAll
    static void restoreDecoder() throws Exception {
        var field = PatternDetailsHelper.class.getDeclaredField("DECODERS");
        field.setAccessible(true);
        ((List<?>) field.get(null)).remove(DECODER);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2 })
    void unchangedAndDoubledQuantitiesKeepAdvancedTypeAndDirections(int multiplier) throws Exception {
        var fixture = new Fixture(advancedPattern());
        for (int slot = 0; slot < ORIGINAL_INPUTS.size(); slot++) {
            var input = ORIGINAL_INPUTS.get(slot);
            fixture.inputs.setStack(slot, new GenericStack(input.what(), input.amount() * multiplier));
        }
        fixture.outputs.setStack(0, new GenericStack(OUTPUT.what(), OUTPUT.amount() * multiplier));

        var encoded = advanced(fixture.encode());
        assertEquals(Direction.NORTH, encoded.getDirectionMap().get(IRON));
        assertEquals(Direction.SOUTH, encoded.getDirectionMap().get(COPPER));
        assertNull(encoded.getDirectionMap().get(GOLD));
        assertEquals(2L * multiplier, encoded.getSparseInputs().getFirst().amount());
        assertEquals(List.of(new GenericStack(OUTPUT.what(), OUTPUT.amount() * multiplier)),
                encoded.getSparseOutputs());
    }

    @Test
    void inputDirectionsFollowResourceKeysAcrossAdditionsRemovalsAndReordering() throws Exception {
        var fixture = new Fixture(advancedPattern());
        fixture.inputs.clear();
        fixture.inputs.setStack(0, new GenericStack(GOLD, 7));
        fixture.inputs.setStack(1, new GenericStack(DIAMOND, 1));
        fixture.inputs.setStack(7, new GenericStack(COPPER, 2));

        var encoded = advanced(fixture.encode());
        var directions = encoded.getDirectionMap();
        assertFalse(directions.containsKey(IRON), "Removed inputs must lose their old directions");
        assertTrue(directions.containsKey(GOLD));
        assertNull(directions.get(GOLD), "Moving into the old north slot must not inherit its direction");
        assertTrue(directions.containsKey(DIAMOND));
        assertNull(directions.get(DIAMOND), "A new resource in the old south slot has no direction");
        assertEquals(Direction.SOUTH, directions.get(COPPER));
        assertEquals(new GenericStack(COPPER, 2), encoded.getSparseInputs().get(7));
    }

    @Test
    void replacingEveryOriginalInputStillProducesAnAdvancedPatternWithoutDirections() throws Exception {
        var fixture = new Fixture(advancedPattern());
        fixture.inputs.clear();
        fixture.inputs.setStack(0, new GenericStack(DIAMOND, 1));
        fixture.inputs.setStack(1, new GenericStack(EMERALD, 2));

        var directions = advanced(fixture.encode()).getDirectionMap();
        assertEquals(2, directions.size());
        assertTrue(directions.containsKey(DIAMOND));
        assertTrue(directions.containsKey(EMERALD));
        assertNull(directions.get(DIAMOND));
        assertNull(directions.get(EMERALD));
    }

    @Test
    void compactsChangedOutputsWithoutLosingTypeOrderDuplicateAmountsOrFluidKeys() throws Exception {
        var fixture = new Fixture(advancedPattern());
        fixture.outputs.clear();
        var water = new GenericStack(AEFluidKey.of(Fluids.WATER), 1_000);
        var duplicate = new GenericStack(OUTPUT.what(), 9);
        fixture.outputs.setStack(1, OUTPUT);
        fixture.outputs.setStack(4, water);
        fixture.outputs.setStack(7, duplicate);

        var encoded = advanced(fixture.encode());
        assertEquals(List.of(OUTPUT, water, duplicate), encoded.getSparseOutputs());
        assertEquals(Direction.NORTH, encoded.getDirectionMap().get(IRON));
        assertNull(fixture.outputs.getStack(0), "Encoding a result must not modify draft slots");
        assertSame(water, fixture.outputs.getStack(4));
    }

    @Test
    void anAdvancedPatternWithNoAssignedDirectionsRetainsItsType() throws Exception {
        var original = AdvPatternDetailsEncoder.encodeProcessingPattern(
                ORIGINAL_INPUTS, List.of(OUTPUT), new HashMap<>());
        var fixture = new Fixture(original);

        var directions = advanced(fixture.encode()).getDirectionMap();
        assertEquals(3, directions.size());
        assertTrue(directions.values().stream().allMatch(direction -> direction == null));
    }

    @Test
    void ordinaryBlankAndEmptyEncodedSlotsStillProduceOrdinaryProcessingPatterns() throws Exception {
        var ordinary = PatternDetailsHelper.encodeProcessingPattern(ORIGINAL_INPUTS, List.of(OUTPUT));
        for (var original : List.of(ordinary, AEItems.BLANK_PATTERN.stack(), ItemStack.EMPTY)) {
            var fixture = new Fixture(original);
            var encoded = fixture.encode();
            assertNotNull(encoded);
            assertSame(AEItems.PROCESSING_PATTERN.asItem(), encoded.getItem());
        }
    }

    @Test
    void missingInputsAndAllEmptyOutputsRemainInvalid() throws Exception {
        var fixture = new Fixture(advancedPattern());
        fixture.inputs.clear();
        assertNull(fixture.encode());

        fixture = new Fixture(advancedPattern());
        fixture.outputs.clear();
        assertNull(fixture.encode());
    }

    @Test
    void explicitlyChangingEncodingModeDoesNotForceAnAdvancedProcessingResult() throws Exception {
        var fixture = new Fixture(advancedPattern());
        fixture.menu.mode = EncodingMode.PROCESSING;
        assertNotNull(fixture.invoke("encodePattern"));

        fixture.menu.mode = EncodingMode.STONECUTTING;
        // No stonecutting recipe is selected. Valid processing slots must not be reused by this mode.
        assertNull(fixture.invoke("encodePattern"));
    }

    private static ItemStack advancedPattern() {
        var directions = new HashMap<AEKey, Direction>();
        directions.put(IRON, Direction.NORTH);
        directions.put(COPPER, Direction.SOUTH);
        return AdvPatternDetailsEncoder.encodeProcessingPattern(ORIGINAL_INPUTS, List.of(OUTPUT), directions);
    }

    private static AdvProcessingPattern advanced(ItemStack encoded) {
        assertNotNull(encoded);
        assertSame(AAEItems.ADV_PROCESSING_PATTERN.asItem(), encoded.getItem(),
                "Re-encoding must preserve the original AdvancedAE pattern type");
        return new AdvProcessingPattern(AEItemKey.of(encoded));
    }

    private static final class Fixture {
        private final ConfigInventory inputs = ConfigInventory.configStacks(81).allowOverstacking(true).build();
        private final ConfigInventory outputs = ConfigInventory.configStacks(27).allowOverstacking(true).build();
        private final PatternEncodingTermMenu menu;

        private Fixture(ItemStack original) throws Exception {
            menu = uninitialized(PatternEncodingTermMenu.class);
            set(PatternEncodingTermMenu.class, menu, "encodedInputsInv", inputs);
            set(PatternEncodingTermMenu.class, menu, "encodedOutputsInv", outputs);
            var patterns = new AppEngInternalInventory(1);
            patterns.setItemDirect(0, original);
            set(PatternEncodingTermMenu.class, menu, "encodedPatternSlot", new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, patterns, 0));
            set(AEBaseMenu.class, menu, "playerInventory", new Inventory(uninitialized(ServerPlayer.class)));
            for (int slot = 0; slot < ORIGINAL_INPUTS.size(); slot++) {
                inputs.setStack(slot, ORIGINAL_INPUTS.get(slot));
            }
            outputs.setStack(0, OUTPUT);
        }

        private ItemStack encode() throws Exception {
            return invoke("encodeProcessingPattern");
        }

        private ItemStack invoke(String methodName) throws Exception {
            var method = PatternEncodingTermMenu.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            try {
                return (ItemStack) method.invoke(menu);
            } catch (InvocationTargetException error) {
                throw new AssertionError("AE2 menu encoding failed", error.getCause());
            }
        }
    }

    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T uninitialized(Class<T> type) throws Exception {
        var constructor = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(
                type, Object.class.getDeclaredConstructor());
        constructor.setAccessible(true);
        return type.cast(constructor.newInstance());
    }
}
