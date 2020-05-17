package info.hkdevstudio.gom.view.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import info.hkdevstudio.gom.R;

public class RadiusButton extends LinearLayout {
    TextView distance;
    ImageButton imageButton;

    public RadiusButton(Context context) {
        super(context);
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
        View v = li.inflate(R.layout.button_radius, this, false);
        addView(v);

        distance = findViewById(R.id.button_radius);
        imageButton = findViewById(R.id.button_radius_image);
    }

    public void setDistance(int dist){
        distance.setText(dist + "m");
    }

}
