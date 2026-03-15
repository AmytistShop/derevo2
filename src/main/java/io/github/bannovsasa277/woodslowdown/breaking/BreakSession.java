package io.github.bannovsasa277.woodslowdown.breaking;

import io.github.bannovsasa277.woodslowdown.util.BlockKey;
import java.util.UUID;
import org.bukkit.block.Block;

public final class BreakSession {

    private final UUID playerId;
    private final BlockKey blockKey;
    private float progress;

    public BreakSession(UUID playerId, Block block) {
        this.playerId = playerId;
        this.blockKey = BlockKey.of(block);
    }

    public UUID playerId() {
        return playerId;
    }

    public BlockKey blockKey() {
        return blockKey;
    }

    public float progress() {
        return progress;
    }

    public void addProgress(float delta) {
        this.progress = Math.min(1.0F, this.progress + delta);
    }
}
