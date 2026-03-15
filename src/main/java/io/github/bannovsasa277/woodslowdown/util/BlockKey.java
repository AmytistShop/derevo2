package io.github.bannovsasa277.woodslowdown.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record BlockKey(String worldName, int x, int y, int z) {

    public static BlockKey of(Block block) {
        Location location = block.getLocation();
        World world = location.getWorld();
        return new BlockKey(world == null ? "unknown" : world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
