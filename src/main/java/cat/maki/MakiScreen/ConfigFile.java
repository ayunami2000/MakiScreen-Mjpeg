package cat.maki.MakiScreen;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ConfigFile extends BukkitRunnable {
    private static int size;
    private static int delay;
    private static String url;
    private static int mapSize;
    private static int mapWidth;
    private static int VCWidth;
    private static int VCHeight;
    private static FileConfiguration config;
    public Plugin plugin;

    //create config file if it doesn't exist
    public ConfigFile(@NotNull Plugin plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveDefaultConfig();
        }
        config = plugin.getConfig();
    }

    @Override
    public void run() {
        this.config = plugin.getConfig();
        if (config.contains("size")&&config.getInt("size") != 0&&config.getInt("size") <= 3) {
            ConfigSize(config.getInt("size"));
        } else {
            config.addDefault("size", 2);
        }
        if (config.contains("delay")&&config.getInt("delay") >= 0) {
            delay=config.getInt("delay");
        } else {
            config.addDefault("delay", 10000);
        }
        if (config.contains("url")&&!config.getString("url").isEmpty()) {
            url=config.getString("url");
        } else {
            config.addDefault("url", "null");
        }
    }

    private void ConfigSize(int sizee) {
        size=sizee;
        switch (sizee) {
            case 1 -> {
                mapSize = 2;
                mapWidth = 2;
                VCWidth = 128 * 2;
                VCHeight = 128;
            }
            case 2 -> {
                mapSize = 8;
                mapWidth = 4;
                VCWidth = 128 * 4;
                VCHeight = 128 * 2;
            }
            case 3 -> {
                mapSize = 32;
                mapWidth = 8;
                VCWidth = 128 * 8;
                VCHeight = 128 * 4;
            }
        }

    }

    public static int getMapSize() {
        return mapSize;
    }

    public static int getMapWidth() {
        return mapWidth;
    }

    public static int getVCWidth() {
        return VCWidth;
    }

    public static int getVCHeight() {
        return VCHeight;
    }

    public static String getUrl() {
        return url;
    }

    public static int getDelay() {
        return delay;
    }

    public static int getSize() {
        return size;
    }

    public static void setVal(String key, Object val){
        config.set(key,val);
        switch(key){
            case "size":
                size=(int)val;
                break;
            case "url":
                url=(String)val;
                break;
            case "delay":
                delay=(int)val;
                break;
            default:
        }
    }
}