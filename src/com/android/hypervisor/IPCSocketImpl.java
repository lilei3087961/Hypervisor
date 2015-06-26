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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
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
import java.io.ByteArrayOutputStream;

import android.media.AudioSystem;

//if packagename is chinese word, we should use : iconv.h  to fit the font;  please reference code in some files  in *.c  on this platform.... 

/**
  *  if IPC use socket, at first, Linux OS need to send its own ip address to Android OS because Android OS can not get Linux OS's ip on Android
  *  platform.But Linux OS is Domain 0 and  can use  libvirt  to get all virtual machine 's  ip .  
  */


//m start -n com.android.music/com.android.music.MediaPlaybackActivity --chenrui  adb shell and run this command...
public class IPCSocketImpl  extends  IPCImpl{


   private String       TAG = "IPCSocketImpl";  
   private Socket       mClientSocket;
   static Socket       mClientInstance; //for no close Socket Instance
   //public static final boolean USE_JSON = true;
   //public static final boolean USE_TWO_WAY = false; //is two way long connect

   static final String KEY_MESSAGE_TYPE = "messageType";
   static final String KEY_PACKAGE_NAME = "packageName";
   static final String KEY_CLASS_NAME = "activityName";
   static final String KEY_CLASS_TITLE = "title";
   static final String KEY_BYTEMAP = "byteIcon";
   static final String KEY_ALL_APPS = "allApps";
   static final String KEY_TIME_IN_MILLS = "timeInMillis";
   static final String KEY_LANGUAGE = "language";
   static final String KEY_AREA = "area";
   
   private DataInputStream dis;
   private DataOutputStream dos;
   
   final static boolean USE_END_CHAR = true;
   final static byte END_CHAR = 0X08;
   public static final boolean SINGLE_CONNECTION = true;  //is single long connect
   final static boolean ADD_BEGIN_END_CHAR = true; //whether add READ_BEGIN READ_END when send json
   final static boolean READ_BEGIN_END_CHAR = false; //whether read with READ_BEGIN READ_END when read json
   final static byte READ_BEGIN = (byte) 0xff;  //convert to Decimal is -1
   final static byte READ_END = (byte) 0xfe;    //convert to Decimal is -2
   final static String SERVER_HOST_IP = "192.168.30.229";  //localhost 本地 121.40.35.89
   final static int SERVER_HOST_PORT = 8001; //socket port
   final static int SOCKET_TIME_OUT = 5*60*1000; //20 minutes
   final static int HEART_BEAT_DELAY = SOCKET_TIME_OUT-10*1000;//10 seconds before SOCKET_TIME_OUT
   private final HypervisorApplication mContext;
   final PackageManager mPackageManager;
   List<ResolveInfo> apps = null;
   ComponentName mComponentName;
   
   public IPCSocketImpl(HypervisorApplication context){

      mContext = context; 
      mPackageManager = context.getPackageManager();
   }
   
   /**
    *  if ServerSocket get socket from  Linux OS, we need to transfer this socket to this class, so 
    *  we can all using socket to send information to Linux OS. 
    */
   public void setSocketImpl(Socket mSocket, DataInputStream dis, DataOutputStream dos){
     
      mClientSocket = mSocket; 
      this.dis = dis; 
      this.dos = dos; 
   }
   
   Socket getClientInstance(){
       if(mClientInstance == null){
           mClientInstance = IPCService.getLcReadySocket();
       }
       return mClientInstance;
   }
   /**
    * get Linux OS ip and port
    */
   public void getLinuxOSIp(){


   }

