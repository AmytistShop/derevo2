package io.github.bannovsasa277.woodslowdown;

import io.github.bannovsasa277.woodslowdown.breaking.BreakController;
import io.github.bannovsasa277.woodslowdown.config.PluginConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class WoodBreakSlowdownPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private BreakController breakController;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPlugin();
        getLogger().info("WoodBreakSlowdown enabled.");
    }

    @Override
    public void onDisable() {
        if (breakController != null) {
            breakController.shutdown();
        }
    }

    public PluginConfig settings() {
        return pluginConfig;
    }

    public void reloadPlugin() {
        reloadConfig();
        this.pluginConfig = PluginConfig.from(this);

        if (this.breakController != null) {
            this.breakController.shutdown();
        }

        this.breakController = new BreakController(this);
        getServer().getPluginManager().registerEvents(this.breakController, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("woodbreakslowdown")) {
            return false;
        }

        if (!sender.hasPermission("woodbreakslowdown.admin")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadPlugin();
            sender.sendMessage("WoodBreakSlowdown config reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /woodbreakslowdown reload");
        return true;
    }
}
