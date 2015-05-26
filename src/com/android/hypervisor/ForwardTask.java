
package com.android.hypervisor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.internal.os.storage.ExternalStorageFormatter;

import android.media.AudioSystem;
import android.os.SystemClock;
import android.util.Log;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.hardware.input.InputManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;


public class ForwardTask extends Task{

          static HashMap<String, Socket> map=new HashMap<String, Socket>();
          private static final String TAG = "ForwardTask"; 
          Socket socket;
          DataInputStream dis = null;
          DataOutputStream dos = null;
          BufferedReader br = null;
          private boolean onWork=true;
          Intent[] mIntents;
          Intent mIntent;
          ComponentName mComponentName;
          String line = "";
          StringBuilder sbLine;
          private boolean mSingleConnection = false;
          public final HypervisorApplication mApp;
          public IPCSocketImpl  ipcImpl; 
          

          public ForwardTask(Socket socket, HypervisorApplication  app){

               this.mApp   = app;

               //here, we use socket ipc ,if we use other ipc , then we should change another icp implement class object
               this.socket = socket;
               

               try {
                   dis=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                   br = new BufferedReader(new InputStreamReader(dis));
                   dos=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                } catch (IOException e) {
                        e.printStackTrace();
                }
               this.ipcImpl = new IPCSocketImpl(app);
               //ipcImpl.setSocketImpl(socket, dis, dos);  // dis may be close
        }
        //add by lilei begin
        public ForwardTask(Socket socket, HypervisorApplication  app,boolean isSingleConnection){
              this(socket,app);
              mSingleConnection = isSingleConnection;
              
         }
        void testSocket(IPCSocketImpl ipcImpl){
        	Log.i(TAG, ">>lilei>>begin test socket");
        	ipcImpl.androidReady();
        }
          
        //add by lilei end
        
        @Override
        public Task[] taskCore() throws Exception {
                return null;
        }

         @Override
        protected boolean useDb() {
                return false;
        }

      @Override
        protected boolean needExecuteImmediate() {
                return false;
        }

        @Override
        public String info() {
                return null;
        }



        private void setWorkState(boolean state){
                onWork=state;
        }

         public void run() {
             Log.w(TAG, ">>lilei>>ForwardTask.run 000 mSingleConnection:"+mSingleConnection);
             if(mSingleConnection){
                 while(onWork){
                     try{
                         receiveMsgSingle();
                         Thread.sleep(500);
                     }catch(Exception e){
                         e.printStackTrace();
                         Log.i(TAG, ">>lilei>>ForwardTask.run 111 error trace:"+
                         Log.getStackTraceString(e));
                         break;
                     }
                 }
             }else{
                 if(onWork){
                     try{
                         receiveMsg();
                     }catch(Exception e){
                         e.printStackTrace();
                         Log.i(TAG, ">>lilei>>ForwardTask.run 222 error trace:"+
                         Log.getStackTraceString(e));
                     }
                 }
                 try{
                     Log.w(TAG, ">>lilei>>~~run socket.close()");
                     if(socket!=null)
                             socket.close();
                       if(dis!=null)
                              dis.close();
                       if(dos!=null)
                              dos.close();
                     socket=null;
                     dis=null;
                     dos=null;
                 }catch (IOException e) {
                     Log.i(TAG, ">>lilei>>ForwardTask.run 33 error:"+e.toString());
                 }
             }

        }

