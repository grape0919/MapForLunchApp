package info.hkdevstudio.gom.view.map;

import android.view.View;
import android.widget.TextView;
import info.hkdevstudio.gom.MainActivity;
import info.hkdevstudio.gom.R;
import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomBalloonAdapter implements CalloutBalloonAdapter {
    private final View mCalloutBalloon;

    private String url = "";
    private MainActivity context;
    public CustomBalloonAdapter(final MainActivity context) {
        this.mCalloutBalloon = context.getLayoutInflater().inflate(R.layout.custom_balloon, null);
        this.context = context;

    }

    /**
     * balloon 에 뿌려줄 내용
     * @param mapPOIItem
     * @return
     */
    @Override
    public View getCalloutBalloon(MapPOIItem mapPOIItem) {
        String info = mapPOIItem.getItemName();
        JSONObject obj = null;
        try {
            obj = new JSONObject(info);
            // parsing 해서 이미지 미리보기 만들기
            // 불가능.. 파싱 못하게 막아 놓은듯..
            url = obj.getString("place_url");

            //((ImageView)mCalloutBalloon.findViewById(R.id.preview_img)).setImageResource(R.drawable.ic_launcher_foreground);

            ((TextView)mCalloutBalloon.findViewById(R.id.place_name)).setText(obj.getString("place_name"));
            ((TextView)mCalloutBalloon.findViewById(R.id.food)).setText(obj.getString("category_name"));
            String addrs = obj.getString("road_address_name");
            if ((addrs == null || addrs.equals(""))) {
                addrs = obj.getString("address_name");
            }
            ((TextView)mCalloutBalloon.findViewById(R.id.addrs)).setText((addrs!=null&&!addrs.equals(""))?addrs:"");
            ((TextView)mCalloutBalloon.findViewById(R.id.phone)).setText(obj.getString("phone"));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return mCalloutBalloon;
    }

    @Override
    public View getPressedCalloutBalloon(MapPOIItem mapPOIItem) {
        //context.selectMarker(url);
        return mCalloutBalloon;
    }
}
