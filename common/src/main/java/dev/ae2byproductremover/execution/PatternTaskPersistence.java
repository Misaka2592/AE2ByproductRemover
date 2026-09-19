package dev.ae2byproductremover.execution;

import java.util.Map;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;

import dev.ae2byproductremover.pattern.OutputPattern;

/** Stores a plan's output view on its CPU task, leaving encoded pattern items untouched. */
public final class PatternTaskPersistence {
    private static final String VIEW_TAG = "ae2byproductremover:outputs";

    private PatternTaskPersistence() {
    }

    public static void writeViews(CompoundTag jobTag, Map<IPatternDetails, ?> tasks,
            Function<GenericStack, CompoundTag> writeStack) {
        ListTag savedTasks = jobTag.getList("tasks", Tag.TAG_COMPOUND);
        int index = 0;
        // AE2 wrote this same map immediately before our return injection, on the server thread.
        // Keep one view per list entry: separate targets can share the same pattern definition.
        for (var details : tasks.keySet()) {
            CompoundTag savedTask = savedTasks.getCompound(index++);
            if (details instanceof OutputPattern pattern) {
                CompoundTag view = new CompoundTag();
                view.put("target", writeStack.apply(new GenericStack(pattern.target(), 1)));
                ListTag outputs = new ListTag();
                for (GenericStack output : pattern.getOutputs()) {
                    outputs.add(writeStack.apply(output));
                }
                view.put("outputs", outputs);
                savedTask.put(VIEW_TAG, view);
            }
        }
    }

    public static IPatternDetails readView(IPatternDetails original, CompoundTag savedTask,
            Function<CompoundTag, GenericStack> readStack) {
        if (original == null || !savedTask.contains(VIEW_TAG, Tag.TAG_COMPOUND)) {
            return original;
        }
        CompoundTag view = savedTask.getCompound(VIEW_TAG);
        GenericStack target = readStack.apply(view.getCompound("target"));
        ListTag outputs = view.getList("outputs", Tag.TAG_COMPOUND);
        GenericStack[] restored = new GenericStack[outputs.size()];
        for (int i = 0; i < restored.length; i++) {
            restored[i] = readStack.apply(outputs.getCompound(i));
            if (restored[i] == null) {
                return null;
            }
        }
        if (target == null || restored.length == 0) {
            return null;
        }
        return new OutputPattern(original, target.what(), restored);
    }
}
