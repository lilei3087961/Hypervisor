package com.android.hypervisor;

import android.util.Log;

public class AndroidChangeTask extends Task {
	//final HypervisorApplication mApp;
	static final String TAG = "AndroidChangeTask";
	short mConfigState = -1;
	String mPackageName;
	String mClassName;
	public IPCSocketImpl  mIPCSocketImpl; 
	
	public AndroidChangeTask(IPCSocketImpl  iPCSocketImpl,short configState,
			String packageName,String className){
		mIPCSocketImpl = iPCSocketImpl;
		mConfigState = configState;
		mPackageName = packageName;
		mClassName = className;
	}
	
	@Override
	public Task[] taskCore() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean useDb() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean needExecuteImmediate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String info() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void run() {
		switch(mConfigState){
			case Config.MESSAGE_ANDROID_ADDONE:
				Log.i(TAG, ">>lilei>>MESSAGE_ANDROID_ADDONE");
				mIPCSocketImpl.androidAddOne(mPackageName, mClassName);
				break;
			case Config.MESSAGE_ANDROID_UPDATEONE:
				Log.i(TAG, ">>lilei>>MESSAGE_ANDROID_UPDATEONE");
				mIPCSocketImpl.androidUpdateOne(mPackageName, mClassName);
				break;
			case Config.MESSAGE_ANDROID_REMOVEONE:
				Log.i(TAG, ">>lilei>>MESSAGE_ANDROID_REMOVEONE");
				mIPCSocketImpl.androidRemoveOne(mPackageName);
				break;
			case Config.MESSAGE_ANDROID_UPDATE_LANGUAGE:
				Log.i(TAG, ">>lilei>>MESSAGE_ANDROID_UPDATE_LANGUAGE");
				mIPCSocketImpl.androidUpdateLanguage();
				break;
			case Config.MESSAGE_ANDROID_KEYCODE_HOME:
				Log.i(TAG, ">>lilei>>MESSAGE_ANDROID_KEYCODE_HOME");
				mIPCSocketImpl.androidKeycodeHome();
		}
	}

	
}
