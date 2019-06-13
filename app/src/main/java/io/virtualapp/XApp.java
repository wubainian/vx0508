package io.virtualapp;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.utils.OSUtils;

import io.virtualapp.delegate.MyVirtualInitializer;

/**
 * @author Lody
 */
public class XApp extends Application {

    private static final String TAG = "XApp";

    public static final String XPOSED_INSTALLER_PACKAGE = "de.robv.android.xposed.installer";

    private static XApp gApp;

    public static XApp getApp() {
        return gApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        gApp = this;
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (OSUtils.getInstance().isMiui()) {
                //MIUI
                try {
                    Class<?> klazz = Class.forName("dalvik.system.VMRuntime");
                    Object vmRuntime = klazz.getMethod("getRuntime").invoke(null);
                    klazz.getMethod("disableJitCompilation").invoke(vmRuntime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                NativeEngine.disableJit(Build.VERSION.SDK_INT);
            }
        }
        VASettings.ENABLE_IO_REDIRECT = true;
        VASettings.ENABLE_INNER_SHORTCUT = false;
        try {
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new MyVirtualInitializer(this, virtualCore));
    }

}
