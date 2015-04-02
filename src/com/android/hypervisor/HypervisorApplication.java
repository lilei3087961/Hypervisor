package com.android.hypervisor;

import android.app.Application;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;

import com.android.hypervisor.R;

import java.lang.ref.WeakReference;

import android.util.Log; //chenrui


/**  Hypervisor application */
public class HypervisorApplication extends Application {

    public ApplicationManager mModel;
    public IconCache mIconCache;

   
    //following are Linux OS (domain) ip address and port; 
    public String    linuxIp ; 
    public String    linuxPort; 

    private long  currentPlaytime;

    @Override
    public void onCreate() {
        super.onCreate();
   
        Log.v("HypervisorApp", ">>>>>chenrui>>>>create");
   
        mIconCache = new IconCache(this);
        mModel = new ApplicationManager(this, mIconCache);
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mModel, filter);
/*        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        registerReceiver(mModel, filter);

*/
        mModel.startLoader(); 



   }


    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        Log.v("chenrui", ">>>>>terminal>>>chenru>");
        if(mModel != null)
           unregisterReceiver(mModel);
        IPCService.clear();

    }


    IconCache getIconCache() {
        return mIconCache;
    }

    ApplicationManager getModel() {
        return mModel;
    }

    public void setPlaytime(long time){

        currentPlaytime = time; 
    }
    
    public long getPlaytime(){

        return currentPlaytime;
    }
    public void cleartime(){

        currentPlaytime = -1 ; 
    }
}