    /**android os will connect to Linux Os
    * if connect fails, return false; if connect sucess,  return true;
    */
   public boolean connectLinux(String ip, int port){
         try {
              mClientSocket=new Socket(ip, port);
              dis=new DataInputStream(new BufferedInputStream(mClientSocket.getInputStream()));
              dos=new DataOutputStream(new BufferedOutputStream(mClientSocket.getOutputStream()));

         }catch (UnknownHostException e) {
                        Log.i(TAG, "NetWorker connect() ..."+e.toString());
                        e.printStackTrace();
                        mClientSocket = null;
                        dis = null;
                        return false;
         } catch (IOException e) {
                        Log.i(TAG, "NetWorker connect() ..."+e.toString());
                        e.printStackTrace();
                        mClientSocket = null;
                        dos = null;
                        return false;
        }

        return true;

  }



/**
     * Checks whether any audio stream is active.
     *
     * @return int if any stream tracks are active.
     * ret: -1, there is no audio; 
     *      
     */
    public int  isAudioStreamActive() {

        if(AudioSystem.isStreamActive(AudioSystem.STREAM_MUSIC, 0))
            return AudioSystem.STREAM_MUSIC; 
        else if(AudioSystem.isStreamActive(AudioSystem.STREAM_SYSTEM, 0))
            return AudioSystem.STREAM_SYSTEM;
        else if(AudioSystem.isStreamActive(AudioSystem.STREAM_RING, 0))
            return AudioSystem.STREAM_RING; 
        else if(AudioSystem.isStreamActive(AudioSystem.STREAM_ALARM, 0))
            return AudioSystem.STREAM_ALARM; 
        else if(AudioSystem.isStreamActive(AudioSystem.STREAM_NOTIFICATION, 0))
            return AudioSystem.STREAM_NOTIFICATION; 
        else if(AudioSystem.isStreamActive(AudioSystem.STREAM_DTMF, 0))
            return AudioSystem.STREAM_DTMF;
        else if(AudioSystem.isStreamActive(AudioSystem.STREAM_TTS, 0))
            return AudioSystem.STREAM_TTS;
        else
            return -1; 
    }

    /**
    * pause or stop audio steam
    *  stop: STREAM_SYSTEM, STREAM_RING,STREAM_ALARM, STREAM_NOTIFICATION
    *  pause: STREAM_MUSIC, STREAM_DTMF, STREAM_TTS
    *  this function need MediaPlayer.java to add one receiver to receive this message and deal with this message....
    */
    public void pauseStopAudioStream(int streamtype){

        String action = null; 
        Intent intent = null; 

        switch(streamtype){
           case AudioSystem.STREAM_MUSIC: 
           case AudioSystem.STREAM_RING: 
                //will pause this ring or music, send intent to pause ring or music; MediaPlayer should add variable to record current playing time
                action = "hypervisor_pause";
                intent = new Intent(action);
                mContext.sendBroadcast(intent);    

                break; 
          case AudioSystem.STREAM_SYSTEM: 
          case AudioSystem.STREAM_ALARM:
          case AudioSystem.STREAM_NOTIFICATION: 
          case AudioSystem.STREAM_DTMF: 
          case AudioSystem.STREAM_TTS: 
              //will stop the voice, send intent to stop ring or music,
              //MediaPlayer.java should call stop and release.
               action = "hypervisor_stop";
               intent = new Intent(action);
               mContext.sendBroadcast(intent);  

               break;  




        }

    }

   /**
    * resume the audio stream, including music, video;
    * only : STREAM_MUSIC, STREAM_DTMF, STREAM_TTS 
    *  this function need MediaPlayer.java to add one receiver to receive this message and deal with this message.... 
    * 
    */
   public void resumeAudioStream(int streamtype){
       String action = null; 
       Intent intent = null; 

      switch(streamtype){
           case AudioSystem.STREAM_MUSIC:
           case AudioSystem.STREAM_RING:
                //will resume this ring or music, send intent to resume ring or music
               action = "hypervisor_resume";
               intent = new Intent(action);
               mContext.sendBroadcast(intent);

                break;
           default: 
               break;

        }
 

   }
  
   /**
    *  exit android system; we will send message to Linux OS and let Linux OS to call hypervisor interface and display Linux OS
    */
   public void  exitAndroid(){
	   	try{//add by lilei
            Socket client = new Socket("localhost", 8083);
            dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
            dos.writeShort(Config.MESSAGE_ANDROID_APP_EXIT);
            dos.flush();
	   	} catch (IOException e) {
                   e.printStackTrace();
	   	}



   }


