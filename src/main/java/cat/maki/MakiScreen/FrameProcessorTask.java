package cat.maki.MakiScreen;

import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

class FrameProcessorTask extends BukkitRunnable {

  private final Object lock = new Object();
  private final Queue<int[][]> frameBuffers = new LinkedBlockingQueue<>(450); // notice: blocking whereas guava's is not blocking // EvictingQueue.create(450)
  private final int mapSize;

  private final int[][] cachedMapData;
  private final int frameWidth;
  private int[] frameData;

  FrameProcessorTask(int mapSize, int mapWidth) {
    this.mapSize = mapSize;
    this.frameWidth = mapWidth * 128;
    this.cachedMapData = new int[mapSize][];
  }

  public Queue<int[][]> getFrameBuffers() {
    return frameBuffers;
  }

  private static int pos(int x, int y, int width) {
    return (y * 3 * width) + (x * 3);
  }

  public static BufferedImage resize(BufferedImage img, int newW, int newH) {
    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
    BufferedImage dimg = new BufferedImage(newW, newH, img.getType()); // todo: combine with toBufferedImage() call for performance

    Graphics2D g2d = dimg.createGraphics();
    g2d.drawImage(tmp, 0, 0, null);
    g2d.dispose();

    return dimg;
  }

  @Override
  public void run() {
    synchronized (lock) {
      BufferedImage frame = VideoCapture.currentFrame;
      if (frame == null) {
        return;
      }
      if(frame.getWidth()!=ConfigFile.getVCWidth()||frame.getHeight()!=ConfigFile.getVCHeight()){
        frame = resize(frame,ConfigFile.getVCWidth(),ConfigFile.getVCHeight());
      }
      frameData = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();

      int[][] buffers = new int[mapSize][];

      for (int partId = 0; partId < buffers.length; partId++) {
        buffers[partId] = getMapData(partId, frameWidth);
      }

      if (frameBuffers.size() >= 450) {
        frameBuffers.remove();
      }
      frameBuffers.offer(buffers);
    }
  }

  private int[] getMapData(int partId, int width) {
    int offset = 0;
    int startX = ((partId % ConfigFile.getMapWidth()) * 128);
    int startY = ((partId / ConfigFile.getMapWidth()) * 128);
    int maxY = startY + 128;
    int maxX = startX + 128;

    boolean modified = false;
    int[] bytes = this.cachedMapData[partId];
    if (bytes == null) {
      bytes = new int[128 * 128];
      modified = true;
    }
    for (int y = startY; y < maxY; y++) {
      int yIndex = y * width;
      for (int x = startX; x < maxX; x++) {
        int newColor = frameData[yIndex + x];
        if (modified) {
          bytes[offset] = newColor;
        } else {
          if (bytes[offset] != newColor) {
            bytes[offset] = newColor;
            modified = true;
          }
        }
        offset++;
      }
    }

    if (modified) {
      this.cachedMapData[partId] = bytes;
      int[] result = new int[bytes.length];
      System.arraycopy(bytes,0, result, 0, bytes.length);
      return result;
    }
    return null;
  }

}