        public void receiveMsgSingle() throws IOException {
            //char[] buffer = new char[1024];
            byte[] buf = new byte[1024];
            byte[] bufMsg = null;
            int index = -1;
            boolean readBegin = false;
            boolean readEnd = false;
            int len = -1;
            
           while((len = dis.read(buf)) != -1){
                Log.w(TAG, ">>lilei>>~~get a new buffer! len is:"+len
                        +" buf.length:"+buf.length);
                if(!readBegin)  //if not find Start identifier init length
                    index = -1;
                for(int i=0;i<len;i++){

                    //int ascii = (int)buf[i];
                    if(buf[i] == IPCSocketImpl.READ_BEGIN){
                        Log.w(TAG, ">>lilei>>###~~receiveMsgSingle get 0xff index i is:"+i
                                +" byte is:"+buf[i]);
                        readBegin = true;
                        readEnd = false;
                        bufMsg = new byte[1024];
                    }else if(buf[i] == IPCSocketImpl.READ_END){
                        Log.w(TAG, ">>lilei>>~~receiveMsgSingle get 0xfe index i is:"+i
                                +" byte is:"+buf[i]);
                        readBegin = false;
                        readEnd = true;
                    }
                    //Log.w(TAG, ">>lilei>>~~receiveMsgSingle (int)buf["+i+"]="+(int)buf[i]);

                    if(readBegin && buf[i] != IPCSocketImpl.READ_BEGIN){
                        //Log.w(TAG, ">>lilei>>~~receiveMsgSingle index:"+index);
                        bufMsg[++index] = buf[i];
                    }else if(readEnd){  //
                        int length = index + 1;
                        index = -1;     
                        //char[] arr = bytesToChars(bufMsg,length);
                        byte[] buftmp = new byte[length];
                        System.arraycopy(bufMsg, 0, buftmp, 0, length);
                        String msg = new String(buftmp,"UTF-8");
                        Log.w(TAG, ">>lilei>>111~~receiveMsgSingle buffer string is:"+msg
                               +" byte length:"+length);
                        doReceiveMsg(msg);
                    }
                }

            } //*/
            //Log.w(TAG, ">>lilei>>~~before read(buffer)");
 /*           while((len = br.read(buffer)) != -1){
                Log.w(TAG, ">>lilei>>~~get a new buffer! len is:"+len);
                sbLine = new StringBuilder();
                for(int i=0;i<len;i++){
                    if((int)buffer[i] ==-1){
                        Log.w(TAG, ">>lilei>>~~receiveMsgSingle get 0xff");
                    }else if((int)buffer[i] == -2){
                        Log.w(TAG, ">>lilei>>~~receiveMsgSingle get 0xfe");
                    }
                   Log.w(TAG, ">>lilei>>~~receiveMsgSingle buffer["+i+"]:"+String.valueOf(buffer[i]).trim()
                           +" >>ascii is:"+(int)buffer[i]);
                   sbLine.append(String.valueOf(buffer[i]).trim());
                }
                Log.w(TAG, ">>lilei>>~~receiveMsgSingle buffer string is:"+sbLine.toString());
                //ipcImpl.testAndroidSendOneApp("a", "b");//for test
                doReceiveMsg(sbLine.toString().trim());
            } //*/
                   
        }
        public void receiveMsg() throws IOException {
 			Log.w(TAG, ">>lilei>>~~receiveMsg 1111");
 			while((line = br.readLine()) != null){
 					Log.d(TAG, ">>lilei>>json~~ receiveMsg line.trim:"+line.trim()
 					        +" timeNow:"+getTimeNow());
 	                doReceiveMsg(line.trim());
 			}
            Log.w(TAG, ">>lilei>>receiveMsg 222 ");
        }
 		//add by lilei begin
        void doReceiveMsg(String message){
            JSONObject jsonObj = null;
            int requestType = -1;
            try {
                jsonObj = new JSONObject(message);
                requestType = jsonObj.getInt(IPCSocketImpl.KEY_MESSAGE_TYPE);
            }catch (JSONException e) {
                Log.e(TAG, ">>lilei>>doReceiveMsg error:"+e.toString());
            }
            switch (requestType) {
                case Config.MESSAGE_LINUX_GETALL:
                    //should send all application size--the applicatoin should have main and launch activity..
                    ipcImpl.androidAppBasicInfo();
                        break;
                case Config.MESSAGE_LINUX_GETONEAPP:
                    linuxGetOneApp(jsonObj);
                      //when receive this message, we should send one app information...
                        break;
                case Config.MESSAGE_LINUX_APP_START:
                    linuxAppStart(jsonObj);
                        break;
                case Config.MESSAGE_LINUX_SET_TIME:
                    //setDateTime(2014,1,1,1,1,1); //for test
                    linuxSetTime(jsonObj);
                        break;
                case Config.MESSAGE_LINUX_SET_LANGUAGE:
                    //setLanguage(Locale.ENGLISH);  //for test
                    linuxSetLanguage(jsonObj);
                        break;
                case Config.MESSAGE_ANDROID_FACTORY_RESET:
                    FactoryReset(false);
                        break;
                case Config.MESSAGE_LINUX_MEMORYCLEAN:
                        break;
                case Config.MESSAGE_LINUX_SEND_KEY:
               //./base/core/java/android/hardware/input/InputManager.java : public boolean injectInputEvent(InputEvent event, int mode)
                    InputManager im = (InputManager) (mApp.getSystemService(Context.INPUT_SERVICE));
                      
                        break;
                case Config.MESSAGE_LINUX_SWITCH_ANDROID_FG:
                        break; 
                case Config.MESSAGE_LINUX_SWITCH_ANDROID_BG:
                        break; 
                case Config.MESSAGE_LINUX_AUDIO_PERMIT:
                        break;
                case Config.MESSAGE_ANDROID_END_AUDIO:
                        break; 
                case Config.MESSAGE_LINUX_ENABLEAUDIO: 
                        break;  

            }
        }
        void linuxGetOneApp(JSONObject jsonObj){
    		try{
        		String packageName = jsonObj.getString(IPCSocketImpl.KEY_PACKAGE_NAME);
        		String className = jsonObj.getString(IPCSocketImpl.KEY_CLASS_NAME);
        		ipcImpl.androidSendOneApp(packageName,className);
        		//ipcImpl.testAndroidSendOneApp(packageName, className);//test bytemap
    		}catch (JSONException e1) {
					Log.e(TAG, ">>lilei>>linuxGetOneApp() error:"+e1.toString());
		    }
        }
        void linuxAppStart(JSONObject jsonObj){
    		try{
        		String packageName = jsonObj.getString(IPCSocketImpl.KEY_PACKAGE_NAME);
        		String className = jsonObj.getString(IPCSocketImpl.KEY_CLASS_NAME);
        		startActivity(packageName,className);	
    		}catch (JSONException e1) {
				Log.e(TAG, ">>lilei>>linuxAppStart() error:"+e1.toString());
		    }

        }
        void linuxSetTime(JSONObject jsonObj){
    		try{
        		long timeInMills = jsonObj.getLong(IPCSocketImpl.KEY_TIME_IN_MILLS);
        		setDateTime(timeInMills);
    		}catch (JSONException e1) {
				Log.e(TAG, ">>lilei>>linuxSetTime() error:"+e1.toString());
		    }
        }
        void linuxSetLanguage(JSONObject jsonObj){
    		try{
        		String language = jsonObj.getString(IPCSocketImpl.KEY_LANGUAGE);
        		String area = jsonObj.getString(IPCSocketImpl.KEY_AREA);
        		setLanguage(language,area);
    		}catch (JSONException e1) {
				Log.e(TAG, ">>lilei>>linuxSetLanguage() error:"+e1.toString());
		    }

        }
        
