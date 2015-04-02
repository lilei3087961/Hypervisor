package com.android.hypervisor;

import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.app.ActivityManager; 
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Debug.MemoryInfo;


import android.util.Log;

import com.android.hypervisor.R;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *  It is expected that there should be only one
 * ApplicationManager object held in a static. Also provide APIs for updating the database state
 * for the hypervisor.
 */
public class ApplicationManager extends BroadcastReceiver {

    static final String TAG = "ApplicationManager";

    private int mBatchSize; // 0 is all apps at once
    private static boolean mAllAppsLoaded; //if application has been loaded to database ,default false; but it should get from  edit pref...
    private final HypervisorApplication mApp;
    private DeferredHandler mHandler = new DeferredHandler();


    private AllAppsList mBgAllAppsList;
    private IconCache mIconCache;
    private Bitmap mDefaultIcon;

    //check system application memory status
    private List<RunningAppProcessInfo> runningAppProcess; 
    private ActivityManager am; 

   

    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private LoaderTask mLoaderTask;
    private boolean mIsLoaderTaskRunning;    


    ApplicationManager(HypervisorApplication app, IconCache iconCache) {
        mApp = app;
        mAllAppsLoaded = false; 
        mBgAllAppsList = new AllAppsList(iconCache);
        mIconCache = iconCache;

        mDefaultIcon = Utilities.createIconBitmap(
                mIconCache.getFullResDefaultActivityIcon(), app);
        /** for running application process , in order to control memory and monitor*/
        am = (ActivityManager)mApp.getSystemService(Context.ACTIVITY_SERVICE);
 //       runningAppProcess = am.getRunningAppProcesses();

   }
   /** return all application information, we will use this in  IPC service */
   public AllAppsList  getAllAppInfo(){
          return mBgAllAppsList; 
   }

   /**check if the applicaton loading is finished. */
   public static boolean getLoadStatus(){
          return mAllAppsLoaded; 
   }


   /**
    * get all running app processes 
    */ 
   public List<RunningAppProcessInfo>  getRunningAppProcesses(){
       runningAppProcess = am.getRunningAppProcesses();
       

       return runningAppProcess; 
   }

    /**
      *  get one process private  memory  information, please check code: ActivityManager.java 
      */
    public int getPrivateMemory(String processName) {

        for (RunningAppProcessInfo proc : runningAppProcess) {
            if (!proc.processName.equals(processName)) {
                continue;
            }

            int[] pids = {
                    proc.pid };

            MemoryInfo meminfo = am.getProcessMemoryInfo(pids)[0];
            return meminfo.dalvikPrivateDirty;

        }
        return -1;
    }

    /** check if the system memory is low;  */ 
    public boolean  isMemoryLow(){

        ActivityManager manager = (ActivityManager)mApp.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(memInfo);

        return memInfo.lowMemory; 

    }
 
   
   /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
         final String action = intent.getAction();

         if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            int op = PackageUpdatedTask.OP_NONE;

