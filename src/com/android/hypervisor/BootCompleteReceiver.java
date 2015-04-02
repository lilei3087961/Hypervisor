/*
 * Copyright (C) 2007 The Android Open Source Project
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
 */

package com.android.hypervisor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log; //chenrui
import android.app.SearchManager;
import android.content.IntentFilter;

/**
 * Boot completed receiver. 
 *
 */
public class BootCompleteReceiver  extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.v("BootCompleteReceiver", ">>>>>>chenrui"); 
        // make sure the app icon is removed every time the device boots.
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {

             Log.v("BootCompleteReceiver",">>>>chenrui>>>>boot complete");
/*             HypervisorApplication mApp = (HypervisorApplication)(context.getApplication()); 
             mApp.mIconCache = new IconCache(context);
             mApp.mModel     = new ApplicationManager(context, mIconCache);
             IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
             filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
             filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
             filter.addDataScheme("package");
             registerReceiver(mApp.mModel, filter);
             filter = new IntentFilter();
             filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
             filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
             filter.addAction(Intent.ACTION_LOCALE_CHANGED);
             filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
             registerReceiver(mApp.mModel, filter);
             filter = new IntentFilter();
             filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
             registerReceiver(mApp.mModel, filter);
             filter = new IntentFilter();
             filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
             registerReceiver(mApp.mModel, filter);

             filter = new IntentFilter(Intent.ACTION_TIME_TICK);
             registerReceiver(mApp.mModel, filter);

*/
             IPCService.clear();

 
              Log.v("BootCompleteReceiver", ">>>>>>chenrui>>.will start IPCservice");
             Intent i = new Intent(context, IPCService.class);
             context.startService(i);

        }
    }
}
