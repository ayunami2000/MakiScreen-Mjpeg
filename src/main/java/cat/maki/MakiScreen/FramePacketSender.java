package cat.maki.MakiScreen;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class FramePacketSender extends BukkitRunnable implements Listener {
  private long frameNumber = 0;
  private final Queue<byte[][]> frameBuffers;
  private final MakiScreen plugin;

  public FramePacketSender(MakiScreen plugin, Queue<byte[][]> frameBuffers) {
    this.frameBuffers = frameBuffers;
    this.plugin = plugin;
    this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void run() {
    byte[][] buffers = frameBuffers.poll();
    if (buffers == null) {
      return;
    }
    List<byte[]> packets = new ArrayList<>(MakiScreen.screens.size());
    for (ScreenPart screenPart : MakiScreen.screens) {
      byte[] buffer = buffers[screenPart.partId];
      if (buffer != null) {
        MapPacketCodec mapPacketCodec = new MapPacketCodec(screenPart.mapId);
        mapPacketCodec.deflate(1);
        int[] intBuffer = new int[buffer.length];
        for (int i = 0; i < buffer.length; i++){
          int v = -16777216; // is this right?
          v += ((int) buffer[i++] & 0xff); // blue
          v += (((int) buffer[i++] & 0xff) << 8); // green
          v += (((int) buffer[i++] & 0xff) << 16); // red
          intBuffer[i] = v;
        }
        mapPacketCodec.setPixels(intBuffer);
        byte[] packet = mapPacketCodec.getNextPacket();
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
      byte[][] peek = frameBuffers.peek();
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

  private void sendToPlayer(Player player, List<byte[]> packets) {
    for (byte[] packet : packets) {
      player.sendPluginMessage(MakiScreen.INSTANCE, "EAG|AyunamiMap", packet);
    }
  }
}
