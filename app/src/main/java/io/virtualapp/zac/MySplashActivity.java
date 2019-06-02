package io.virtualapp.zac;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.DeviceUtil;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.MD5Utils;
import com.lody.virtual.helper.utils.VLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.LoadingActivity;

/**
 * Created by v5snake(admin) on 2019/5/7/0007.
 */
public class MySplashActivity extends VActivity {
    private static final String TAG = MySplashActivity.class.getSimpleName();
    public static final String PACKAGE_XPOSED_INSTALLER = "de.robv.android.xposed.installer";
    public static final String PACKAGE_TENCENT_MM = "com.tencent.mm";
    public static final String PACKAGE_HOOK_VX = "com.bt.hook.vx";

    private static final int msg_code = 101;
    private static final int delay_time = 1000;
    private int installModuleNum = 0;
    private Handler mHandler = new MyHandler();

    private TextView tv_result;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_my_splash);

        tv_result = findViewById(R.id.tv_result);

        startVxEngine();
    }

    @Override
    protected void onResume() {
        super.onResume();

        reInstallModules();
    }

    private void startMmActivity() {
        boolean result = LoadingActivity.launch(MySplashActivity.this, PACKAGE_TENCENT_MM, 0);
        if (!result) {
            throw new ActivityNotFoundException("can not launch activity for :" + PACKAGE_TENCENT_MM);
        }
    }
    private void startXposedActivity() {
        boolean result = LoadingActivity.launch(MySplashActivity.this, PACKAGE_XPOSED_INSTALLER, 0);
        if (!result) {
            throw new ActivityNotFoundException("can not launch activity for :" + PACKAGE_XPOSED_INSTALLER);
        }
    }

    private void reInstallModules() {
        installModuleNum = 0;
        mHandler.sendEmptyMessageDelayed(msg_code, delay_time);
        installModules();
    }

    private void installModules() {
        String[][] modules = new String[][]{
                {
                        PACKAGE_XPOSED_INSTALLER,
                        "XposedInstaller_3.1.5.apk",
                        "XposedInstaller_3.1.5.apk_",
                        "8537fb219128ead3436cc19ff35cfb2e",
                        "正在安装模块1",
                        "false"
                },
                {
                        PACKAGE_HOOK_VX,
                        "app-vx.apk",
                        "app-debug-robot-1.5.201906021130.apk_",
                        "84D566ED6BF932AA870183681E570E49",
                        "正在安装模块2",
                        "true"
                },
                {
                        PACKAGE_TENCENT_MM,
                        "weixin703android1400.apk",
                        "weixin703android1400.apk_",
                        "CAD927E843B4382F3C757EBE8A95722B",
                        "正在安装模块3",
                        "false"
                }
        };

        for(int i=0; i<modules.length; i++){
            String[] module = modules[i];

            boolean isXposedAppInstalled = checkAppIsInstalled(module[0]);
            if(isXposedAppInstalled) {
                if("true".equalsIgnoreCase(module[5])) {
                    File appFile = getFileStreamPath(module[1]);
                    String dataFileMD5 = null;
                    try {
                        dataFileMD5 = MD5Utils.getFileMD5String(appFile);
                    } catch (Exception e) {
                        Log.e(TAG, "failed to get file md5, file=" + module[1], e);
                    }
                    if(!module[3].equalsIgnoreCase(dataFileMD5)){
                        isXposedAppInstalled = false;
                        Log.d(TAG, "onCreate : app is installed but md5 is not correct. file=" + module[1]);
                    }else{
                        Log.d(TAG, "onCreate : app md5 is checked successfully. file=" + module[1]);
                    }
                }
            }
            Log.d(TAG, "onCreate :isXposedAppInstalled=" + isXposedAppInstalled);
            if(!isXposedAppInstalled) {
                installApps(module[0], module[1], module[2], module[3], module[4], module[5]);
            }else{
                installModuleNum++;
            }
        }
    }

    /**
     * 启动引擎
     */
    private void startVxEngine() {
        VUiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            doActionInThread();
            time = System.currentTimeMillis() - time;
            long delta = 100L - time;
            if (delta > 0) {
                VUiKit.sleep(delta);
            }
        }).done((res) -> {

        });
    }

    private void doActionInThread() {
        if (!VirtualCore.get().isEngineLaunched()) {
            VirtualCore.get().waitForEngine();
        }
    }

    private void installApps(String packageName, String dataFileName, String assetsFileName, String assetsFileMd5, String msg, String checkApk) {
        File appFile = getFileStreamPath(dataFileName);
        copyAssetsFile(appFile, assetsFileName);
        Log.d(TAG, "install dataFileName=" + dataFileName);
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(msg);
        dialog.show();

        new Thread(){
            @Override
            public void run() {
                try {
                    if (appFile.isFile() && !DeviceUtil.isMeizuBelowN()) {
                        try {
                            String dataFileMD5 = MD5Utils.getFileMD5String(appFile);
                            if (assetsFileMd5.equalsIgnoreCase(dataFileMD5)) {
                                Log.d(TAG, "install appFile=" + appFile.getAbsolutePath());
                                VirtualCore.get().installPackage(appFile.getPath(), InstallStrategy.UPDATE_IF_EXIST);
                                if(!"true".equalsIgnoreCase(checkApk)){
                                    try {
                                        appFile.delete();
                                    } catch (Exception e) {  }
                                }
                            } else {
                                VLog.e(TAG, "unknown Xposed installer, ignore! dataFileMD5="+dataFileMD5 + ", assetsFileMd5="+assetsFileMd5);
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "failed to install app", e);
                        }
                    }else{
                        Log.e(TAG, "apkFile copy failed, assetsFileName=" + assetsFileName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "failed to install module", e);
                }
                installModuleNum++;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog(dialog);
                    }
                });
            }
        }.start();
    }

    private boolean checkAppIsInstalled(String packageName) {
        boolean isAppInstalled = false;
        try {
            isAppInstalled = VirtualCore.get().isAppInstalled(packageName);
        } catch (Throwable e) {
            VLog.d(TAG, "check app install failed. package=" + packageName, e);
        }
        return isAppInstalled;
    }

    private boolean copyAssetsFile(File destFilePath, String assetsFileName) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = getApplicationContext().getAssets().open(assetsFileName);
            output = new FileOutputStream(destFilePath);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            return true;
        } catch (Throwable e) {
            VLog.e(TAG, "copy file error", e);
        } finally {
            FileUtils.closeQuietly(input);
            FileUtils.closeQuietly(output);
        }
        return false;
    }

    private void dismissDialog(ProgressDialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private boolean enableXposedModule(){
        /*
        ugglite:/ # cat /data/data/de.robv.android.xposed.installer/shared_prefs/enabled_modules.xml
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
        <map>
            <int name="com.bt.hook.vx" value="1" />
        </map>
         */
        String filepath1 = "/data/data/io.va.exposed/virtual/data/user/0/de.robv.android.xposed.installer/shared_prefs/enabled_modules.xml";
        String filepath2 = "/data/data/io.va.exposed/virtual/data/user/0/de.robv.android.xposed.installer/exposed_conf/enabled_modules.list";
        String filepath3 = "/data/data/io.va.exposed/virtual/data/user/0/de.robv.android.xposed.installer/exposed_conf/modules.list";

        boolean isSuccess1 = MyUtil.writeFileContent(filepath1,
                "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" +
                        "<map>\n" +
                        "    <int name=\"com.bt.hook.vx\" value=\"1\" />\n" +
                        "</map>\n"
        );

        boolean isSuccess2 = MyUtil.writeFileContent(filepath2,
                "com.bt.hook.vx\n"
        );

        boolean isSuccess3 = MyUtil.writeFileContent(filepath3,
                "/data/user/0/io.va.exposed/virtual/data/app/com.bt.hook.vx/base.apk\n"
        );
        return isSuccess1 && isSuccess2 && isSuccess3;
    }

    private void listFilesCircle(File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files != null){
                for(File f : files){
                    Log.d(TAG, "isDir=" + f.isDirectory() + ", subfile=" + f.getAbsolutePath() );
                    if(f.isDirectory()){
                        listFilesCircle(f);
                    }else if(f.isFile()){
                        String[] careFileNames = new String[]{"enabled_modules.list", "modules.list", "enabled_modules.xml"};
                        for(String fileName : careFileNames){
                            if(fileName.equals(f.getName())){
                                BufferedReader br = null;
                                try {
                                    br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                                    String line= null;
                                    while( (line=br.readLine()) != null){
                                        Log.d(TAG, "content=" + line );
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "failed to read file=" + f.getAbsolutePath(), e);
                                }finally {
                                    if(br!=null){
                                        try {
                                            br.close();
                                        } catch (Exception e) {}
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
    private void listFiles(String filePath) {
        File file = new File(filePath);
        if(file.exists()){
            Log.d(TAG, "file exits : " + filePath );
            listFilesCircle(file);
        }else{
            Log.d(TAG, "file not exits : " + filePath );
        }
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(installModuleNum < 3 ){
                mHandler.sendEmptyMessageDelayed(msg_code, delay_time);
                return;
            }

            boolean isXposedAppInstalled = checkAppIsInstalled(PACKAGE_XPOSED_INSTALLER);
            boolean isMmAppInstalled = checkAppIsInstalled(PACKAGE_TENCENT_MM);
            boolean isVxAppInstalled = checkAppIsInstalled(PACKAGE_HOOK_VX);
            if(isXposedAppInstalled && isMmAppInstalled && isVxAppInstalled){
                Log.d(TAG, "start mm activity");
                tv_result.setText("成功");
                //listFiles("/data/data/" + MySplashActivity.this.getPackageName()+ "/virtual/");
                boolean isEnableXposedSuccess = enableXposedModule();
                if(isEnableXposedSuccess) {
                    boolean isStartedXposed = MyPreference.getIsStartedXposed(MySplashActivity.this);
                    if(isStartedXposed){
                        startMmActivity();
                    }else {
                        startXposedActivity();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                MyPreference.setIsStartedXposed(MySplashActivity.this, true);
                                startMmActivity();
                            }
                        }, 5 * 1000);
                    }
                    MySplashActivity.this.finish();
                }else{
                    Log.e(TAG, "failed to enable xposed module");
                }
            }else{
                tv_result.setText("失败");
                AlertDialog.Builder builder = new AlertDialog.Builder(MySplashActivity.this);
                builder.setTitle("安装失败");
                builder.setMessage("有模块未安装成功。您想要重试安装吗?");
                //点击对话框以外的区域是否让对话框消失
                builder.setCancelable(false);
                //设置正面按钮
                builder.setPositiveButton("重试", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        reInstallModules();
                    }
                });
                //设置反面按钮
                builder.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

}
