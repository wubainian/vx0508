package io.virtualapp.zac;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by v5snake(admin) on 2019/5/7/0007.
 */
public class MyPreference {
    private static final String my_name = "my_sp_vx";
    private static final String key_is_vx_started = "is_vx_started";

    public static boolean getIsStartedXposed(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(my_name, Context.MODE_PRIVATE);
        return sp.getBoolean(key_is_vx_started, false);
    }

    public static void setIsStartedXposed(Context ctx, boolean isStarted){
        SharedPreferences sp = ctx.getSharedPreferences(my_name, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key_is_vx_started, isStarted).commit();
    }
}
