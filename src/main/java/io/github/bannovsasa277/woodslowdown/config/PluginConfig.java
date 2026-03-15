package io.github.bannovsasa277.woodslowdown.config;

import io.github.bannovsasa277.woodslowdown.WoodBreakSlowdownPlugin;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public record PluginConfig(
        double breakSpeedMultiplier,
        double maxBreakDistance,
        boolean onlyHandleSurvival,
        boolean debug,
        boolean includeLogs,
        boolean includeWoods,
        boolean includeStripped,
        boolean includeNetherWood,
        boolean includeBambooBlock,
        boolean includeMangroveRoots,
        boolean includeLeaves,
        Set<Material> extraMaterials,
        Set<Material> excludedMaterials
) {

    public static PluginConfig from(WoodBreakSlowdownPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        return new PluginConfig(
                cfg.getDouble("break-speed-multiplier", 0.4D),
                cfg.getDouble("max-break-distance", 6.0D),
                cfg.getBoolean("only-handle-survival", true),
                cfg.getBoolean("debug", false),
                cfg.getBoolean("affected-blocks.logs", true),
                cfg.getBoolean("affected-blocks.woods", true),
                cfg.getBoolean("affected-blocks.stripped-logs-and-woods", true),
                cfg.getBoolean("affected-blocks.nether-stems-and-hyphae", true),
                cfg.getBoolean("affected-blocks.bamboo-block", true),
                cfg.getBoolean("affected-blocks.mangrove-roots", false),
                cfg.getBoolean("affected-blocks.leaves", false),
                parseMaterials(cfg.getStringList("extra-materials")),
                parseMaterials(cfg.getStringList("excluded-materials"))
        );
    }

    private static Set<Material> parseMaterials(List<String> raw) {
        Set<Material> result = new HashSet<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Material material = Material.matchMaterial(entry.trim().toUpperCase(Locale.ROOT));
            if (material != null) {
                result.add(material);
            }
        }
        return result;
    }
}
