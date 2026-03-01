package me.antigravity.jail;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetJailCommand implements CommandExecutor {

    private final JailManager jailManager;

    public SetJailCommand(JailManager jailManager) {
        this.jailManager = jailManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        double radius = 10.0;
        if (args.length > 0) {
            try {
                radius = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c正しい半径を指定してください。デフォルトの10を使用します。");
            }
        }

        Location loc = player.getLocation();
        Main plugin = Main.getInstance();
        plugin.getConfig().set("jail.world", loc.getWorld().getName());
        plugin.getConfig().set("jail.x", loc.getX());
        plugin.getConfig().set("jail.y", loc.getY());
        plugin.getConfig().set("jail.z", loc.getZ());
        plugin.getConfig().set("jail.radius", radius);
        plugin.saveConfig();

        player.sendMessage("§a牢獄の場所と範囲 (" + radius + "ブロック) を設定しました。");
        return true;
    }
}