        ///utils
        void startActivity(String packageName,String className){
 			mComponentName = new ComponentName(packageName,className);
 			mIntent = new Intent(Intent.ACTION_MAIN);
 			mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
 			mIntent.setComponent(mComponentName);
 			mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
 			mIntents = new Intent[]{mIntent};
 			if(isRunning(mApp,packageName)){
 				ipcImpl.androidAppResume(packageName, className);
 			}
 			try {
 				mApp.startActivities(mIntents);
 			}catch (ActivityNotFoundException e) {
 				Log.d(TAG, ">>lilei>>startActivity fail !!!");
 				ipcImpl.androidAppStartFail(packageName, className);
 				return;
 			}
 			Log.d(TAG, ">>lilei>>startActivity success!");
 			ipcImpl.androidAppStartSuccess(packageName, className);
 		}
        boolean isRunning(Context context,String packageName){
    	    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    	    List<RunningAppProcessInfo> infos = am.getRunningAppProcesses();
    	    for(RunningAppProcessInfo rapi : infos){
    	        if(rapi.processName.equals(packageName)){
    	        	Log.d(TAG, ">>lilei>>packageName:"+packageName+" is Running!");
    	            return true;
    	        }
    	    }
    	    Log.d(TAG, ">>lilei>>packageName:"+packageName+" is not Running!");
    	    return false;
        }
        
