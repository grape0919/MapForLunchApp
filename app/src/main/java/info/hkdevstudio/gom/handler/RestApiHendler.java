package info.hkdevstudio.gom.handler;

import android.os.AsyncTask;
import android.util.Log;
import info.hkdevstudio.gom.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestApiHendler {

    final static String REST_API_KEY = "61273287a5a93b183d1e0525f734e787";

    public static String getApi(final String msg){
        final String[] result = {""};
        // Open the connection
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // All your networking logic
                // should be here
                String r = null;
                try {

                    URL url = new URL(msg);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Authorization","KakaoAK " + REST_API_KEY);
                    conn.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                    conn.setRequestProperty("Accept","*/*");
                    conn.setRequestMethod("GET");
                    Log.d("REST API", "response Cosde : " + conn.getResponseCode());
                    InputStream is = conn.getInputStream();

                    // Get the stream
                    StringBuilder builder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    // Set the result
                    r = builder.toString();

                    Log.d("REST_API", "GET method succeed: " + r);
                    result[0] = r;
                }
                catch (Exception e) {
                    // Error calling the rest api
                    Log.e("REST_API", "GET method failed: " + e.getMessage());
                    e.printStackTrace();
                    result[0] = null;
                }
            }
        });
        return result[0];
    }
}
