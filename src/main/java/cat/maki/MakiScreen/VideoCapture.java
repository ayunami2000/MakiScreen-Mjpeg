package cat.maki.MakiScreen;

import cat.maki.MakiScreen.MJPG.MjpegFrame;
import cat.maki.MakiScreen.MJPG.MjpegInputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

class VideoCaptureMjpeg extends Thread {
    public boolean running = true;

    public void onFrame(BufferedImage frame) { }

    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public void run() {
        while(this.isAlive()&&this.running) {
            String currUrl=ConfigFile.getUrl();
            try {
                MjpegInputStream in = new MjpegInputStream(new URL(currUrl).openStream());

                MjpegFrame frame = null;

                try {
                    (new Thread(MakiScreen.audioPlayer)).start();
                    while (!MakiScreen.paused&&(frame = in.readMjpegFrame()) != null) {
                        onFrame(toBufferedImage(frame.getImage()));
                        if(!currUrl.equals(ConfigFile.getUrl()))in.close();
                        if(!MakiScreen.audioPlayer.isEnabled())(new Thread(MakiScreen.audioPlayer)).start();
                    }
                }catch(EOFException e){}
                in.close();
            } catch (IOException e) {}
            MakiScreen.audioPlayer.stopIt();
            if(MakiScreen.paused||currUrl.equals(ConfigFile.getUrl())) {
                do {
                    try {
                        Thread.sleep(MakiScreen.paused?1000:ConfigFile.getDelay());
                    } catch (InterruptedException e) {
                    }
                } while (MakiScreen.paused);
            }
        }
    }

    public void cleanup() {
        running = false;
    }
}

public class VideoCapture extends Thread {
    public int width;
    public int height;
    MakiScreen plugin;
    public static BufferedImage currentFrame;

    VideoCaptureMjpeg videoCaptureMjpeg;

    public VideoCapture(MakiScreen plugin, int width, int height) {
        this.plugin = plugin;
        this.width = width;
        this.height = height;


        videoCaptureMjpeg = new VideoCaptureMjpeg() {
            @Override
            public void onFrame(BufferedImage frame) {
                currentFrame = frame;
            }
        };
        videoCaptureMjpeg.start();

    }

    public void cleanup() {
        videoCaptureMjpeg.cleanup();
    }
}
