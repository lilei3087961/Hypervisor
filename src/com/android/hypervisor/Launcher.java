
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 */

package com.android.hypervisor;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.hypervisor.R;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default launcher application.
 */
public final class Launcher extends Activity{
    static final String TAG = "Launcher";

    private boolean isfirstResume = true; 
    private HypervisorApplication mApp;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

     Log.v(TAG, ">>>>>chenrui>>>laucher onCreate >>.");

     Intent i = new Intent(this, IPCService.class);
     startService(i);


}


    @Override
    protected void onResume() {
        super.onResume();
        if(mApp == null)
        	mApp = (HypervisorApplication)getApplication();
        if(isfirstResume){
              isfirstResume = false;
              return; 
        }else{
//        	  mApp.getModel().getAllAppInfo().doChangeInTask(
//        			Config.MESSAGE_ANDROID_KEYCODE_HOME, null, null);
            //here, we will deal with : back key and home key , if onResume is not called by the first time, it will send message to Linux and switch to Linux OS
              Log.v(TAG, ">>>>chenrui>>>>not first resume, we should send message to Linux OS and exit android"); 

        }


    }



    @Override
    public void onBackPressed() {

         Log.v(TAG, ">>>>chenrui>>>back >>");

    }

/*
private List<String> getHomes() { 
        List<String> names = new ArrayList<String>(); 
        PackageManager packageManager = this.getPackageManager(); 
         
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME); 
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, 
              PackageManager.MATCH_DEFAULT_ONLY); 
        for(ResolveInfo ri : resolveInfo){ 
           names.add(ri.activityInfo.packageName); 
           Log.i("zhangyinfu PinyinIME.java", "packageName =" + ri.activityInfo.packageName);
        } 
        return names;
    }


 public boolean isHome(){
        ActivityManager mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE); 
        List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        List<String> strs = getHomes();
        if(strs != null && strs.size() > 0){
            return strs.contains(rti.get(0).topActivity.getPackageName());
        }else{
            return false;
        }
    }

*/


}
































