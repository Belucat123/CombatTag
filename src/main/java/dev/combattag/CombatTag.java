package dev.combattag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CombatTag extends JavaPlugin {

    private CombatManager combatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        combatManager = new CombatManager(this);
        getServer().getPluginManager().registerEvents(
            new CombatListener(this, combatManager), this
        );
        getLogger().info("CombatTag enabled — combat duration: "
            + getConfig().getInt("combat-duration", 20) + "s");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.cleanUpAll();
        }
        getLogger().info("CombatTag disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("combattag")) return false;
        if (!sender.hasPermission("combattag.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(Component.text("CombatTag config reloaded.", NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /combattag reload", NamedTextColor.YELLOW));
        return true;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
