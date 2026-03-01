package me.antigravity.jail;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private JailManager jailManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        jailManager = new JailManager(this);

        getCommand("jail").setExecutor(new JailCommand(jailManager));
        getCommand("unjail").setExecutor(new UnjailCommand(jailManager));
        getCommand("setjail").setExecutor(new SetJailCommand(jailManager));
        getCommand("setrelease").setExecutor(new SetReleaseCommand());
        getCommand("sethologram").setExecutor(new SetHologramCommand());

        getServer().getPluginManager().registerEvents(new MoveListener(jailManager), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, jailManager), this);

        getLogger().info("Jail plugin enabled!");
    }

    public static Main getInstance() {
        return instance;
    }
}
