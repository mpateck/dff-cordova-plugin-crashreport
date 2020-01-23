/**
 *
 */
package com.dff.cordova.plugin.crashreport;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;

import com.dff.cordova.plugin.common.AbstractPluginListener;
import com.dff.cordova.plugin.common.log.CordovaPluginLog;
import com.dff.cordova.plugin.crashreport.json.model.*;
import com.dff.cordova.plugin.packagemanager.model.json.JSONPackageInfo;

import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author frank
 */
public class CrashReporter extends AbstractPluginListener implements UncaughtExceptionHandler {
  public static final String LOG_TAG = "com.dff.cordova.plugin.crashreport.CrashReporter";

  private static final int PACKAGE_INFO_FLAGS = PackageManager.GET_ACTIVITIES
    | PackageManager.GET_CONFIGURATIONS
    | PackageManager.GET_GIDS
    | PackageManager.GET_INSTRUMENTATION
    | PackageManager.GET_INTENT_FILTERS
    | PackageManager.GET_META_DATA
    | PackageManager.GET_PERMISSIONS
    | PackageManager.GET_PROVIDERS
    | PackageManager.GET_RECEIVERS
    | PackageManager.GET_SERVICES
    | PackageManager.GET_SHARED_LIBRARY_FILES
    | PackageManager.GET_SIGNATURES
    | PackageManager.GET_URI_PERMISSION_PATTERNS;

  private static volatile boolean mCrashing = false;
  private UncaughtExceptionHandler defaultHandler;
  private CordovaInterface cordova;

  public CrashReporter(UncaughtExceptionHandler defaultHandler, CordovaInterface cordova) {
    this.defaultHandler = defaultHandler;
    this.cordova = cordova;
  }

