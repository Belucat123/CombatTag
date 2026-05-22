package dev.combattag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CombatManager {

    private final CombatTag plugin;
    private final Map<UUID, Integer> combatTimers = new HashMap<>();
    private final Map<UUID, BukkitTask> tickTasks = new HashMap<>();
    private final Map<UUID, UUID> opponents = new HashMap<>();

    public CombatManager(CombatTag plugin) {
        this.plugin = plugin;
    }

    public void tag(Player attacker, Player defender) {
        if (attacker.hasPermission("combattag.bypass") || defender.hasPermission("combattag.bypass")) {
            return;
        }
        int duration = plugin.getConfig().getInt("combat-duration", 20);
        tagSingle(attacker, defender.getUniqueId(), duration);
        tagSingle(defender, attacker.getUniqueId(), duration);
    }

    private void tagSingle(Player player, UUID opponentId, int duration) {
        UUID uuid = player.getUniqueId();
        opponents.put(uuid, opponentId);
        combatTimers.put(uuid, duration);
        BukkitTask existing = tickTasks.remove(uuid);
        if (existing != null) existing.cancel();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(uuid), 20L, 20L);
        tickTasks.put(uuid, task);
        sendActionBar(player, duration);
    }

    private void tick(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            cleanUp(uuid);
            return;
        }
        int remaining = combatTimers.getOrDefault(uuid, 0) - 1;
        if (remaining <= 0) {
            cleanUp(uuid);
            player.sendActionBar(Component.text("✔ Combat ended", NamedTextColor.GREEN));
            return;
        }
        combatTimers.put(uuid, remaining);
        sendActionBar(player, remaining);
    }

    public void handleLogout(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isTagged(uuid)) return;
        player.setHealth(0.0);
        Bukkit.broadcast(
            Component.text(player.getName(), NamedTextColor.RED)
                .append(Component.text(" logged out during combat and died!", NamedTextColor.GRAY))
        );
        cleanUp(uuid);
    }

    public void cleanUp(UUID uuid) {
        combatTimers.remove(uuid);
        opponents.remove(uuid);
        BukkitTask task = tickTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public boolean isTagged(UUID uuid) {
        return combatTimers.containsKey(uuid);
    }

    public void cleanUpAll() {
        new HashSet<>(tickTasks.keySet()).forEach(this::cleanUp);
    }

    private void sendActionBar(Player player, int seconds) {
        NamedTextColor timerColor;
        if (seconds <= 5) {
            timerColor = NamedTextColor.RED;
        } else if (seconds <= 10) {
            timerColor = NamedTextColor.YELLOW;
        } else {
            timerColor = NamedTextColor.GOLD;
        }
        Component bar = Component.text("⚔ ", NamedTextColor.RED)
            .append(Component.text("Combat: ", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
            .append(Component.text(seconds + "s", timerColor).decoration(TextDecoration.BOLD, true));
        player.sendActionBar(bar);
    }
}
