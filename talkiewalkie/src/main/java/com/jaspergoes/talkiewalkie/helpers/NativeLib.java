package com.jaspergoes.talkiewalkie.helpers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by Jasper on 17-2-2017.
 */

public class NativeLib {

    public static void init(Context context, String library) {

        ApplicationInfo appInfo = context.getApplicationInfo();
        String libName = "lib" + library + ".so";
        String destPath = context.getFilesDir().toString();
        try {
            String soName = destPath + File.separator + libName;
            new File(soName).delete();
            UnzipUtil.extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libName, destPath);
            System.load(soName);
        } catch (IOException e) {
            // extractFile to app files dir did not work. Not enough space? Try elsewhere...
            destPath = context.getExternalCacheDir().toString();
            // Note: location on external memory is not secure, everyone can read/write it...
            // However we extract from a "secure" place (our apk) and instantly load it,
            // on each start of the app, this should make it safer.
            String soName = destPath + File.separator + libName;
            new File(soName).delete(); // this copy could be old, or altered by an attack
            try {
                UnzipUtil.extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libName, destPath);
                System.load(soName);
            } catch (IOException e2) {
                Log.e("ERROR", "Exception in InstallInfo.init(): " + e);
                e.printStackTrace();
            }
        }

    }

}
