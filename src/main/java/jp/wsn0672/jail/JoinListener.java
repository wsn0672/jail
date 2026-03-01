package me.antigravity.jail;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinListener implements Listener {

    private final JavaPlugin plugin;
    private final JailManager jailManager;

    public JoinListener(JavaPlugin plugin, JailManager jailManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check IP first
        jailManager.checkIP(player);

        if (jailManager.isJailed(player.getUniqueId())) {
            // Still jailed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    jailManager.teleportToJail(player);
                    player.sendMessage("§cあなたはまだ投獄されています。");
                }
            }, 20L);
        } else {
            // Not jailed, but check if they are in the jail area
            if (jailManager.isOutside(player.getLocation()) == false) {
                // They are in jail but the term is up (or they were never jailed)
                // To be safe, we only teleport if we have a release location set
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        jailManager.teleportToRelease(player);
                    }
                }, 20L);
            }
        }
    }
}
