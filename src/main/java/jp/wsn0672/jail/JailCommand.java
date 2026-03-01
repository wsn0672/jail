package me.antigravity.jail;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JailCommand implements CommandExecutor {

    private final JailManager jailManager;

    public JailCommand(JailManager jailManager) {
        this.jailManager = jailManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /jail <プレイヤー> <時間> [理由]");
            return true;
        }

        String targetName = args[0];
        long seconds = jailManager.parseTime(args[1]);

        if (seconds <= 0) {
            sender.sendMessage("§c正しい時間を指定してください (例: 15m, 1h)。");
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
        } else {
            reasonBuilder.append("理由なし");
        }
        String reason = reasonBuilder.toString().trim();

        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.isOnline()) {
            String ip = target.getAddress().getAddress().getHostAddress();
            jailManager.jailPlayer(target.getUniqueId(), ip, seconds, reason);
            sender.sendMessage("§a" + target.getName() + " を " + args[1] + " 間投獄しました。理由: " + reason);
            target.sendMessage("§cあなたは " + args[1] + " 間投獄されました。理由: " + reason);
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget.hasPlayedBefore() || offlineTarget.isOnline()) {
                jailManager.jailPlayer(offlineTarget.getUniqueId(), null, seconds, reason);
                sender.sendMessage("§a" + offlineTarget.getName() + " (オフライン) を " + args[1] + " 間投獄しました。次回参加時に適用されます。");
            } else {
                sender.sendMessage("§cプレイヤーが見つかりません。");
            }
        }

        return true;
    }
}
