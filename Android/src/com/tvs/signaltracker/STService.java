package com.tvs.signaltracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class STService extends Service{
	
	private static final String TAG = "SignalTracker Service";
	private static final int NOTIFICATION = R.string.app_name;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
