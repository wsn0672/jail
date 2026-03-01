package me.antigravity.jail;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetReleaseCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Location loc = player.getLocation();
        Main plugin = Main.getInstance();
        plugin.getConfig().set("release.world", loc.getWorld().getName());
        plugin.getConfig().set("release.x", loc.getX());
        plugin.getConfig().set("release.y", loc.getY());
        plugin.getConfig().set("release.z", loc.getZ());
        plugin.saveConfig();

        player.sendMessage("§a釈放場所を設定しました。");
        return true;
    }
}
