package io.github.bannovsasa277.woodslowdown.breaking;

import io.github.bannovsasa277.woodslowdown.WoodBreakSlowdownPlugin;
import io.github.bannovsasa277.woodslowdown.config.PluginConfig;
import io.github.bannovsasa277.woodslowdown.config.WoodBlockClassifier;
import io.github.bannovsasa277.woodslowdown.util.BlockKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public final class BreakController implements Listener {

    private final WoodBreakSlowdownPlugin plugin;
    private final PluginConfig config;
    private final WoodBlockClassifier classifier;
    private final VanillaLikeSpeedCalculator speedCalculator;
    private final Map<UUID, BreakSession> activeSessions = new HashMap<>();
    private final Set<String> bypassBreakEvent = new HashSet<>();
    private final BukkitTask tickTask;

    public BreakController(WoodBreakSlowdownPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.settings();
        this.classifier = new WoodBlockClassifier(config);
        this.speedCalculator = new VanillaLikeSpeedCalculator(config);
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        tickTask.cancel();
        for (BreakSession session : activeSessions.values()) {
            Player player = Bukkit.getPlayer(session.playerId());
            if (player != null) {
                clearCrackAnimation(player, getBlock(session.blockKey()));
            }
        }
        activeSessions.clear();
        bypassBreakEvent.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!shouldHandle(player, block)) {
            return;
        }

        event.setCancelled(true);

        BreakSession existing = activeSessions.get(player.getUniqueId());
        if (existing != null && existing.blockKey().equals(BlockKey.of(block))) {
            return;
        }

        if (existing != null) {
            cancelSession(player.getUniqueId());
        }

        activeSessions.put(player.getUniqueId(), new BreakSession(player.getUniqueId(), block));
        debug("Started custom break for " + player.getName() + " at " + BlockKey.of(block));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        cancelSession(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String bypassKey = bypassKey(player.getUniqueId(), BlockKey.of(event.getBlock()));
        if (bypassBreakEvent.remove(bypassKey)) {
            return;
        }

        cancelSession(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSession(event.getPlayer().getUniqueId());
    }

    private void tick() {
        if (activeSessions.isEmpty()) {
            return;
        }

        Set<UUID> toCancel = new HashSet<>();

        for (Map.Entry<UUID, BreakSession> entry : activeSessions.entrySet()) {
            UUID playerId = entry.getKey();
            BreakSession session = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);
            Block block = getBlock(session.blockKey());

            if (player == null || block == null || !shouldContinue(player, block)) {
                toCancel.add(playerId);
                continue;
            }

            float perTick = speedCalculator.getScaledProgressPerTick(player, block);
            if (perTick <= 0.0F) {
                toCancel.add(playerId);
                continue;
            }

            session.addProgress(perTick);
            showCrackAnimation(player, block, session.progress());

            if (session.progress() >= 1.0F) {
                forceBreak(player, block);
                toCancel.add(playerId);
            }
        }

        for (UUID uuid : toCancel) {
            cancelSession(uuid);
        }
    }

    private boolean shouldHandle(Player player, Block block) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        if (config.onlyHandleSurvival() && player.getGameMode() != GameMode.SURVIVAL) {
            return false;
        }

        return classifier.isAffected(block.getType());
    }

    private boolean shouldContinue(Player player, Block block) {
        if (!shouldHandle(player, block)) {
            return false;
        }

        Block target = player.getTargetBlockExact((int) Math.ceil(config.maxBreakDistance()), FluidCollisionMode.NEVER);
        if (target == null) {
            return false;
        }

        if (!BlockKey.of(target).equals(BlockKey.of(block))) {
            return false;
        }

        Location eye = player.getEyeLocation();
        Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        return eye.distanceSquared(center) <= config.maxBreakDistance() * config.maxBreakDistance();
    }

    private void forceBreak(Player player, Block block) {
        BlockKey key = BlockKey.of(block);
        bypassBreakEvent.add(bypassKey(player.getUniqueId(), key));
        clearCrackAnimation(player, block);
        player.breakBlock(block);
        debug("Forced block break for " + player.getName() + " at " + key);
    }

    private void cancelSession(UUID playerId) {
        BreakSession session = activeSessions.remove(playerId);
        if (session == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            clearCrackAnimation(player, getBlock(session.blockKey()));
        }
    }

    private void showCrackAnimation(Player player, Block block, float progress) {
        if (block == null) {
            return;
        }

        Location location = block.getLocation();
        int sourceId = player.getEntityId();
        for (Player viewer : block.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(location) <= 32 * 32) {
                viewer.sendBlockDamage(location, progress, sourceId);
            }
        }
    }

    private void clearCrackAnimation(Player player, Block block) {
        if (block == null) {
            return;
        }

        Location location = block.getLocation();
        int sourceId = player.getEntityId();
        for (Player viewer : block.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(location) <= 32 * 32) {
                viewer.sendBlockDamage(location, 0.0F, sourceId);
            }
        }
    }

    private Block getBlock(BlockKey key) {
        World world = Bukkit.getWorld(key.worldName());
        if (world == null) {
            return null;
        }
        return world.getBlockAt(key.x(), key.y(), key.z());
    }

    private String bypassKey(UUID uuid, BlockKey key) {
        return uuid + ":" + key.worldName() + ":" + key.x() + ":" + key.y() + ":" + key.z();
    }

    private void debug(String message) {
        if (config.debug()) {
            plugin.getLogger().info(message);
        }
    }
}