   /**
     * initial all application title and   icon to Linux OS
     */
  public void  initialAllApplicationsToLinux(){



  }


   /**
    *  if the new application is installed, this function will be called and send information to  Linux OS
    *  the java char use  16 bit; so, we should pay attention to this
    */
   public void installApplication(String packagename){ //chenrui
        
        String title = null; 
        Bitmap appIcon = null; 

        if(mContext.getIconCache()!= null){
  /*
             appIcon = mContext.getIconCache().getComponentIcon(packagename);
             title = mContext.getIconCache().getApplicationTitle(packagename);
*/
        }
              

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        appIcon.compress(Bitmap.CompressFormat.PNG, 100, baos);
        short bitmapSize = (short)baos.size();
        byte[]bitmapbyte =  baos.toByteArray();
        //java char is 16 bit; but the C char is 8 bit; so, it should mul * 2; 
        char[ ]chars =  packagename.toCharArray(); 
        short namelength = (short)(chars.length * 2);

        char[] titlechar = title.toCharArray();
        short titlelength  = (short)(titlechar.length * 2);

 

        try{

          Socket client = new Socket("localhost", 8083);
          dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
          dos.writeShort(Config.MESSAGE_ANDROID_ADDONE);
          dos.writeShort(namelength);
          dos.writeShort(titlelength);
          dos.writeShort(bitmapSize);
          dos.writeChars(packagename);
          dos.writeChars(title);
          dos.write(bitmapbyte); 
        
          dos.flush();
   
          } catch (IOException e) {
               e.printStackTrace();
          }

   }

   /**
   * if the application is uninstalled ; the function will be called  and send information to Linux OS
   */
   public void uninstallApplication(String packagename){
        char[ ]chars =  packagename.toCharArray();
        short namelength = (short)(chars.length * 2);


        try{

          Socket client = new Socket("localhost", 8083);
          dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
          dos.writeShort(Config.MESSAGE_ANDROID_REMOVEONE);
          dos.writeShort(namelength);
          dos.writeChars(packagename);

          dos.flush();

          } catch (IOException e) {
               e.printStackTrace();
          }

   }

