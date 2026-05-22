package dev.combattag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CombatListener implements Listener {

    private final CombatTag plugin;
    private final CombatManager manager;

    public CombatListener(CombatTag plugin, CombatManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player defender = getPlayerVictim(event.getEntity());
        if (defender == null) return;
        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null) return;
        if (attacker.equals(defender)) return;
        boolean tagProjectile = plugin.getConfig().getBoolean("tag-on-projectile", true);
        if (!tagProjectile && event.getDamager() instanceof Projectile) return;
        manager.tag(attacker, defender);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        manager.handleLogout(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        manager.cleanUp(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        if (!manager.isTagged(player.getUniqueId())) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) return;
        if (!player.isGliding()) return;
        event.setCancelled(true);
        player.sendMessage(
            Component.text("⚔ ", NamedTextColor.RED)
                .append(Component.text("You cannot use fireworks while in combat!", NamedTextColor.YELLOW))
        );
    }

    private Player getPlayerVictim(Entity entity) {
        return entity instanceof Player p ? p : null;
    }

    private Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) return p;
        }
        return null;
    }
}
