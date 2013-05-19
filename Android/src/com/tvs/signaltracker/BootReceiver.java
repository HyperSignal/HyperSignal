package com.tvs.signaltracker;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver  {

    private PendingIntent pendingIntent;
	@Override
	public void onReceive(Context context, Intent intent) {
		//if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) 	  {
	    	Intent myIntent = new Intent(context, STService.class);
	    	pendingIntent = PendingIntent.getService(context, 0, myIntent, 0);
	    	AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	        Calendar calendar = Calendar.getInstance();
	        calendar.setTimeInMillis(System.currentTimeMillis());
	        calendar.add(Calendar.SECOND, 10);
	        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
		//}
	}

}