        void setDateTime(int year,int month,int day,int hour,
        	  int minute,int second){
            Calendar c = Calendar.getInstance();  
      
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month-1);
            c.set(Calendar.DAY_OF_MONTH, day);  
            c.set(Calendar.HOUR_OF_DAY, hour);  
            c.set(Calendar.MINUTE, minute);  
            c.set(Calendar.SECOND, second);
            Log.d(TAG, ">>lilei>>setDateTime "+year+"-"+month+"-"+day
            		+" "+hour+":"+minute+":"+second);
            setDateTime(c.getTimeInMillis());
        }
        public static long getTimeNow(){
            Calendar c = Calendar.getInstance();
            long now = c.getTimeInMillis();
            return now;
        }
        void setDateTime(long timemills){
        	Calendar c = Calendar.getInstance();
      	  	c.setTimeInMillis(timemills);
      	  	Log.d(TAG, ">>lilei>>setDateTime 111  "+c.get(Calendar.YEAR)+"-"
      	  			+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DAY_OF_MONTH)
      		+" "+c.get(Calendar.HOUR_OF_DAY)+":"+
      		c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND)
      		+" InMillis is:"+c.getTimeInMillis());
            
        	long when = timemills;  
            
            if (when / 1000 < Integer.MAX_VALUE) {
                SystemClock.setCurrentTimeMillis(when);
            }
            c = Calendar.getInstance();
            long now = c.getTimeInMillis();
            Log.d(TAG, ">>lilei>>setDateTime>>set tm="+when + ", now tm="+now
            		+" "+c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"
            		+c.get(Calendar.DAY_OF_MONTH)+" "+c.get(Calendar.HOUR_OF_DAY)
            		+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND));  
      
            if(now - when > 1000)
            	Log.d(TAG, ">>lilei>>setDateTime failed to set Date.");   
        }
        
        void setLanguage(String strLocale){
        	String language = strLocale.split("_")[0];
        	String area = strLocale.split("_")[1];
        	setLanguage(language,area);
        }
        void setLanguage(String language,String area){
        	Locale locale = new Locale(language,area);
        	setLanguage(locale);
        }
        void setLanguage(Locale locale){
        	try {
        		Log.d(TAG, ">>lilei>>setLanguage:"+locale.getDisplayName());
                Object objIActMag, objActMagNative;  
                Class clzIActMag = Class.forName("android.app.IActivityManager");  
                Class clzActMagNative = Class.forName("android.app.ActivityManagerNative");  
                Method mtdActMagNative$getDefault = clzActMagNative.getDeclaredMethod("getDefault");  
                // IActivityManager iActMag = ActivityManagerNative.getDefault();  
                objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);  
                // Configuration config = iActMag.getConfiguration();  
                Method mtdIActMag$getConfiguration = clzIActMag.getDeclaredMethod("getConfiguration");  
                Configuration config = (Configuration) mtdIActMag$getConfiguration.invoke(objIActMag);  
                config.locale = locale;
                // iActMag.updateConfiguration(config);  
                // 此处需要声明权限:android.permission.CHANGE_CONFIGURATION  
                // 会重新调用 onCreate();
                Class[] clzParams = { Configuration.class }; 
                Method mtdIActMag$updateConfiguration = clzIActMag.getDeclaredMethod(
                        "updateConfiguration", clzParams);  
                mtdIActMag$updateConfiguration.invoke(objIActMag, config);  
            }catch (Exception e){
            	Log.d(TAG, ">>lilei>>setLanguage error:"+e.toString());
                e.printStackTrace();  
            }
        }
        void FactoryReset(boolean eraseSdCard){
        	if (eraseSdCard) {
        		Log.d(TAG, ">>lilei>>FactoryReset 11 eraseSdCard");
                Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
                intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                mApp.startService(intent);
            } else {
            	Log.d(TAG, ">>lilei>>FactoryReset 22 ");
            	mApp.sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                // Intent handling is asynchronous -- assume it will happen soon.
            }
        }
        /***
         * convert Specified length of bytes to char array
         * @param bytes
         * @param length
         * @return
         */
        private char[] bytesToChars(byte[] bytes,int length) {
            byte[] buftmp = new byte[length];
            System.arraycopy(bytes, 0, buftmp, 0, length);
            for(int i=0;i<buftmp.length;i++){
                Log.i(TAG, ">>lilei>>buftmp["+i+"]:"+buftmp[i]+" ascii is:"+(int)buftmp[i]);
            }
            Charset cs = Charset.forName ("UTF-8");
            ByteBuffer bb = ByteBuffer.allocate (buftmp.length);
            bb.put(buftmp);
            bb.flip();
            CharBuffer cb = cs.decode(bb);
            for(int i=0;i<cb.array().length;i++){
                Log.i(TAG, ">>lilei>>arr["+i+"]:"+cb.array()[i]+" ascii is:"+(int)cb.array()[i]);
            }
            return cb.array(); //cb.array() char array
        }
        //add by lilei end



}