  public static JSONObject createCrashreport(Thread t, Throwable e, Context context) {
    JSONObject jsonCrashReport = new JSONObject();
    String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS").format(new Date());
    int pid = android.os.Process.myPid();
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.RunningAppProcessInfo myMemoryOutState = new ActivityManager.RunningAppProcessInfo();
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

    ActivityManager.getMyMemoryState(myMemoryOutState);
    activityManager.getMemoryInfo(memoryInfo);
    android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
    List<ActivityManager.RunningAppProcessInfo> runningAppProcessessInfo = activityManager.getRunningAppProcesses();
    List<ActivityManager.ProcessErrorStateInfo> processErrorStateInfo = activityManager.getProcessesInErrorState();
    List<ActivityManager.RunningServiceInfo> runningServiceInfo = activityManager.getRunningServices(1000);

    String packagename = context.getPackageName();
    PackageManager packageManager = context.getPackageManager();

    PackageInfo packageinfo = null;
    try {
      packageinfo = packageManager.getPackageInfo(packagename, PACKAGE_INFO_FLAGS);
    } catch (NameNotFoundException e1) {
      CordovaPluginLog.e(LOG_TAG, e1.getMessage(), e1);
    }

    JSONObject jsonOS = new JSONObject();
    try {
      if (packageinfo != null) {
        jsonCrashReport.put("packageInfo", JSONPackageInfo.toJSON(packageinfo));
      }

      jsonOS.put("CODENAME", Build.VERSION.CODENAME);
      jsonOS.put("INCREMENTAL", Build.VERSION.INCREMENTAL);
      jsonOS.put("RELEASE", Build.VERSION.RELEASE);
      jsonOS.put("SDK_INT", Build.VERSION.SDK_INT);
      try {
        jsonCrashReport.put("os", jsonOS);
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("directories", getDirectories(context));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }

      try {
        jsonCrashReport.put("runningAppProcesses", JsonRunningAppProcessInfo.toJson(runningAppProcessessInfo));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("processErrorStateInfo", JsonProcessErrorStateInfo.toJson(processErrorStateInfo));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("runningServiceInfo", JsonRunningServiceInfo.toJson(runningServiceInfo));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("myMemoryState", JsonRunningAppProcessInfo.toJson(myMemoryOutState));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("memoryInfo", JsonMemoryInfo.toJson(memoryInfo));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("debugMemoryInfo", JsonDebugMemoryInfo.toJson(memoryInfos));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("memoryClass", activityManager.getMemoryClass());
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("lowRamDevice", activityManager.isLowRamDevice());
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("isUserAMonkey", ActivityManager.isUserAMonkey());
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("isRunningInTestHarness", ActivityManager.isRunningInTestHarness());
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {

        jsonCrashReport.put("pid", pid);
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("date", date);
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("thread", JsonThread.toJson(t));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
      try {
        jsonCrashReport.put("throwable", JsonThrowable.toJson(e));
      } catch (JSONException je) {
        CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
      }
    } catch (JSONException je) {
      CordovaPluginLog.e(LOG_TAG, je.getMessage(), je);
    }
    if (isExternalStorageWritable()) {
      File crashReportDir = new File(context.getExternalFilesDir(null), "crashreports");

      if (!crashReportDir.mkdirs()) {
        CordovaPluginLog.w(LOG_TAG, crashReportDir.getAbsolutePath() + " not created");
      }

      if (crashReportDir.exists()) {
        String filename = "crashreport_" + date + ".txt";
        File crashReportFile = new File(crashReportDir, filename);

        try {
          if (!crashReportFile.exists() && crashReportFile.createNewFile()) {
            CordovaPluginLog.i(LOG_TAG, crashReportFile.getAbsolutePath() + " created");
          }

          FileOutputStream outputStream = new FileOutputStream(crashReportFile, true);

          outputStream.write(jsonCrashReport.toString().getBytes());
          outputStream.flush();
          outputStream.close();
        } catch (IOException ioe) {
          CordovaPluginLog.e(LOG_TAG, ioe.getMessage(), ioe);
        }
      } else {
        CordovaPluginLog.w(LOG_TAG, crashReportDir.getAbsolutePath() + " does not exist");
      }
    }

    return jsonCrashReport;
  }

  public void cleanLogs(Context context) {
    if (isExternalStorageWritable()) {
      File crashReportDir = new File(context.getExternalFilesDir(null), "crashreports");

      if (!crashReportDir.mkdirs()) {
        CordovaPluginLog.w(LOG_TAG, crashReportDir.getAbsolutePath() + " not created");
      }

      if (crashReportDir.exists()) {
        File[] files = crashReportDir.listFiles();
        for (int i = 0; i < files.length; i++) {
          try {
            CordovaPluginLog.d(LOG_TAG, "Deleting file " + files[i].getName());
            if (!files[i].delete()) {
              CordovaPluginLog.d(LOG_TAG, "File " + files[i].getName() + " will be deleted on exit");
              files[i].deleteOnExit();
            }
          } catch (Exception e) {
            CordovaPluginLog.e(LOG_TAG, "File " + files[i].getName() + " could not be deleted", e);
          }
        }

      } else {
        CordovaPluginLog.w(LOG_TAG, crashReportDir.getAbsolutePath() + " does not exist");
      }
    }
  }

  public JSONArray getLogList(Context context) {
    JSONArray logs = new JSONArray();

    if (isExternalStorageWritable()) {
      File crashReportDir = new File(context.getExternalFilesDir(null), "crashreports");

      if (!crashReportDir.mkdirs()) {
        CordovaPluginLog.w(LOG_TAG, crashReportDir.getAbsolutePath() + " not created");
      }

      if (crashReportDir.exists()) {
        File[] files = crashReportDir.listFiles();
        for (int i = 0; i < files.length; i++) {
          try {
            String line = "";
            StringBuffer b = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(files[i]));
            while ((line = br.readLine()) != null) {
              b.append(line);
            }
            JSONObject obj = new JSONObject();
            obj.put("file", files[i].getName());
            obj.put("data", new JSONObject(b.toString()));
            logs.put(i, obj);
          } catch (Exception e) {
            CordovaPluginLog.e(LOG_TAG, "Failed to add log to array", e);
          }
        }
        CordovaPluginLog.d(LOG_TAG, "Found " + logs.length() + " logs");

      } else {
        CordovaPluginLog.w(LOG_TAG, crashReportDir.getAbsolutePath() + " does not exist");
      }
    }
    return logs;
  }

  private static JSONObject getDirectories(Context context) throws JSONException {
    JSONObject space = new JSONObject();
    try {
      space.put("appData", getFileInfo(context, context.getApplicationInfo().dataDir));
    } catch (Exception e) {
    }
    try {
      space.put("appInternal", getFileInfo(context, context.getFilesDir()));
    } catch (Exception e) {
    }
    space.put("appCache", getFileInfo(context, context.getCacheDir()));
    try {
      space.put("appExternal", getFileInfo(context, context.getExternalFilesDirs(null)));
    } catch (Exception e) {
    }
    try {
      space.put("appExternalCache", getFileInfo(context, context.getExternalCacheDirs()));
    } catch (Exception e) {
    }
    try {

      space.put("data", getFileInfo(context, Environment.getDataDirectory()));
    } catch (Exception e) {
    }
    try {
      space.put("documentsExternal", getFileInfo(context, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)));
    } catch (Exception e) {
    }
    try {
      space.put("external", getFileInfo(context, Environment.getExternalStorageDirectory()));
    } catch (Exception e) {
    }
    try {
      space.put("isExternalStorageEmulated", Environment.isExternalStorageEmulated());
    } catch (Exception e) {
    }
    try {
      space.put("isExternalStorageRemovable", Environment.isExternalStorageRemovable());
    } catch (Exception e) {
    }

    return space;
  }

