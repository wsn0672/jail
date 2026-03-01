package me.antigravity.jail;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class UnjailCommand implements CommandExecutor {

    private final JailManager jailManager;

    public UnjailCommand(JailManager jailManager) {
        this.jailManager = jailManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c使用法: /unjail <プレイヤー>");
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!jailManager.isJailed(target.getUniqueId())) {
            sender.sendMessage("§cそのプレイヤーは投獄されていません。");
            return true;
        }

        jailManager.release(target.getUniqueId());
        sender.sendMessage("§a" + target.getName() + " を釈放しました。");
        return true;
    }
}
