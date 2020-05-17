package info.hkdevstudio.gom.view.map;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import info.hkdevstudio.gom.R;
import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomBalloonAdapter implements CalloutBalloonAdapter {
    private final View mCalloutBalloon;

    private String url = "";

    public CustomBalloonAdapter(Activity context) {
        this.mCalloutBalloon = context.getLayoutInflater().inflate(R.layout.custom_balloon, null);
        mCalloutBalloon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(url != null && !url.equals("")){
                    //TODO webView 구현
                    System.out.println("!@#!@# 이것도 작동하나?");
                }
            }
        });
    }

    @Override
    public View getCalloutBalloon(MapPOIItem mapPOIItem) {
        String info = mapPOIItem.getItemName();
        JSONObject obj = null;
        try {
            obj = new JSONObject(info);
            // parsing 해서 이미지 미리보기 만들기
            // 불가능.. 파싱 못하게 막아 놓은듯..
            String url = obj.getString("place_url");

            //((ImageView)mCalloutBalloon.findViewById(R.id.preview_img)).setImageResource(R.drawable.ic_launcher_foreground);

            ((TextView)mCalloutBalloon.findViewById(R.id.place_name)).setText(obj.getString("place_name"));
            ((TextView)mCalloutBalloon.findViewById(R.id.food)).setText(obj.getString("category_name"));
            ((TextView)mCalloutBalloon.findViewById(R.id.addrs)).setText(obj.getString("road_address_name"));
            ((TextView)mCalloutBalloon.findViewById(R.id.phone)).setText(obj.getString("phone"));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return mCalloutBalloon;
    }

    @Override
    public View getPressedCalloutBalloon(MapPOIItem mapPOIItem) {
        //TODO 클릭 시 슬라이더 webView 보여주기
        return mCalloutBalloon;
    }
}
