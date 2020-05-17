package info.hkdevstudio.gom.db;

import android.content.ContentValues;
import android.provider.BaseColumns;

public class UserContDB implements BaseColumns {
    static final String TABLE_NAME = "USER_CONF";
    static final String COLUMN_NAME_KEY = "key";
    static final String COLUMN_NAME_VALUE = "value";

    public static final String DISTANCE_KEY = "distance";
    static final String DEFAULT_DISTANCE = "100";

    static final String CREATE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    _ID + " INTEGER," +
                    COLUMN_NAME_KEY + " TEXT PRIMARY KEY," +
                    COLUMN_NAME_VALUE + " TEXT)";

    static final String DELETE_QUERY =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    static final String[] PROJECTION_ALL = {
            COLUMN_NAME_KEY
            , COLUMN_NAME_VALUE
    };

    static final String SELECTION = COLUMN_NAME_KEY + " = ?";
    public static String[] gerSELCTION_ARG(String arg){
        return new String[]{ arg };
    }

    public static ContentValues getContentValues(String key, String value){
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_KEY, key);
        values.put(COLUMN_NAME_VALUE, value);
        return values;
    }

}
