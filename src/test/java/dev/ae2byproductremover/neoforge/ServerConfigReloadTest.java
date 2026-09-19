/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (c) 2026 Misaka2592 and contributors.
 */
package dev.ae2byproductremover.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.electronwill.nightconfig.core.file.FileWatcher;
import dev.ae2byproductremover.PlanningMode;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the registered mod's real FML loading events and asynchronous file watcher. */
class ServerConfigReloadTest {
    @Test
    void savingTheServerConfigReloadsBothDirectionsWithoutRestarting(@TempDir Path directory) throws Exception {
        assertReloadsThroughFileWatcher(directory, false);
    }

    @Test
    void atomicallyReplacingTheServerConfigReloadsBothDirections(@TempDir Path directory) throws Exception {
        assertReloadsThroughFileWatcher(directory, true);
    }

    private static void assertReloadsThroughFileWatcher(Path directory, boolean atomicReplace) throws Exception {
        assertFalse(FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.DISABLE_CONFIG_WATCHER),
                "This integration test requires FML's normally enabled file watcher");
        var config = ModConfigs.getModConfigs(AE2ByproductRemover.MOD_ID).stream()
                .filter(candidate -> candidate.getType() == ModConfig.Type.SERVER
                        && candidate.getSpec() == ServerConfig.SPEC)
                .findFirst().orElseThrow();
        var previousConfig = config.getLoadedConfig();
        boolean previousMode = PlanningMode.useByproducts();
        ModConfigSpec.BooleanValue value = ServerConfig.SPEC.getValues().get("useByproducts");
        Path file = directory.resolve(config.getFileName());
        Files.writeString(file, "useByproducts = false\n");

        // FML exposes only all-config loading publicly. Open just our existing registration against
        // a temporary directory so no other mod's config, or actual server file, is loaded or edited.
        var open = ConfigTracker.class.getDeclaredMethod("openConfig", ModConfig.class, Path.class, Path.class);
        open.setAccessible(true);
        var unload = ConfigTracker.class.getDeclaredMethod("unloadConfig", ModConfig.class);
        unload.setAccessible(true);
        try {
            PlanningMode.loadWorld(true);
            open.invoke(null, config, directory, null);
            assertFalse(PlanningMode.useByproducts(), "The real Loading event must apply the file's false value");
            assertFalse(value.getAsBoolean(), "Prime the configured value's cache before changing the file");

            // FML's addWatch returns before NightConfig's watcher thread registers its callback.
            // Queue another ADD and wait for it: this preserves FML's handler and ensures it is ready.
            FileWatcher.defaultInstance().addWatchFuture(file, () -> {}).get(5, TimeUnit.SECONDS);

            saveValue(file, true, atomicReplace);
            awaitMode(true);
            assertTrue(value.getAsBoolean(), "Reloading must invalidate the cached false value");

            saveValue(file, false, atomicReplace);
            awaitMode(false);
            assertFalse(value.getAsBoolean(), "Reloading must invalidate the cached true value");
        } finally {
            try {
                if (config.getLoadedConfig() != previousConfig) {
                    unload.invoke(null, config);
                    FileWatcher.defaultInstance().removeWatchFuture(file).get(5, TimeUnit.SECONDS);
                    if (previousConfig != null) {
                        restoreLoadedConfig(config, previousConfig);
                    }
                }
            } finally {
                PlanningMode.loadWorld(previousMode);
            }
        }
    }

    private static void saveValue(Path file, boolean enabled, boolean atomicReplace) throws Exception {
        // Preserve the comments added by FML during loading, just as editing this value would.
        // This avoids unrelated correction writes without changing normal truncating-save behavior.
        String content = Files.readString(file).replace("useByproducts = " + !enabled,
                "useByproducts = " + enabled);
        if (atomicReplace) {
            Path replacement = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(replacement, content);
            Files.move(replacement, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.writeString(file, content);
        }
    }

    private static void awaitMode(boolean expected) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (PlanningMode.useByproducts() != expected && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(expected, PlanningMode.useByproducts(),
                "Saving the file must reach the registered Reloading listener through FML's file watcher");
    }

    private static void restoreLoadedConfig(ModConfig config, IConfigSpec.ILoadedConfig previous) throws Exception {
        var setConfig = ModConfig.class.getDeclaredMethod("setConfig", previous.getClass(), Function.class);
        setConfig.setAccessible(true);
        Function<ModConfig, ModConfigEvent> loadingEvent = ModConfigEvent.Loading::new;
        setConfig.invoke(config, previous, loadingEvent);
    }
}
