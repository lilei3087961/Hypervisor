package com.android.hypervisor;

public interface Config {

    public static final short MESSAGE_ANDROID_HEART_BEAT       = 101; 
    public static final short MESSAGE_ANDROID_READY            = 100;
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
    public static final short MESSAGE_ANDROID_APP_RESUME       = 113;
    public static final short MESSAGE_ANDROID_APP_START_FAIL   = 114;
    public static final short MESSAGE_ANDROID_APP_BACK         = 115; 
    public static final short MESSAGE_ANDROID_APP_EXIT         = 116; 
    public static final short MESSAGE_LINUX_SWITCH_ANDROID_FG  = 117; 
    public static final short MESSAGE_LINUX_SWITCH_ANDROID_BG  = 118;
    public static final short MESSAGE_ANDROID_REQUEST_AUDIO    = 119; 
    public static final short MESSAGE_LINUX_AUDIO_PERMIT       = 120; 
    public static final short MESSAGE_ANDROID_END_AUDIO       = 121; 
  
    public static final short MESSAGE_LINUX_ENABLEAUDIO        = 122; 
    public static final short MESSAGE_LINUX_RESUME_MEDIA_AUDIO = 123; 
    public static final short MESSAGE_ANDROID_MEDIA_AUDIO_RESUME_SUCCESS  = 124;
    public static final short MESSAGE_ANDROID_MEDIA_AUDIO_RESUME_FAIL     = 125;
    public static final short MESSAGE_LINUX_RESUME_MEDIA_VOLUME= 126;
    public static final short MESSAGE_LINUX_LOWER_MEDIA_VOLUME = 127;
    public static final short MESSAGE_ANDROID_UPDATE_LANGUAGE  = 128;
    public static final short MESSAGE_LINUX_SET_LANGUAGE       = 129;
    public static final short MESSAGE_LINUX_SET_TIME           = 130;
    public static final short MESSAGE_ANDROID_KEYCODE_HOME     = 131;
    public static final short MESSAGE_ANDROID_FACTORY_RESET     = 132;
    public static final short MESSAGE_ANDROID_APP_ERROR_EXIT     = 133;
}
