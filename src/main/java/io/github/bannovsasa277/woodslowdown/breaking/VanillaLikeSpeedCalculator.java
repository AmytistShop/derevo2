package io.github.bannovsasa277.woodslowdown.breaking;

import io.github.bannovsasa277.woodslowdown.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class VanillaLikeSpeedCalculator {

    private static final float DEFAULT_WOOD_HARDNESS = 2.0F;

    private final PluginConfig config;

    public VanillaLikeSpeedCalculator(PluginConfig config) {
        this.config = config;
    }

    public float getScaledProgressPerTick(Player player, Block block) {
        float vanilla = getVanillaManagedWoodProgress(player, block);
        return (float) (vanilla * config.breakSpeedMultiplier());
    }

    private float getVanillaManagedWoodProgress(Player player, Block block) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        float destroySpeed = getDestroySpeed(tool);

        if (destroySpeed > 1.0F) {
            int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (efficiency > 0) {
                destroySpeed += (efficiency * efficiency) + 1.0F;
            }
        }

        destroySpeed *= getHasteMultiplier(player);
        destroySpeed *= getMiningFatigueMultiplier(player);

        if (isUnderwaterWithoutAquaAffinity(player)) {
            destroySpeed *= 0.2F;
        }

        if (!player.isOnGround()) {
            destroySpeed *= 0.2F;
        }

        float hardness = getHardness(block.getType());
        return destroySpeed / hardness / 30.0F;
    }

    private float getDestroySpeed(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return 1.0F;
        }

        Material material = stack.getType();
        String name = material.name();
        if (!name.endsWith("_AXE")) {
            return 1.0F;
        }

        if (name.startsWith("WOODEN_") || name.startsWith("GOLDEN_")) {
            return 2.0F;
        }
        if (name.startsWith("STONE_")) {
            return 4.0F;
        }
        if (name.startsWith("IRON_")) {
            return 6.0F;
        }
        if (name.startsWith("DIAMOND_")) {
            return 8.0F;
        }
        if (name.startsWith("NETHERITE_")) {
            return 9.0F;
        }
        if (name.startsWith("COPPER_")) {
            return 5.0F;
        }

        return 1.0F;
    }

    private float getHasteMultiplier(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.HASTE);
        if (effect == null) {
            return 1.0F;
        }
        return 1.0F + ((effect.getAmplifier() + 1) * 0.2F);
    }

    private float getMiningFatigueMultiplier(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (effect == null) {
            return 1.0F;
        }

        int level = effect.getAmplifier();
        return switch (level) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 0.00081F;
        };
    }

    private boolean isUnderwaterWithoutAquaAffinity(Player player) {
        boolean underwater = player.getEyeLocation().getBlock().isLiquid();
        if (!underwater) {
            return false;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) {
            return true;
        }
        return helmet.getEnchantmentLevel(Enchantment.AQUA_AFFINITY) <= 0;
    }

    private float getHardness(Material material) {
        String name = material.name();
        if (material == Material.BAMBOO_BLOCK) {
            return 2.0F;
        }
        if (material == Material.MANGROVE_ROOTS) {
            return 0.7F;
        }
        if (name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphae")) {
            return DEFAULT_WOOD_HARDNESS;
        }
        if (name.endsWith("_leaves")) {
            return 0.2F;
        }
        return DEFAULT_WOOD_HARDNESS;
    }
}
