package com.android.hypervisor;

public interface Config {

        public static final short MESSAGE_ANDROID_READY            = 100; 
        public static final short MESSAGE_ANDROID_HEART_BEAT       = 101; 
        public static final short MESSAGE_LINUX_MEMORYCLEAN        = 102; 
        public static final short MESSAGE_LINUX_SEND_KEY           = 103; 
        public static final short MESSAGE_LINUX_GETALL             = 104; 
        public static final short MESSAGE_ANDROID_APPBASICINFO     = 105;
        public static final short MESSAGE_LINUX_GETONEAPP          = 106;  
        public static final short MESSAGE_ANDROID_SENDONEAPP       = 107; 
        public static final short MESSAGE_ANDROID_ADDONE           = 108; 
        public static final short MESSAGE_ANDROID_UPDATEONE        = 109; 
        public static final short MESSAGE_ANDROID_REMOVEONE        = 110; 
	public static final short MESSAGE_LINUX_APP_START          = 111; 
        public static final short MESSAGE_ANDROID_APP_START_SUCCESS= 112; 
        public static final short MESSAGE_ANDROID_APP_BACK         = 113; 
        public static final short MESSAGE_ANDROID_APP_EXIT         = 114; 
        public static final short MESSAGE_LINUX_SWITCH_ANDROID_FG  = 115; 
        public static final short MESSAGE_LINUX_SWITCH_ANDROID_BG  = 116;
        public static final short MESSAGE_ANDROID_REQUEST_AUDIO    = 117; 
        public static final short MESSAGE_LINUX_AUDIO_PERMIT       = 118; 
        public static final short MESSAGE_LINUX_DISABLEAUDIO       = 119; 
        public static final short MESSAGE_LINUX_ENABLEAUDIO        = 120 ; 
}
