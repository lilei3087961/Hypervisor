
package com.android.hypervisor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.media.AudioSystem;
import android.app.Service;

import android.hardware.input.InputManager;
import android.content.Context;


public class ForwardTask extends Task{

          static HashMap<String, Socket> map=new HashMap<String, Socket>();

          Socket socket;
          DataInputStream dis;
          DataOutputStream dos;
          private boolean onWork=true;

          public final HypervisorApplication mApp;
          public IPCSocketImpl  ipcImpl; 
          

          public ForwardTask(Socket socket, HypervisorApplication  app){

               this.mApp   = app;

               //here, we use socket ipc ,if we use other ipc , then we should change another icp implement class object
               this.socket = socket;
               

               try {
                   dis=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                   dos=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                } catch (IOException e) {
                        e.printStackTrace();
                }
               this.ipcImpl = new IPCSocketImpl(app);
               ipcImpl.setSocketImpl(socket, dis, dos); 
          
        }
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
                while(onWork){

                        try{
                                receiveMsg();
                        }catch(Exception e){
                                e.printStackTrace();
                                break;
                        }
                }

                try{
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

                }
        }


 public void receiveMsg() throws IOException {

                int requestType = dis.readInt();
                System.out.println("receiveMsg requestType="+requestType);
                switch (requestType) {
                case Config.MESSAGE_LINUX_GETALL:
                     //should send all application size--the applicatoin should have main and launch activity..
                   
                        break;
                case Config.MESSAGE_LINUX_GETONEAPP:
                      //when receive this message, we should send one app information...
                        break;
                case Config.MESSAGE_ANDROID_SENDONEAPP:
                        break; 

                case Config.MESSAGE_LINUX_APP_START:
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
                case Config.MESSAGE_LINUX_DISABLEAUDIO:
                        break; 
                case Config.MESSAGE_LINUX_ENABLEAUDIO: 
                        break;  
  
                }
        }




}