     /**
    * if the application is updated; the funciton will be called and send information to Linux OS
    */
   public void  updateApplication(String packagename){
        String title = null;
        Bitmap appIcon = null;

        if(mContext.getIconCache()!= null){
 /* 
            appIcon = mContext.getIconCache().etComponentIcon(packagename);
             title = mContext.getIconCache().getApplicationTitle(packagename);
*/
        }


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        appIcon.compress(Bitmap.CompressFormat.PNG, 100, baos);
        short bitmapSize = (short)baos.size();
        byte[]bitmapbyte =  baos.toByteArray();
        //java char is 16 bit; but the C char is 8 bit; so, it should mul * 2; 
        char[ ]chars =  packagename.toCharArray();
        short namelength = (short)(chars.length * 2);

        char[] titlechar = title.toCharArray();
        short titlelength  = (short)(titlechar.length * 2);

         try{

          Socket client = new Socket("localhost", 8083);
          dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
          dos.writeShort(Config.MESSAGE_ANDROID_UPDATEONE);
          dos.writeShort(namelength);
          dos.writeShort(titlelength);
          dos.writeShort(bitmapSize);
          dos.writeChars(packagename);
          dos.writeChars(title);
          dos.write(bitmapbyte);

          dos.flush();

          } catch (IOException e) {
               e.printStackTrace();
          }



  }
   /*add by lilei begin*/
   /**
    if Android started, this function will be called and android os can get message for LInux OS
     **/
   public void androidReady(){
	   Log.i(TAG,">>lilei>>send:android Ready");
	   sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_READY);
   }
   /**
   	if Android error,send message to LInux OS
    **/
  public void androidHeartBeat(){
	   Log.i(TAG,">>lilei>>send:android Heart Beat");
	   sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_HEART_BEAT);
  }
  /***
   * if android press home and and show laucher,send message to Linux OS
   */
  public void androidKeycodeHome(){
	  Log.i(TAG,">>lilei>>send:android keycode home");
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_KEYCODE_HOME);
  }
  /**
 	when receive linux os message MESSAGE_LINUX_GETALL
 	send all app info to linux OS
  **/
  public void androidAppBasicInfo(){
	   Log.i(TAG,">>lilei>>send:android app basic infos");
	   sendAllAndroidAppToLinux();
  }
  /***
   * XXXXXXremoved 废弃的方法
   * 
   * @param className
   */
  public void androidSendOneApp(String className){
	  //String packageName = className.substring(0, className.lastIndexOf("."));
	  Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
      mainIntent.setPackage("com.android.settings");
      //mainIntent.setClassName(mContext, className);
      apps = mPackageManager.queryIntentActivities(mainIntent, 0);
      Log.i(TAG,">>lilei>>~androidSendOneApp(.) apps.size():"+apps.size());
      if(apps.size()>0){
    	  //Log.i(TAG,">>lilei>>androidSendOneApp(.) apps(0) packageName:"+apps.get(0).activityInfo.packageName);
      }
	  //androidSendOneApp(packageName,className);
  }
   /***
    *  	when linux request one app info,send this message info to linux OS
    * @param packageName
    * @param className
    */
  public void androidSendOneApp(String packageName,String className){
       mComponentName =  new ComponentName(packageName,className);
       String title = null;
       Bitmap appIcon = null;
       if(mContext.getIconCache()!= null){
           appIcon = mContext.getIconCache().getComponentIcon(mComponentName);
           title = mContext.getIconCache().getApplicationTitle(mComponentName);
       }
       Log.i(TAG,">>lilei>>send:android send one app"
               +" packageName:"+packageName+" className:"+className
               +" title:"+title+" appIcon:"+appIcon);
	   sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_SENDONEAPP,packageName,
			   className,title,appIcon);
  }
  
  /***
   * ####### only for test transfer bytemap test
   * @param packageName
   * @param className
   */
  public void testAndroidSendOneApp(String packageName,String className){
      mComponentName =  new ComponentName(packageName,className);
      String title = null;
      Bitmap appIcon = null;
      if(mContext.getIconCache()!= null){
          appIcon = mContext.getIconCache().getComponentIcon(mComponentName);
          title = mContext.getIconCache().getApplicationTitle(mComponentName);
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      appIcon.compress(Bitmap.CompressFormat.PNG, 100, baos);
      short bitmapSize = (short)baos.size();
      byte[]bitmapbyte =  baos.toByteArray();
      Socket client = null;
      Log.i(TAG,">>lilei>>#use Base64 byteTest 11##testAndroidSendOneApp() android send one app"
              +" packageName:"+packageName+" className:"+className
              +" title:"+title+" appIcon:"+appIcon+" bitmapSize:"+bitmapSize);
      try{
          if(SINGLE_CONNECTION){
              client = getClientInstance();
          }else{
              client = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
          }
        dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        //dos.write(bitmapbyte);
        //dos.writeChars(bitmapbyte.toString());
        byte[] byteTest = new byte[]{'a','b'};
        //String str = "李";
        //dos.writeChars(byteTest.toString());
        String base64Str = Base64.encodeToString(byteTest,0,byteTest.length,Base64.NO_WRAP);
        Log.i(TAG,">>lilei>>~~testAndroidSendOneApp() byteTest.base64:"
                +base64Str+" bitmapbyte.length:"+byteTest.length);
        dos.writeChars("ab");
        dos.flush();
      }catch (IOException e) {
          Log.i(TAG,">>lilei>>testAndroidSendOneApp() error:"+e.toString());
          e.printStackTrace();
      }finally{
          close(client);
      }
  }
  /**
 	when install an app,send message to linux OS
   **/
  public void androidAddOne(String packageName,String className){
	   Log.i(TAG,">>lilei>>send:android add one app"
			   +" packageName:"+packageName+" className:"+className);
       mComponentName =  new ComponentName(packageName,className);
       String title = null;
       Bitmap appIcon = null;
       if(mContext.getIconCache()!= null){
           appIcon = mContext.getIconCache().getComponentIcon(mComponentName);
           title = mContext.getIconCache().getApplicationTitle(mComponentName);
       }
	   sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_ADDONE,packageName,
			   className,title,appIcon);
  }
  /**
 	when update one app send message to linux OS
   **/
  public void androidUpdateOne(String packageName,String className){
	   Log.i(TAG,">>lilei>>send:android update one app"
			   +" packageName:"+packageName+" className:"+className);
       mComponentName =  new ComponentName(packageName,className);
       String title = null;
       Bitmap appIcon = null;
       if(mContext.getIconCache()!= null){
           appIcon = mContext.getIconCache().getComponentIcon(mComponentName);
           title = mContext.getIconCache().getApplicationTitle(mComponentName);
       }
	   sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_UPDATEONE,packageName,
			   className,title,appIcon);
  }
  /**
	when remove one app send message to linux OS
   **/
  public void androidRemoveOne(String packageName){
	  Log.i(TAG,">>lilei>>send:android remove one app "
			  +" packageName:"+packageName);
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_REMOVEONE,packageName);
  }
   /** 
   * when app start success send message to linux OS
   * @param packageName
   * @param className
   */
  public void androidAppStartSuccess(String packageName,String className){
	  Log.i(TAG,">>lilei>>send:android app start success "
	  		+ "packageName:"+packageName +" className:"+className);
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_APP_START_SUCCESS,
			  packageName,className);
  }
  /**
   * when starting an app,the app is already started send message to linux OS
   * 
   * @param packageName
   * @param className
   */
  public void androidAppResume(String packageName,String className){
	  Log.i(TAG,">>lilei>>send:android app resume "
		  		+ "packageName:"+packageName +" className:"+className);
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_APP_RESUME,
			  packageName,className);
  }
  /***
   * when start app fail,send message to linux OS
   * @param packageName
   * @param className
   */
  public void androidAppStartFail(String packageName,String className){
	  Log.i(TAG,">>lilei>>send:android app start fail "
		  		+ "packageName:"+packageName +" className:"+className);
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_APP_START_FAIL,
			  packageName,className);
  }
  /***
   * when android app exit and should back to show launcher,send message to linux OS
   * to show linux OS screen 
   */
  public void androidAppBack(){
	  Log.i(TAG,">>lilei>>send:android app back ");
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_APP_BACK);
  }
  /***
   * when android app exit,send message to linux OS
   * @param packageName
   * @param className
   */
  public void androidAppExit(String packageName,String className){
	  Log.i(TAG,">>lilei>>send:android app exit "
			  + "packageName:"+packageName +" className:"+className);
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_APP_EXIT,
			  packageName,className);
  }
  /***
   * when android request audio,send message to linux OS
   */
  public void androidRequstAudio(){
	  Log.i(TAG,">>lilei>>send:android request audio ");
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_REQUEST_AUDIO);
  }
  /***
   * when android os change language,send message to linux OS
   * note:linux OS get this message should regain app infos 
   */
  public void androidUpdateLanguage(){
	  Log.i(TAG,">>lilei>>send:android update language ");
	  sendAndroidMessageToLinux(Config.MESSAGE_ANDROID_UPDATE_LANGUAGE);
  }
  
   /***
    * only send android state to linux os
    * 
    * @param configState
    */
   public void sendAndroidMessageToLinux(int configState){
       Socket client = null;
	   	try{
	   	    if(SINGLE_CONNECTION){
	   	        client = getClientInstance();
	   	    }else{
	   	        client = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
	   	    }
            dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        	try{
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(KEY_MESSAGE_TYPE,configState);
           	 	Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(1) jsonObj.toString:"+jsonObj.toString()
        			 +" toCharArray size:"+jsonObj.toString().toCharArray().length);
                //dos.writeChars(jsonObj.toString());
                doWriteChars(dos,jsonObj.toString());
        	}catch (JSONException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(1) error:"+e.toString());
     	 	}

            dos.flush();
	   	} catch (IOException e) {
	   	    Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(1) error:"+e.toString());
            e.printStackTrace();
	   	}finally{
	        close(client);
	    }
   }
   
   /**
    * send android state and package name to linux os
    *
    * @param configState
    * @param packageName
    */
   public void sendAndroidMessageToLinux(int configState,String packageName){
       Socket client = null;
       try{
           if(SINGLE_CONNECTION){
               client = getClientInstance();
           }else{
               client = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
           }
         dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));

    	 try{
        	 JSONObject jsonObj = new JSONObject();
        	 jsonObj.put(KEY_MESSAGE_TYPE,configState);
        	 jsonObj.put(KEY_PACKAGE_NAME,packageName);
        	 Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(2) jsonObj.toString:"+jsonObj.toString()
        			 +" toCharArray size:"+jsonObj.toString().toCharArray().length);
             //dos.writeChars(jsonObj.toString());
             doWriteChars(dos,jsonObj.toString());
    	 }catch (JSONException e) {
 			 // TODO Auto-generated catch block
 			 e.printStackTrace();
 			 Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(2) error:"+e.toString());
     	 }

         dos.flush();

       } catch (IOException e) {
    	   Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(2) error:"+e.toString());
           e.printStackTrace();
       }finally{
           close(client);
       }
   }
   
   /**
    * send android state,package name,class name to linux os
    * 
    * @param configState
    * @param packageName
    * @param className
    */
   public void sendAndroidMessageToLinux(int configState,String packageName,
		   String className){
       if(!isValidActivity(packageName,className)){
           return;
       }
       char[] packageChars =  packageName.toCharArray(); 
       short packageLength = (short)(packageChars.length * 2);
       
       char[] classChars = className.toCharArray();
       short classLength = (short)(classChars.length * 2);
       Socket client = null;
       try{
           if(SINGLE_CONNECTION){
               client = getClientInstance();
           }else{
               client = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
           }
         dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
    	 try{
        	 JSONObject jsonObj = new JSONObject();
        	 jsonObj.put(KEY_MESSAGE_TYPE,configState);
        	 jsonObj.put(KEY_PACKAGE_NAME,packageName);
        	 jsonObj.put(KEY_CLASS_NAME,className);
        	 Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(3) jsonObj.toString:"+jsonObj.toString()
        			 +" toCharArray size:"+jsonObj.toString().toCharArray().length);
             //dos.writeChars(jsonObj.toString());
             doWriteChars(dos,jsonObj.toString());
    	 }catch (JSONException e) {
 			 // TODO Auto-generated catch block
 			 e.printStackTrace();
 			 Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(3) error:"+e.toString());
     	 }
  
         dos.flush();

       } catch (IOException e) {
           Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(3) error:"+e.toString());
           e.printStackTrace();
       }finally{
           close(client);
       }
   }
   
   /**
    * send android state,package name,class name,title and app icon to linux
    * 
    * @param configState
    * @param packageName
    * @param className
    * @param title
    * @param appIcon
    */
   public void sendAndroidMessageToLinux(int configState,String packageName,
		   String className,String title,Bitmap appIcon){
       //String title = null;
	   if(!isValidActivity(packageName,className)){
	       return;
	   }
	   Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(5) appIcon!=null?"
			   +(appIcon != null ? "true":"false")+" configState:"+configState
			   +" title byte length:"+title.getBytes().length);
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       appIcon.compress(Bitmap.CompressFormat.PNG, 100, baos);
       short bitmapSize = (short)baos.size();
       byte[]bitmapbyte =  baos.toByteArray();
       //java char is 16 bit; but the C char is 8 bit; so, it should mul * 2; 
       char[] packageChars =  packageName.toCharArray(); 
       short packageLength = (short)(packageChars.length * 2);
       
       char[] classChars = className.toCharArray();
       short classLength = (short)(classChars.length * 2);
       
       char[] titlechar = title.toCharArray();
       short titlelength  = (short)(titlechar.length * 2);
       Socket client = null;
       try{
           if(SINGLE_CONNECTION){
               client = getClientInstance();
           }else{
               client = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
           }
         dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));

    	 try{

        	 JSONObject jsonObj = new JSONObject();
        	 jsonObj.put(KEY_MESSAGE_TYPE,configState);
        	 jsonObj.put(KEY_PACKAGE_NAME,packageName);
        	 jsonObj.put(KEY_CLASS_NAME,className);
        	 jsonObj.put(KEY_CLASS_TITLE,Base64.encodeToString( 
        	         title.getBytes(),0,title.getBytes().length,Base64.NO_WRAP));
        	 //for test begin
        	 Log.i(TAG,">>lilei>>Base64 title is:"+Base64.encodeToString( 
                     title.getBytes(),0,title.getBytes().length,Base64.NO_WRAP)
                     +" Base64 title length is:"+Base64.encodeToString( 
                     title.getBytes(),0,title.getBytes().length,Base64.NO_WRAP).length());
        	 for(int i=0;i<title.getBytes().length;i++){
        	     Log.i(TAG,">>lilei>>title byte["+i+"]:"+title.getBytes()[i]);
        	 }
        	 //for test end
        	 jsonObj.put(KEY_BYTEMAP,Base64.encodeToString(
        			 bitmapbyte,0,bitmapbyte.length,Base64.NO_WRAP));
        	 Log.i(TAG,">>lilei>>byteMap Base64.encode length is:"+
        	Base64.encodeToString(bitmapbyte,0,bitmapbyte.length,Base64.NO_WRAP).length()
        	+" bitmapbyte.length:"+bitmapbyte.length);
        	 
        	 Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(5) toCharArray size:"+
        	 jsonObj.toString().toCharArray().length+" timeNow:"+ForwardTask.getTimeNow());
        			 //+" jsonObj.toString:"+jsonObj.toString());
        	 
             //dos.writeChars(jsonObj.toString());
             doWriteChars(dos,jsonObj.toString());
    	 }catch (JSONException e) {
 			 // TODO Auto-generated catch block
 			 e.printStackTrace();
 			 Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(5) error:"+e.toString());
     	 }
       
         dos.flush();
  
      } catch (IOException e) {
         Log.i(TAG,">>lilei>>sendAndroidMessageToLinux(5) error:"+e.toString());
         e.printStackTrace();
      }finally{
    	 close(client);
      }
  }
   /***
    * send all android package and class info 
    */
   public void sendAllAndroidAppToLinux(){
	   AllAppsList mBgAllAppsList = mContext.mModel.getAllAppInfo();
	   final ArrayList<ApplicationInfo> list = 
			   (ArrayList<ApplicationInfo>)mContext.mModel.getAllAppInfo().data.clone();
	   List<HashMap<String,Short>> listSize = new ArrayList<HashMap<String,Short>>();
	   List<HashMap<String,String>> listName = new ArrayList<HashMap<String,String>>();
	   listSize.clear();
	   listName.clear();
	   HashMap<String,Short> mapSize = null;
	   HashMap<String,String> mapName = null;
	   Log.i(TAG,">>lilei>>send:send all app to linux,>>1>>  app size:"+list.size());
	   for(int i=0;i< list.size();i++){
		   if(list.get(i)!=null){
			   String packageName = list.get(i).componentName.getPackageName();
			   String className = list.get(i).componentName.getClassName();
			   Log.i(TAG,">>lilei>>packageName:"+packageName+" className:"+className);
		       char[] packageChars =  packageName.toCharArray();
		       short packageLength = (short)(packageChars.length * 2);
		       
		       char[] classChars = className.toCharArray();
		       short classLength = (short)(classChars.length * 2);
		       
			   mapSize = new HashMap<String,Short>();
			   mapName = new HashMap<String,String>();
			   mapSize.clear();
			   mapName.clear();
			   
		       mapSize.put("packageLength", packageLength);
		       mapSize.put("classLength", classLength);
		       listSize.add(mapSize);
		       
		       mapName.put("packageName", packageName);
		       mapName.put("className", className);
		       listName.add(mapName);
		       
		   }else{
			   Log.i(TAG,">>lilei>>send:send all app to linux,error index:"+i);
		   }
	   }
	   Socket client = null;
	   try{
             if(SINGLE_CONNECTION){
                 client = getClientInstance();
             }else{
                 client = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
             }
	         dos=new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));

	         Log.i(TAG,">>lilei>>send:send all app to linux,>>2>>  "
		         		+ "listName size:"+listName.size());

        	 try{
        		 JSONObject jsonObj = new JSONObject();
        		 JSONObject objApp;
	        	 jsonObj.put(KEY_MESSAGE_TYPE,Config.MESSAGE_ANDROID_APPBASICINFO);
	        	 JSONArray allApps = new JSONArray();
	        	 for(int i=0;i<listName.size();i++){
    	        	 String packageName = listName.get(i).get("packageName");
    	        	 String className = listName.get(i).get("className");
    	        	 objApp = new JSONObject();
    	        	 objApp.put(KEY_PACKAGE_NAME,packageName);
    	        	 objApp.put(KEY_CLASS_NAME,className);
    	        	 allApps.put(objApp);
	        	 }
	        	 jsonObj.put(KEY_ALL_APPS,allApps);
            	 Log.i(TAG,">>lilei>> jsonObj.toString:"+jsonObj.toString());
            	 Log.i(TAG,">>lilei>> jsonObj.toCharArray size:"+jsonObj.toString().toCharArray().length);
	        	 //dos.writeChars(jsonObj.toString());
            	 doWriteChars(dos,jsonObj.toString());
        	 }catch (JSONException e) {
     			 // TODO Auto-generated catch block
     			 e.printStackTrace();
     			 Log.i(TAG,">>lilei>>send all app error:"+e.toString());
     		}
	 
	         dos.flush();
	         
         } catch (IOException e) {
        	 Log.i(TAG,">>lilei>>sendAllAndroidAppToLinux() error:"+e.toString());
              e.printStackTrace();
         }finally{
        	 close(client);
         }
   }
   public void doWriteChars(DataOutputStream dos,String str){
       try{
           if(SINGLE_CONNECTION && ADD_BEGIN_END_CHAR){
               byte[] bytes= str.getBytes();
               int length = bytes.length;
               byte[] newBytes = new byte[length+2];
               System.arraycopy(bytes, 0, newBytes, 1, length);
               newBytes[0] = READ_BEGIN;
               newBytes[length+1] = READ_END;
               dos.write(newBytes);
           }else{
               dos.writeChars(str);
           }
       }catch(IOException e){
           Log.i(TAG,">>lilei>> dowriteChars error:"+e.toString());
       }
   }

   public boolean isValidActivity(String packageName,String className){
       Intent intent = new Intent();
       intent.setClassName(packageName, className);

       ResolveInfo component =mContext.getPackageManager().resolveActivity(intent, 0);
       if(component != null) {  
           //Log.i(TAG,"valid Activity() 1111 component:"+component);
           return true;
       }else{
           Log.i(TAG,">>lilei>>error!! invalid Activity for packageName:"+packageName
                   +" className:"+className);
           return false;
       }
   }
   /*add by lilei end*/
  /** start packagename's application*/
  public void startApplication(String packagename){




  }
  /**
   * close dis, dos
   */ 
  public void close(Socket socket){
  
   try{
       if(SINGLE_CONNECTION){
           Log.i(TAG,">>lilei>># no close socket !!!!!"
                   +" timeNow:"+ForwardTask.getTimeNow());
           return;
       }
       Log.i(TAG,">>lilei>>close socket !!!!!");
       if(dis!=null)
           dis.close();
       if(dos!=null)
          dos.close();
       if(socket != null)
           socket.close();
       dis=null;
       dos=null;
       socket = null;
      }catch (IOException e) {
          Log.e(TAG, ">>lilei>>close error e:"+e.toString());
      }


  }



}
