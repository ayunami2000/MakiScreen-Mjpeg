package cat.maki.MakiScreen;

import net.minecraft.server.v1_5_R3.Packet131ItemData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class FramePacketSender extends BukkitRunnable implements Listener {
  private long frameNumber = 0;
  private final Queue<int[][]> frameBuffers;
  private final MakiScreen plugin;

  public FramePacketSender(MakiScreen plugin, Queue<int[][]> frameBuffers) {
    this.frameBuffers = frameBuffers;
    this.plugin = plugin;
    this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void run() {
    int[][] buffers = frameBuffers.poll();
    if (buffers == null) {
      return;
    }
    List<Packet131ItemData> packets = new ArrayList<>(MakiScreen.screens.size());
    for (ScreenPart screenPart : MakiScreen.screens) {
      int[] buffer = buffers[screenPart.partId];
      if (buffer != null) {
        MapPacketCodecBukkit mapPacketCodec = new MapPacketCodecBukkit(screenPart.mapId);
        mapPacketCodec.deflate(0);
        /*
        int[] intBuffer = new int[buffer.length];
        for (int i = 0; i < buffer.length; i++){
          intBuffer[i] = (-16777216)
                  | ((buffer[i++] & 0xff) << 16)
                  | ((buffer[i++] & 0xff) << 8)
                  | (buffer[i++] & 0xff);
        }
        */
        mapPacketCodec.setPixels(buffer);
        Packet131ItemData packet = (Packet131ItemData) mapPacketCodec.getNextBukkitPacket();
        if (packet == null) continue;
        if (!screenPart.modified) {
          packets.add(0, packet);
        } else {
          packets.add(packet);
        }
        screenPart.modified = true;
        screenPart.lastFrameBuffer = buffer;
      } else {
        screenPart.modified = false;
      }
    }

    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      sendToPlayer(onlinePlayer, packets);
    }

    if (frameNumber % 300 == 0) {
      int[][] peek = frameBuffers.peek();
      if (peek != null) {
        frameBuffers.clear();
        frameBuffers.offer(peek);
      }
    }
    frameNumber++;
  }

  /*
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    //do i REALLY need this to be added to the task list? disabled for now...
    new BukkitRunnable() {
      @Override
      public void run() {
        List<Packet131ItemData> packets = new ArrayList<>();
        for (ScreenPart screenPart : MakiScreen.screens) {
          if (screenPart.lastFrameBuffer != null) {
            //this SHOULD work but it doesnt lol
            packets.add(getPacket(screenPart.mapId, screenPart.lastFrameBuffer));
          }
        }
        sendToPlayer(event.getPlayer(), packets);
        //todo: maybe remove from task list once we get here?
      }
    }.runTaskLater(plugin, 10);
    //MakiScreen.tasks.add(task);
  }
  */

  private void sendToPlayer(Player player, List<Packet131ItemData> packets) {
    for (Packet131ItemData packet : packets) {
      MapPacketCodecBukkit.nativeSendPacketToPlayer(player, packet);
    }
  }
}
