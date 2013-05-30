package com.tvs.signaltracker;
/**
 * @author Lucas Teske
 *  _   _                       ____  _                   _ 
 * | | | |_   _ _ __   ___ _ __/ ___|(_) __ _ _ __   __ _| |
 * | |_| | | | | '_ \ / _ \ '__\___ \| |/ _` | '_ \ / _` | |
 * |  _  | |_| | |_) |  __/ |   ___) | | (_| | | | | (_| | |
 * |_| |_|\__, | .__/ \___|_|  |____/|_|\__, |_| |_|\__,_|_|
 *       |___/|_|                      |___/               
 *  ____  _                   _ _____               _             
 * / ___|(_) __ _ _ __   __ _| |_   _| __ __ _  ___| | _____ _ __ 
 * \___ \| |/ _` | '_ \ / _` | | | || '__/ _` |/ __| |/ / _ \ '__|
 * _ __) | | (_| | | | | (_| | | | || | | (_| | (__|   <  __/ |   
 * |____/|_|\__, |_| |_|\__,_|_| |_||_|  \__,_|\___|_|\_\___|_|   
 *         |___/                                                 
 * 
 * Created by: Lucas Teske from Teske Virtual System
 * Package: com.tvs.signaltracker
 */

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