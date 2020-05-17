package info.hkdevstudio.gom.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import info.hkdevstudio.gom.MainActivity;
import info.hkdevstudio.gom.R;
import info.hkdevstudio.gom.conf.Configuration;
import info.hkdevstudio.gom.db.DBManager;
import info.hkdevstudio.gom.db.UserContDB;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InputDialog extends Dialog {

    NumberPicker radius;
    Button apply_button;
    Button cancel_button;

    DBManager db;
    Configuration conf;

    String[] numberList = {"100", "200", "300", "400", "500", "600", "700", "800","900","1000"};

    Context parent;

    public InputDialog(Context context, DBManager db, Configuration conf) {
        super(context);
        setContentView(R.layout.dialog_input);
        this.db = db;
        this.conf = conf;
        this.parent = context;
        radius = findViewById(R.id.dlg_input);
        apply_button = findViewById(R.id.apply);
        cancel_button = findViewById(R.id.cancel);

        radius.setMinValue(1);
        radius.setMaxValue(10);
        radius.setDisplayedValues(numberList);
        radius.setOnLongPressUpdateInterval(100);

        String distance = db.selectUserConf(UserContDB.DISTANCE_KEY);
        radius.setValue(Integer.parseInt(distance)/100);
    }

    @Override
    public void show() {
        apply_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.updateUserConf(UserContDB.DISTANCE_KEY, String.valueOf(radius.getValue()*100));
                conf.setDisatance(radius.getValue()*100);
                try {
                    Method renewLocationMethod = MainActivity.class.getMethod("renewLocation", null);
                    renewLocationMethod.invoke(parent, null);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                dismiss();
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        super.show();
    }
}
