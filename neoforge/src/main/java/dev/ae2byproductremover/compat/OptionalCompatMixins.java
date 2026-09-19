package dev.ae2byproductremover.compat;

import java.util.List;
import java.util.Set;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Selects addon mixins from discovered mod metadata without loading addon classes early. */
public final class OptionalCompatMixins implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String integration = mixinClassName.substring("dev.ae2byproductremover.mixin.compat.".length())
                .split("\\.", 2)[0];
        String modId = switch (integration) {
            case "advancedae" -> "advanced_ae";
            case "neoecoae" -> "neoecoae";
            case "thunderbolt" -> "thunderbolt";
            case "dataenergistics" -> "data_energistics";
            default -> throw new IllegalArgumentException("Unknown compatibility mixin: " + mixinClassName);
        };
        return FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(mod -> mod.getModId().equals(modId));
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
            IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
            IMixinInfo mixinInfo) { }
}
