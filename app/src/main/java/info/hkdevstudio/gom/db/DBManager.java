package info.hkdevstudio.gom.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBManager extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "GOM";

    SQLiteDatabase db;

    public DBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        this.db.execSQL(UserContDB.CREATE_QUERY);
        insertUserConf(UserContDB.DISTANCE_KEY, UserContDB.DEFAULT_DISTANCE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(UserContDB.DELETE_QUERY);
        onCreate(db);
    }

    public String selectUserConf(String key){
        Cursor cursor = this.db.query(
                UserContDB.TABLE_NAME, UserContDB.PROJECTION_ALL
                , UserContDB.SELECTION, UserContDB.gerSELCTION_ARG(key)
                , null, null, null);

        if(cursor.getCount() > 0){
            cursor.moveToFirst();
            return cursor.getString(1);
        }else{
            return null;
        }
    }

    public void insertUserConf(String key, String value){
        if(selectUserConf(key) == null) {
            this.db.insert(UserContDB.TABLE_NAME, null
                    , UserContDB.getContentValues(key, value));
        }
    }

    public void updateUserConf(String key, String value){
        this.db.update(UserContDB.TABLE_NAME, UserContDB.getContentValues(key, value)
                , UserContDB.SELECTION, UserContDB.gerSELCTION_ARG(key));
    }

}
