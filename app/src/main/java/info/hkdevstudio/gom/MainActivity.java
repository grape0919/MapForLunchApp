package info.hkdevstudio.gom;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import net.daum.mf.map.api.MapView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapView mapView = new MapView(this);

        ViewGroup mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
    }
}