  private static JSONArray getFileInfo(Context context, File[] files) throws JSONException {
    JSONArray filesSpace = new JSONArray();

    if (files != null) {
      for (File f : files) {
        filesSpace.put(getFileInfo(context, f));
      }
    }

    return filesSpace;
  }

  private static JSONObject getFileInfo(Context context, String file) throws JSONException {
    return getFileInfo(context, new File(file));
  }

  private static JSONObject getFileInfo(Context context, File file) throws JSONException {
    JSONObject ms = new JSONObject();

    if (file != null) {
      try {
        long total = file.getTotalSpace();
        long free = file.getFreeSpace();
        long usable = file.getUsableSpace();
        double used = ((double) (total - free) / total) * 100;

        ms.put("path", file.getAbsolutePath());
        ms.put("total", total);
        ms.put("totalHuman", Formatter.formatFileSize(context, total));
        ms.put("free", free);
        ms.put("freeHuman", Formatter.formatFileSize(context, free));
        ms.put("usable", usable);
        ms.put("usableHuman", Formatter.formatFileSize(context, usable));
        ms.put("used", used);

        ms.put("canRead", file.canRead());
        ms.put("canWrite", file.canWrite());
        ms.put("canExecute", file.canExecute());

      } catch (SecurityException e) {
        CordovaPluginLog.e(LOG_TAG, e.getMessage(), e);
      }
    }

    return ms;
  }

  /* Checks if external storage is available for read and write */
  private static boolean isExternalStorageWritable() {
    return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
  }

  /* (non-Javadoc)
   * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
   */
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    // Don't re-enter -- avoid infinite loops if crash-reporting crashes.
    if (mCrashing) {
      return;
    }
    mCrashing = true;

    try {
      CordovaPluginLog.e(LOG_TAG, e.getMessage(), e);
      JSONObject jsonCrashReport = createCrashreport(t, e, this.cordova.getActivity());
      super.sendPluginResult(jsonCrashReport);
    } catch (Throwable e1) {
      try {
        CordovaPluginLog.e(LOG_TAG, e1.getMessage(), e1);
      } catch (Throwable t3) {
        Log.e(LOG_TAG, t3.getMessage(), t3);
      }
    } finally {
      // finally call initial exception handler
      defaultHandler.uncaughtException(t, e);
    }
  }

}
