package me.antigravity.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JailManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, JailRecord> jailed = new HashMap<>();
    private final Map<String, Long> jailedIPs = new HashMap<>();

    private UUID hologramUUID;

    public record JailRecord(long releaseTime, String reason) {
    }

    public JailManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
        startScheduler();
    }

    public void jailPlayer(UUID uuid, String ip, long seconds, String reason) {
        long releaseTime = System.currentTimeMillis() + (seconds * 1000);
        jailed.put(uuid, new JailRecord(releaseTime, reason));
        if (ip != null) {
            jailedIPs.put(ip, releaseTime);
        }
        saveData();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            teleportToJail(player);
        }
    }

    public boolean isJailed(UUID uuid) {
        if (!jailed.containsKey(uuid))
            return false;

        JailRecord record = jailed.get(uuid);
        if (System.currentTimeMillis() > record.releaseTime) {
            release(uuid);
            return false;
        }
        return true;
    }

    public void release(UUID uuid) {
        jailed.remove(uuid);
        saveData();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            teleportToRelease(player);
            player.sendMessage("§a刑期が終了、または管理者により釈放されました。");
        }
    }

    public void teleportToRelease(Player player) {
        Location loc = getReleaseLocation();
        if (loc != null) {
            player.teleport(loc);
        }
    }

    public void checkIP(Player player) {
        String ip = player.getAddress().getAddress().getHostAddress();
        if (jailedIPs.containsKey(ip)) {
            long release = jailedIPs.get(ip);
            if (System.currentTimeMillis() < release) {
                if (!jailed.containsKey(player.getUniqueId())) {
                    jailed.put(player.getUniqueId(), new JailRecord(release, "投獄回避行為"));
                    saveData();
                }
            } else {
                jailedIPs.remove(ip);
            }
        }
    }

    public void teleportToJail(Player player) {
        Location jail = getJailLocation();
        if (jail != null) {
            player.teleport(jail);
        }
    }

    public boolean isOutside(Location loc) {
        Location center = getJailLocation();
        if (center == null || !loc.getWorld().equals(center.getWorld()))
            return true;
        return loc.distance(center) > getRadius();
    }

    public Location getJailLocation() {
        return loadLocation("jail");
    }

    public Location getReleaseLocation() {
        return loadLocation("release");
    }

    public double getRadius() {
        return plugin.getConfig().getDouble("jail.radius", 10.0);
    }

    public long parseTime(String input) {
        Pattern pattern = Pattern.compile("(\\d+)([smhd])");
        Matcher matcher = pattern.matcher(input.toLowerCase());
        long totalSeconds = 0;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "s" -> totalSeconds += value;
                case "m" -> totalSeconds += value * 60;
                case "h" -> totalSeconds += value * 3600;
                case "d" -> totalSeconds += value * 86400;
            }
        }

        if (!found) {
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return totalSeconds;
    }

    private void loadData() {
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection jailedSection = dataConfig.getConfigurationSection("jailed");
        if (jailedSection != null) {
            for (String key : jailedSection.getKeys(false)) {
                long time = jailedSection.getLong(key + ".time");
                String reason = jailedSection.getString(key + ".reason", "No reason");
                jailed.put(UUID.fromString(key), new JailRecord(time, reason));
            }
        }
        ConfigurationSection ipsSection = dataConfig.getConfigurationSection("ips");
        if (ipsSection != null) {
            for (String key : ipsSection.getKeys(false)) {
                // YAML keys don't like dots, so we might have encoded them or just used them
                String ip = key.replace("-", ".");
                jailedIPs.put(ip, ipsSection.getLong(key));
            }
        }

        String holoId = dataConfig.getString("hologram_uuid");
        if (holoId != null) {
            hologramUUID = UUID.fromString(holoId);
        }
    }

    private void saveData() {
        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, JailRecord> entry : jailed.entrySet()) {
            dataConfig.set("jailed." + entry.getKey().toString() + ".time", entry.getValue().releaseTime);
            dataConfig.set("jailed." + entry.getKey().toString() + ".reason", entry.getValue().reason);
        }
        for (Map.Entry<String, Long> entry : jailedIPs.entrySet()) {
            dataConfig.set("ips." + entry.getKey().replace(".", "-"), entry.getValue());
        }
        if (hologramUUID != null) {
            dataConfig.set("hologram_uuid", hologramUUID.toString());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }

    private Location loadLocation(String path) {
        String worldName = plugin.getConfig().getString(path + ".world");
        if (worldName == null)
            return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;

        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        return new Location(world, x, y, z);
    }

    private void startScheduler() {
        new org.bukkit.scheduler.BukkitRunnable() {
            int secondCounter = 0;

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, JailRecord>> it = jailed.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, JailRecord> entry = it.next();
                    if (now >= entry.getValue().releaseTime) {
                        UUID uuid = entry.getKey();
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> release(uuid));
                        it.remove();
                    } else if (secondCounter == 0) {
                        Player p = org.bukkit.Bukkit.getPlayer(entry.getKey());
                        if (p != null && p.isOnline()) {
                            long secondsLeft = (entry.getValue().releaseTime - now) / 1000;
                            p.sendMessage("§cあなたは投獄されています。" + formatTimeRemaining(secondsLeft) + "後に釈放されます。");
                        }
                    }
                }
                updateHologram();
                secondCounter = (secondCounter + 1) % 60;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void updateHologram() {
        Location loc = loadLocation("hologram");
        if (loc == null)
            return;

        Entity entity = hologramUUID != null ? Bukkit.getEntity(hologramUUID) : null;
        TextDisplay display;
        if (entity == null || !(entity instanceof TextDisplay existing)) {
            display = loc.getWorld().spawn(loc, TextDisplay.class);
            hologramUUID = display.getUniqueId();
            saveData();
        } else {
            display = existing;
            display.teleport(loc);
        }

        StringBuilder sb = new StringBuilder("§e§l[ 投獄中のプレイヤー ]\n");
        if (jailed.isEmpty()) {
            sb.append("§7現在、投獄されているプレイヤーはいません。");
        } else {
            for (Map.Entry<UUID, JailRecord> entry : jailed.entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                long secondsLeft = (entry.getValue().releaseTime - System.currentTimeMillis()) / 1000;
                if (secondsLeft < 0)
                    secondsLeft = 0;
                sb.append(String.format("§f- %s §7(残り: %s) §c理由: %s\n", name, formatTimeRemaining(secondsLeft),
                        entry.getValue().reason));
            }
        }
        display.setText(sb.toString());
    }

    public String formatTimeRemaining(long seconds) {
        if (seconds <= 0)
            return "0秒";
        if (seconds >= 86400)
            return (seconds / 86400) + "日";
        if (seconds >= 3600)
            return (seconds / 3600) + "時間";
        if (seconds >= 60)
            return (seconds / 60) + "分";
        return seconds + "秒";
    }
}