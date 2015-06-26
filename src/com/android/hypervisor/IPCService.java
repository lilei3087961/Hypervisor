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
import java.net.ConnectException;
import java.net.InetSocketAddress;
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

    private static final String TAG = "IPCService"; 

    private ServerThread mServerThread = null;

    private static boolean mServerRunning = false;
    private static boolean mServiceStarted = false;
    IPCSocketImpl  mIPCSocketImpl;
    private static Socket  mReadySocket;
    static int try_count = 0;
    static final int TRY_TIMES_LIMIT = 10;
    static final int TRY_NEXT_CONNECT_DELAY = 20*1000;
    public HypervisorApplication mApp;
    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    public static final Handler sWorker = new Handler(sWorkerThread.getLooper());

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

    //add by lilei begin
    void testSocket(){
    	Log.i(TAG, ">>lilei>>begin test socket");
    	IPCSocketImpl  ipcImpl = new IPCSocketImpl(mApp);
    	Log.i(TAG, ">>lilei>>test socket 1");
    	ipcImpl.androidAppBack();
    	Log.i(TAG, ">>lilei>>test socket 2");
    	ipcImpl.androidAppExit("a", "a");
    	Log.i(TAG, ">>lilei>>test socket 3");
    	ipcImpl.androidAppResume("b", "b");
    	Log.i(TAG, ">>lilei>>test socket 4");
    	ipcImpl.androidAppStartFail("c", "c");
    	Log.i(TAG, ">>lilei>>test socket 5");
    	ipcImpl.androidAppStartSuccess("d", "d");
    	Log.i(TAG, ">>lilei>>test socket 6");
    	ipcImpl.androidRemoveOne("e");
    	Log.i(TAG, ">>lilei>>test socket 7");
    	ipcImpl.androidReady();
    	Log.i(TAG, ">>lilei>>test socket 8");
    	ipcImpl.androidHeartBeat();
    	Log.i(TAG, ">>lilei>>test socket 9");
    	ipcImpl.androidRequstAudio();
    	Log.i(TAG, ">>lilei>>test socket 10");
    	ipcImpl.androidUpdateLanguage();
    	//ipcImpl.androidAppBasicInfo();
    	//String packageName = "com.android.camera";
    	//String className = "com.android.camera.Camera";
    	//ipcImpl.androidUpdateOne(packageName, className);
    	//ipcImpl.androidSendOneApp(packageName, className);
    	//ipcImpl.androidRemoveOne("a");
   }
   static Socket getLcReadySocket(){  //for long connect
       if(mReadySocket == null){
           Log.e(TAG, ">>lilei>>error! mReadySocket == null,please set first!!");
       }
       return mReadySocket;
   }
   /***
    * set long connect ReadySocket,and set long connect listed and send
    * android ready message to linux
    */
   void setLcReadySocket(){
       try{
           mReadySocket = new Socket(IPCSocketImpl.SERVER_HOST_IP, 
                   IPCSocketImpl.SERVER_HOST_PORT);  
//           mReadySocket = new Socket();
//           Log.v(TAG, ">>lilei>>setLcReadySocket>>> 111 SOCKET_TIME_OUT:"
//                   +IPCSocketImpl.SOCKET_TIME_OUT);
//           InetSocketAddress isa = new InetSocketAddress(IPCSocketImpl.SERVER_HOST_IP,
//                   IPCSocketImpl.SERVER_HOST_PORT);
//           mReadySocket.connect(isa,IPCSocketImpl.SOCKET_TIME_OUT);
           ForwardTask task=new ForwardTask(mReadySocket,mApp,true);
           ThreadPool.getInstance().addTask(task);
           Log.v(TAG, ">>lilei>>no set timeout setLcReadySocket>>> 222");
           mIPCSocketImpl.androidReady();
       }catch(ConnectException e){
           if(try_count<TRY_TIMES_LIMIT){
               Log.i(TAG, ">>lilei>>~~~~recall setLcReadySocket() times is:"+try_count);
               try_count++;
               //setReadySocket();
               sWorker.postDelayed(new Runnable(){
                   @Override
                   public void run() {
                       // TODO Auto-generated method stub
                       setLcReadySocket();
                   }
              }, TRY_NEXT_CONNECT_DELAY);
           }
       }catch(Exception e){
           Log.e(TAG, ">>lilei>>setLcReadySocket() error3:"+e.toString());
       }
       
   }
   /***
    * send short connect android ready message to linux
    * short connect must call socket.close()
    */
   void sendScAndroidReadyMsg(){
       try{
           Log.v(TAG, ">>lilei>>sendScAndroidReadyMsg>>> 111");
           mReadySocket = new Socket(IPCSocketImpl.SERVER_HOST_IP, 
                   IPCSocketImpl.SERVER_HOST_PORT);  
           mReadySocket.close();
           Log.v(TAG, ">>lilei>>sendScAndroidReadyMsg>>> 222");
           mIPCSocketImpl.androidReady();
       }catch(Exception e){
           if(try_count<TRY_TIMES_LIMIT){
               Log.i(TAG, ">>lilei>>~~~~recall sendAndroidReadyMsg() times is:"+try_count
                       +" error:"+e.toString());
               try_count++;
               sWorker.postDelayed(new Runnable(){
                   @Override
                   public void run() {
                       // TODO Auto-generated method stub
                       sendScAndroidReadyMsg();
                   }
              }, TRY_NEXT_CONNECT_DELAY);
           }
       }
   }
   //add by lilei end
   private class ServerThread extends Thread {

        ServerSocket mServerSocket;
        @Override
        public void run() {

           try{
               mServerSocket  = new  ServerSocket(4700);
    
               ThreadPool pool=ThreadPool.getInstance();
               Log.v(TAG, ">>lilei>>serverThread>>>run");
               //add by lilei begin
               mIPCSocketImpl = new IPCSocketImpl(mApp);
               try{
                   if(IPCSocketImpl.SINGLE_CONNECTION){
                       Log.v(TAG, ">>lilei>>serverThread>>>run 111");
                       setLcReadySocket(); //init mReadySocket,and send message
                   }else{
                       sendScAndroidReadyMsg();
                       Log.v(TAG, ">>lilei>>serverThread>>>run 444");
                   }
               } catch (Exception e) {
                   Log.e(TAG, ">>lilei>>serverThread 111 error:"+e.toString());
                   e.printStackTrace();
               }
               //add by lilei end
               while(true){  //if SINGLE_CONNECTION is true,below code may not useful
                   Log.v(TAG, ">>lilei>>serverThread>>wait for connect...");
                   Socket socket= mServerSocket.accept();
                   Log.v(TAG, ">>lilei>>serverThread>>get a connection!~~~");
                   ForwardTask task=new ForwardTask(socket,mApp);
                   pool.addTask(task);
               }
           } catch (IOException e) {
               Log.e(TAG, ">>lilei>>serverThread error:"+e.toString());
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

