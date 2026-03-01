package me.antigravity.jail;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    private final JailManager jailManager;

    public MoveListener(JailManager jailManager) {
        this.jailManager = jailManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!jailManager.isJailed(event.getPlayer().getUniqueId()))
            return;

        if (jailManager.isOutside(event.getTo())) {
            event.getPlayer().sendMessage("§cあなたは投獄されています！範囲外に出ることはできません。");
            jailManager.teleportToJail(event.getPlayer());
        }
    }
}
