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


import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SplashScreen extends Activity {
    private long splashDelay = 1500;
    private static Handler SplashHandler;
    
    @SuppressLint({ "HandlerLeak", "NewApi" })
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        SplashHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
    	        TimerTask task = new TimerTask() {
    	            @Override
    	            public void run() {
    	            	if(!CommonHandler.ServiceRunning)	{
    	            		Intent myIntent = new Intent(SplashScreen.this, STService.class);
    	            		startService(myIntent);
    	            	}

    	                finish();
    	                Intent MainMenuIntent  = new Intent().setClass(SplashScreen.this, MainMenu.class);
    	                startActivity(MainMenuIntent);
    	            }
    	        };

    	        Timer timer = new Timer();
    	        timer.schedule(task, splashDelay);
            }
        };

		CommonHandler.InitDB(this);		
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
			new LoadWorker().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
		} else {
			new LoadWorker().execute(this);
		}
    }

    private static class LoadWorker extends AsyncTask<Object, Object, Object>	{

		@Override
		protected Object doInBackground(Object... params) {
			if(!CommonHandler.ServiceRunning)	{
				/*	Inicializar Banco de Dados	*/
				CommonHandler.dbman.LoginDB("SplashScreen");
				CommonHandler.LoadPreferences();
				
				/*	Inicializar Listas e Callbacks	*/
				CommonHandler.LoadLists();
				CommonHandler.InitLists();
				CommonHandler.InitCallbacks();
				
				CommonHandler.dbman.LogoutDB("SplashScreen");
			}
			SplashHandler.sendEmptyMessage(0);
			return null;
		}
    	
    }
}
