package info.hkdevstudio.gom.handler;

import android.util.Log;
import android.util.Pair;
import info.hkdevstudio.gom.vo.DocumentVo;
import info.hkdevstudio.gom.vo.MetaVo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RestApiHandler {

    final static String REST_API_KEY = "61273287a5a93b183d1e0525f734e787";

    public static List<DocumentVo> getApi(final String msg) {
        List<DocumentVo> result = new ArrayList<>();
        try {

            URL url = new URL(msg);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "KakaoAK " + REST_API_KEY);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestMethod("GET");
            Log.d("REST API", "response Cosde : " + conn.getResponseCode());
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();

                // Get the stream
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                // Set the result
                result = parse(builder.toString()).second;

                Log.d("REST_API", "GET method succeed: " + result.toString());
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            // Error calling the rest api
            Log.e("REST_API", "GET method failed: " + e.getMessage());
            e.printStackTrace();
            result = new ArrayList<>();
        }
        return result;
    }


    public static Pair<MetaVo, List<DocumentVo>> parse(String jsonString) {

        MetaVo metaVo = new MetaVo();
        List<DocumentVo> documentVoList = new ArrayList<DocumentVo>();
        try {
            //META 정보
            JSONObject reader = new JSONObject(jsonString);
            JSONObject meta = reader.getJSONObject("meta");
            metaVo.setIs_end(meta.getBoolean("is_end"));
            metaVo.setPageable_count(meta.getInt("pageable_count"));
            metaVo.setTotal_count(meta.getInt("total_count"));


            //documents 정보
            JSONArray objects = reader.getJSONArray("documents");
            for (int i = 0; i < objects.length(); i++) {
                JSONObject object = objects.getJSONObject(i);
                //DocumentVo 클래스에 json 데이터 할당.
                DocumentVo documentVo = new DocumentVo();
                documentVo.setPlace_name(object.getString("place_name"));
                documentVo.setDistance(object.getString("distance"));
                documentVo.setPlace_url(object.getString("place_url"));
                documentVo.setX(object.getString("x"));
                documentVo.setY(object.getString("y"));
                documentVoList.add(documentVo);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Pair<MetaVo, List<DocumentVo>> result = new Pair<>(metaVo, documentVoList);

        return result;
    }

}