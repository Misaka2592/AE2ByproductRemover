package dev.ae2byproductremover.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;
import sun.reflect.ReflectionFactory;

/** Exercises AE2's actual menu method after the encoding mixin has been applied. */
class MenuEncodingTest {
    @Test
    void acceptsTheFirstOutputInAnySlot() throws Exception {
        var fixture = new Fixture();
        var output = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 3);
        fixture.outputs.setStack(8, output);

        assertEquals(List.of(output), decode(fixture.encode()).getSparseOutputs());
        assertNull(fixture.outputs.getStack(0));
        assertSame(output, fixture.outputs.getStack(8), "Encoding a preview must not edit the menu slots");
    }

    @Test
    void encodedPatternRemovesEveryHoleWithoutReorderingItemsOrFluids() throws Exception {
        var fixture = new Fixture();
        var iron = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2);
        var water = new GenericStack(AEFluidKey.of(Fluids.WATER), 1_000);
        var gold = new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 5);
        fixture.outputs.setStack(1, iron);
        fixture.outputs.setStack(3, water);
        fixture.outputs.setStack(6, gold);

        assertEquals(List.of(iron, water, gold), decode(fixture.encode()).getSparseOutputs());
    }

    @Test
    void encodedPatternPreservesDuplicateSlotsAndEachAmount() throws Exception {
        var fixture = new Fixture();
        var first = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 2);
        var second = new GenericStack(AEItemKey.of(Items.IRON_INGOT), 5);
        fixture.outputs.setStack(1, first);
        fixture.outputs.setStack(4, second);

        assertEquals(List.of(first, second), decode(fixture.encode()).getSparseOutputs());
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
        assertNotNull(encoded, "A non-first output must be encodable after the mixin is applied");
        return new AEProcessingPattern(AEItemKey.of(encoded));
    }

    private static final class Fixture {
        private final ConfigInventory inputs = ConfigInventory.configStacks(81).allowOverstacking(true).build();
        private final ConfigInventory outputs = ConfigInventory.configStacks(27).allowOverstacking(true).build();
        private final PatternEncodingTermMenu menu;

        private Fixture() throws Exception {
            // Only player/world setup is bypassed. The instance, inventories, injected method, encoding
            // API and decoded pattern are real AE2 classes loaded by NeoForge's transforming classloader.
            var constructor = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(
                    PatternEncodingTermMenu.class, Object.class.getDeclaredConstructor());
            constructor.setAccessible(true);
            menu = (PatternEncodingTermMenu) constructor.newInstance();
            setInventory("encodedInputsInv", inputs);
            setInventory("encodedOutputsInv", outputs);
            inputs.setStack(1, new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1));
        }

        private void setInventory(String name, ConfigInventory inventory) throws Exception {
            var field = PatternEncodingTermMenu.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(menu, inventory);
        }

        private ItemStack encode() throws Exception {
            var method = PatternEncodingTermMenu.class.getDeclaredMethod("encodeProcessingPattern");
            method.setAccessible(true);
            try {
                return (ItemStack) method.invoke(menu);
            } catch (InvocationTargetException error) {
                throw new AssertionError("AE2 menu encoding failed", error.getCause());
            }
        }
    }
}