            if (packageName == null || packageName.length() == 0) {
                // they sent us a bad intent
                return;
            }

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                op = PackageUpdatedTask.OP_UPDATE;
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_REMOVE;
                }
                // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                // later, we will update the package at this time
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_ADD;
                } else {
                    op = PackageUpdatedTask.OP_UPDATE;
                }
            }

            if (op != PackageUpdatedTask.OP_NONE) {
                enqueuePackageUpdated(new PackageUpdatedTask(op, new String[] { packageName }));
            }

           } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, packages));
            // Then, rebind everything.
            startLoaderFromBackground();
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            enqueuePackageUpdated(new PackageUpdatedTask(
                        PackageUpdatedTask.OP_UNAVAILABLE, packages));
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // If we have changed locale we need to clear out the labels in all apps/workspace.
            forceReload();
        } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
        } else if (SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED.equals(action) ||
                   SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED.equals(action)) {
        }
       
        }

     void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }


     public void startLoader() {  //chenrui , start query the applications

         mLoaderTask = new LoaderTask(mApp, false);
         sWorkerThread.setPriority(Thread.NORM_PRIORITY);
         sWorker.post(mLoaderTask);
 
    }

    public void startLoaderFromBackground() {
            startLoader();
    }


     private void forceReload() {

        // Do this here because if the launcher activity is running it will be restarted.
        // If it's not running startLoaderFromBackground will merely tell it that it needs
        // to reload.
        startLoaderFromBackground();
    }

    static void checkItemInfo(final ItemInfo item){
        final long itemId = item.id;
        
    } 

    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }


   
    

     private static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

  
     

    private class LoaderTask implements Runnable {
        private Context mContext;
        private boolean mIsLaunching;
        private boolean mStopped;

        private HashMap<Object, CharSequence> mLabelCache;

        LoaderTask(Context context, boolean isLaunching) {
            mContext = context;
            mIsLaunching = isLaunching;
            mLabelCache = new HashMap<Object, CharSequence>();
        }


        public void run() {

           android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

           loadAllAppsByBatch(); 

       }        
       private void loadAllAppsByBatch() {  //chenrui, load all application information using PackageManager...

           Log.v("ApplicationManager", ">>>>chenrui>>>>loadAllAppsByBatch");

           final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager packageManager =  mApp.getPackageManager();
            List<ResolveInfo> apps = null;

            int N = Integer.MAX_VALUE;

            int i=0;
            int batchSize = -1;
             Log.v("ApplicationManager", ">>>>chenrui>>>>loadAllAppsByBatch: step 1");
             while (i < N /* && !mStopped */) {
                if (i == 0) {
                    mBgAllAppsList.clear();
                    apps = packageManager.queryIntentActivities(mainIntent, 0);
                    if (apps == null) {
                        return;
                    }
                    N = apps.size();
                    Log.v("ApplicationManager", ">>>>chenrui>>>>apps.size N is: " + N);
                    if (N == 0) {
                        // There are no apps?!?
                        return;
                    }
                    if (mBatchSize == 0) {
                        batchSize = N;
                    } else {
                        batchSize = mBatchSize;
                    }

                    Collections.sort(apps,
                            new ApplicationManager.ShortcutNameComparator(packageManager, mLabelCache));

                for (int j=0; i<N && j<batchSize; j++) {
                    // This builds the icon bitmaps.
                    mBgAllAppsList.add(new ApplicationInfo(packageManager, apps.get(i),
                            mIconCache, mLabelCache));
                    i++;
                }
                Log.v("ApplicationManager", ">>>>chenrui>>>>loadAllAppsByBatch: step 2");
                mAllAppsLoaded = true; //the application has been  loaded. 

               }
          }
               Log.v("ApplicationManager", ">>>>chenrui>>>>loadAllAppsByBatch: step 3 + all apps list size is:" + mBgAllAppsList.size());
   
    }

}
    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3; // uninstlled
        public static final int OP_UNAVAILABLE = 4; // external media unmounted


        public PackageUpdatedTask(int op, String[] packages) {
            mOp = op;
            mPackages = packages;
        }
        public void run() {
            final Context context = mApp;

            final String[] packages = mPackages;
            final int N = packages.length;
            switch (mOp) {
                case OP_ADD:
                    for (int i=0; i<N; i++) {
                        Log.d(TAG, "mAllAppsList.addPackage " + packages[i]);
                        mBgAllAppsList.addPackage(context, packages[i]);
                    }
                    break;
                case OP_UPDATE:
                    for (int i=0; i<N; i++) {
                        Log.d(TAG, "mAllAppsList.updatePackage " + packages[i]);
                        mBgAllAppsList.updatePackage(context, packages[i]);
                        HypervisorApplication app =
                                (HypervisorApplication) context.getApplicationContext();
                    }
                    break;
                case OP_REMOVE:
                case OP_UNAVAILABLE:
                    for (int i=0; i<N; i++) {
                        Log.d(TAG, "mAllAppsList.removePackage " + packages[i]);
                        mBgAllAppsList.removePackage(packages[i]);
                        HypervisorApplication app =
                                (HypervisorApplication) context.getApplicationContext();
                    }
                    break;
            }
            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> modified = null;
            final ArrayList<ApplicationInfo> removedApps = new ArrayList<ApplicationInfo>();

            if (mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<ApplicationInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if (mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<ApplicationInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if (mBgAllAppsList.removed.size() > 0) {
                removedApps.addAll(mBgAllAppsList.removed);
                mBgAllAppsList.removed.clear();
            }


            if (added != null) {
                final ArrayList<ApplicationInfo> addedFinal = added;
                mHandler.post(new Runnable() {
                    public void run() {
                    }
                });
            }
          if (modified != null) {
                final ArrayList<ApplicationInfo> modifiedFinal = modified;
                mHandler.post(new Runnable() {
                    public void run() {
                    }
                });
            }
            // If a package has been removed, or an app has been removed as a result of
            // an update (for example), make the removed callback.
            if (mOp == OP_REMOVE || !removedApps.isEmpty()) {
                final boolean permanent = (mOp == OP_REMOVE);
                final ArrayList<String> removedPackageNames =
                        new ArrayList<String>(Arrays.asList(packages));

                mHandler.post(new Runnable() {
                    public void run() {
                    }
                });
            }
        }
    }


   public static class ShortcutNameComparator implements Comparator<ResolveInfo> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, CharSequence> mLabelCache;
        ShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, CharSequence>();
            mCollator = Collator.getInstance();
        }
        ShortcutNameComparator(PackageManager pm, HashMap<Object, CharSequence> labelCache) {
            mPackageManager = pm;
            mLabelCache = labelCache;
            mCollator = Collator.getInstance();
        }
        public final int compare(ResolveInfo a, ResolveInfo b) {
            CharSequence labelA, labelB;
            ComponentName keyA = ApplicationManager.getComponentNameFromResolveInfo(a);
            ComponentName keyB = ApplicationManager.getComponentNameFromResolveInfo(b);
            if (mLabelCache.containsKey(keyA)) {
                labelA = mLabelCache.get(keyA);
            } else {
                labelA = a.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyA, labelA);
            }
            if (mLabelCache.containsKey(keyB)) {
                labelB = mLabelCache.get(keyB);
            } else {
                labelB = b.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyB, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };


}
