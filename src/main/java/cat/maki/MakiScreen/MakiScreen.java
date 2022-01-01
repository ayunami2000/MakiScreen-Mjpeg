package cat.maki.MakiScreen;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public final class MakiScreen extends JavaPlugin implements Listener {

    private final Logger logger = getLogger();

    public static final Set<ScreenPart> screens = new TreeSet<>(
        Comparator.comparingInt(to -> to.mapId));
    private VideoCapture videoCapture;

    public static boolean paused = false;

    @Override
    public void onEnable() {
        ConfigFile configFile = new ConfigFile(this);
        configFile.run();

        ImageManager manager = ImageManager.getInstance();
        manager.init();

        logger.info("Enabling MakiScreen!");
        getServer().getPluginManager().registerEvents(this, this);

        logger.info("Config file loaded \n"+
                "Size: " + ConfigFile.getSize() +"\n"+
                " - Map Size: " + ConfigFile.getMapSize() +"\n"+
                " - Map Width: " + ConfigFile.getMapWidth() +"\n"+
                " - Width: " + ConfigFile.getVCWidth() +"\n"+
                " - Height: " + ConfigFile.getVCHeight() +"\n"+
                "URL: <REDACTED FOR PRIVACY> \n"+
                "Delay: " + ConfigFile.getDelay()
        );

        int mapSize = ConfigFile.getMapSize();
        int mapWidth = ConfigFile.getMapWidth();

        videoCapture = new VideoCapture(this,
                ConfigFile.getVCWidth(),
                ConfigFile.getVCHeight()
        );
        videoCapture.start();

        FrameProcessorTask frameProcessorTask = new FrameProcessorTask(mapSize, mapWidth);
        frameProcessorTask.runTaskTimerAsynchronously(this, 0, 1);
        FramePacketSender framePacketSender =
            new FramePacketSender(this, frameProcessorTask.getFrameBuffers());
        framePacketSender.runTaskTimerAsynchronously(this, 0, 1);
    }

    @Override
    public void onDisable() {
        logger.info("Disabling MakiScreen!");
        this.saveConfig();
        videoCapture.cleanup();
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        Player player = (Player) sender;
        if (command.getName().equals("maki")) {
            if(args.length==0){
                sender.sendMessage("Usage: /maki [give|clear|toggle|size|url|delay]\n - give: Generates new maps and gives them to you.\n - clear: Clears all map data.\n - toggle: Toggles map playback.\n - size: Sets or gets the current size value.\n - url: Sets or gets the current mjpeg url.\n - delay: Sets or gets the current delay value.");
                return true;
            }
            if (!sender.isOp()) {
                sender.sendMessage("Error: You don't have permission!");
                return true;
            }

            switch(args[0]){
                case "give":
                    if(sender instanceof ConsoleCommandSender) {
                        sender.sendMessage("Error: This command cannot be run from console!");
                    }else{
                        for (int i=0; i<ConfigFile.getMapSize(); i++) {
                            MapView mapView = getServer().createMap(player.getWorld());
                            mapView.setScale(MapView.Scale.CLOSEST);
                            mapView.setUnlimitedTracking(true);
                            for (MapRenderer renderer : mapView.getRenderers()) {
                                mapView.removeRenderer(renderer);
                            }

                            ItemStack itemStack = new ItemStack(Material.FILLED_MAP);

                            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
                            mapMeta.setMapView(mapView);

                            itemStack.setItemMeta(mapMeta);
                            player.getInventory().addItem(itemStack);
                            screens.add(new ScreenPart(mapView.getId(), i));
                            ImageManager manager = ImageManager.getInstance();
                            manager.saveImage(mapView.getId(), i);
                        }
                    }
                    break;
                case "clear":
                    try{
                        new PrintWriter(new File(this.getDataFolder(), "data.yml")).close();
                        sender.sendMessage("All maps have been reset!");
                        sender.sendMessage("§lNote: The current maps will not change until the server is restarted, and using the give command will restore map data!");
                    } catch (FileNotFoundException e) {
                        sender.sendMessage("Error: No data file was found, so nothing happened.");
                    }
                    break;
                case "toggle":
                    paused=!paused;
                    sender.sendMessage("MakiScreen is now "+(paused?"":"un")+"paused.");
                    break;
                case "size":
                    if(args.length==1) {
                        sender.sendMessage("Current size value is: " + ConfigFile.getSize());
                    }else{
                        try{
                            int size=Integer.parseInt(args[1]);
                            size=Math.min(3,Math.max(1,size));
                            ConfigFile.setVal("size",size);
                            sender.sendMessage("Size value now set to: "+size);
                            sender.sendMessage("§lNote: The size will not change until the server is restarted!");
                        }catch(NumberFormatException e){
                            sender.sendMessage("Error: Invalid command arguments!");
                        }
                    }
                    break;
                case "url":
                    if(args.length==1) {
                        sender.sendMessage("Current URL is: " + ConfigFile.getUrl());
                    }else{
                        ConfigFile.setVal("url",args[1]);
                        sender.sendMessage("URL is now: "+args[1]);
                    }
                    break;
                case "delay":
                    if(args.length==1) {
                        sender.sendMessage("Current delay value is: " + ConfigFile.getDelay());
                    }else{
                        try{
                            int delay=Integer.parseInt(args[1]);
                            delay=Math.max(0,delay);
                            ConfigFile.setVal("delay",delay);
                            sender.sendMessage("Delay value now set to: "+delay);
                        }catch(NumberFormatException e){
                            sender.sendMessage("Error: Invalid command arguments!");
                        }
                    }
                    break;
                default:
                    sender.sendMessage("Error: Invalid command arguments!");
            }
        }

        return true;
    }

}
