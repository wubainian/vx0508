package io.virtualapp.zac;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by v5snake(admin) on 2019/5/7/0007.
 */
public class MyUtil {
    private static final String TAG = MyUtil.class.getSimpleName();
    public static boolean writeFileContent(String filepath, String content){
        BufferedWriter br = null;
        try {
            File file1 = new File(filepath);
            File parentFile1 = file1.getParentFile();
            if(!parentFile1.exists()){
                parentFile1.mkdirs();
            }
            br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file1)));
            br.write(content);
            br.flush();
            Log.d(TAG, "enableXposedModule success file=" + filepath);
            file1.setReadable(true, false);
            file1.setWritable(true, false);
            file1.setExecutable(true, false);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "failed to enableXposedModule", e);
        }finally {
            if(br != null) {
                try {
                    br.close();
                } catch (Exception e) { }
            }
        }
        return false;
    }
}
