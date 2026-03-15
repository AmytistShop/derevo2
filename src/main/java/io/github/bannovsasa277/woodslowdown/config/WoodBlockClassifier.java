package io.github.bannovsasa277.woodslowdown.config;

import java.util.Locale;
import org.bukkit.Material;

public final class WoodBlockClassifier {

    private final PluginConfig config;

    public WoodBlockClassifier(PluginConfig config) {
        this.config = config;
    }

    public boolean isAffected(Material material) {
        if (config.excludedMaterials().contains(material)) {
            return false;
        }
        if (config.extraMaterials().contains(material)) {
            return true;
        }

        String name = material.name().toLowerCase(Locale.ROOT);

        if (config.includeBambooBlock() && material == Material.BAMBOO_BLOCK) {
            return true;
        }

        if (config.includeMangroveRoots() && material == Material.MANGROVE_ROOTS) {
            return true;
        }

        if (config.includeLeaves() && name.endsWith("_leaves")) {
            return true;
        }

        if (config.includeNetherWood() && (name.endsWith("_stem") || name.endsWith("_hyphae") || name.endsWith("stripped_crimson_stem")
                || name.endsWith("stripped_warped_stem") || name.endsWith("stripped_crimson_hyphae") || name.endsWith("stripped_warped_hyphae"))) {
            return true;
        }

        if (config.includeStripped() && (name.startsWith("stripped_") && (name.endsWith("_log") || name.endsWith("_wood")))) {
            return true;
        }

        if (config.includeLogs() && name.endsWith("_log")) {
            return true;
        }

        if (config.includeWoods() && name.endsWith("_wood")) {
            return true;
        }

        return false;
    }
}
