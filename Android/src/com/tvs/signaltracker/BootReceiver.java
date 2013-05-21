package com.tvs.signaltracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver  {

    //private PendingIntent pendingIntent;
	@Override
	public void onReceive(Context context, Intent intent) {
		//if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) 	  {
		/*
			CommonHandler.InitDB(context);		
			CommonHandler.dbman.LoginDB("BootReceiver");
			CommonHandler.LoadPreferences();
			
			CommonHandler.LoadLists();
			CommonHandler.InitLists();
			CommonHandler.InitCallbacks();
			
			CommonHandler.dbman.LogoutDB("BootReceiver");
			CommonHandler.ServiceRunning = false;
	    	Intent myIntent = new Intent(context, STService.class);
	    	pendingIntent = PendingIntent.getService(context, 0, myIntent, 0);
	    	AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	        Calendar calendar = Calendar.getInstance();
	        calendar.setTimeInMillis(System.currentTimeMillis());
	        calendar.add(Calendar.SECOND, 10);
	        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
	        */
		//}
	}

}