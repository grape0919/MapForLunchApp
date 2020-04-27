package info.hkdevstudio.gom.handler;

import androidx.annotation.NonNull;

import java.util.Map;


public class RequestPram {
    final static String enpoint = "https://dapi.kakao.com/v2/local/search/keyword.json?";
    private double x;
    private double y;
    private int redius;
    private String query;
    private String category_group_code;
    private Map<String, String> properties;

    @NonNull
    @Override
    public String toString() {
        String value = enpoint + "y=" + y + "&x=" + x + "&redius=" + redius + "&query=" + query;
        value += (category_group_code != null && !category_group_code.equals(""))?"&category_group_code=" + category_group_code:"";
        return value;
    }

    public static String getEnpoint() {
        return enpoint;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getRedius() {
        return redius;
    }

    public void setRedius(int redius) {
        this.redius = redius;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setCategory_group_code(String cat) {
        category_group_code = cat;
    }
}
