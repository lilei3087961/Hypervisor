/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2011, 2013 The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log; 

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.UnknownHostException;
/**
 *
 */
public class IPCService extends Service {

    private final String TAG = "PICService"; 

    private ServerThread mServerThread = null;

    private static boolean mServerRunning = false;
    private static boolean mServiceStarted = false; 
  
    public HypervisorApplication mApp; 
    // Used for setting FLAG_ACTIVITY_NO_USER_ACTION when
    // creating an intent.

    @Override
    public void onCreate() {
        // Get Phone count
    }

    /**
     * get IPC Service state: 0, service not started; 1, service started but not run server; 2, server started and service started; 
     */
    public static int getServerState(){
         int ret = 0; 

         if(mServiceStarted == false) return 0; 
         if(mServiceStarted == true && mServerRunning == false) ret = 1; 
         if(mServiceStarted == true && mServerRunning == true)  ret = 2; 

         return ret; 
    }
    /**
     * clear IPC state 
     */
    public static void clear(){
        mServiceStarted = false;
        mServerRunning  = false; 
    }
    @Override
    public void onStart(Intent intent, int startId) {

        Log.v("IPCService", ">>chenrui>>>>>onStart>>>");
        // onStart() method can be passed a null intent
        // TODO: replace onStart() with onStartCommand()
        if (intent == null) {
            return;
        }
        mServiceStarted = true; 

        mApp = (HypervisorApplication)getApplication();
        Log.v("IPCService", ">>>>>chenrui>>>>will wait for ApplicationManager load"); 
        while(ApplicationManager.getLoadStatus() != true){//if the applicaton information has not been got using query,then we wait ...

             try {
                Thread.sleep(500);  //500ms
            } catch (InterruptedException unused) {
            }

        }
         Log.v("IPCService", ">>>>.chenrui>>>load application end");
         //start server socket thread...
         if (mServerThread == null) {
            mServerThread = new ServerThread();
            mServerThread.start();
            mServerRunning = true;
        }


    }


    @Override
    public void onDestroy() {
          if (mServerThread != null) {
             mServerThread.shutdown();
             mServerThread = null;
             mServerRunning = false;
          }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


   private class ServerThread extends Thread {

        ServerSocket mServerSocket;
        @Override
        public void run() {

           try{
           mServerSocket  = new  ServerSocket(4700);

           ThreadPool pool=ThreadPool.getInstance();
           Log.v("IPCService", ">>>>chenrui>>>>serverThread>>>run");

           while(true){
		Socket socket= mServerSocket.accept();

	        ForwardTask task=new ForwardTask(socket, mApp);
		pool.addTask(task);
	   }
           } catch (IOException e) {
	       e.printStackTrace();
	   }
            

        }
        public void shutdown() {
                if (mServerSocket != null) {
                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                    }
                    mServerSocket = null;
                }
        }


}

}
