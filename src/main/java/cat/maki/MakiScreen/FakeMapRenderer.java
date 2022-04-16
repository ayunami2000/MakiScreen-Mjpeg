package cat.maki.MakiScreen;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class FakeMapRenderer extends MapRenderer {
    @Override
    public void initialize(MapView map) {
        System.out.println(123);
        ImageManager manager = ImageManager.getInstance();
        if (manager.hasImage(map.getId())) {
            MakiScreen.screens.add(new ScreenPart(map.getId(), manager.getImage(map.getId())));
        }
    }
    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        // trol
    }
}
