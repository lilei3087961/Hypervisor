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
   private DataInputStream dis;
   private DataOutputStream dos;

   final static int PORT=9999; //socket port

   private final HypervisorApplication mContext;
   

   public IPCSocketImpl(HypervisorApplication context){

      mContext = context; 

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

  /** start packagename's application*/
  public void startApplication(String packagename){




 }
  /**
   * close dis, dos
   */ 
  public void close(){
  
   try{
       if(dis!=null)
           dis.close();
       if(dos!=null)
          dos.close();
       dis=null;
       dos=null;
      }catch (IOException e) {

      }


  }



}
