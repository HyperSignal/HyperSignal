package com.tvs.signaltracker;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class SplashScreen extends Activity {
    private long splashDelay = 1500;
    private static Handler SplashHandler;
    
    @SuppressLint("HandlerLeak")
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
    	                finish();
    	                Intent MainMenuIntent  = new Intent().setClass(SplashScreen.this, MainMenu.class);
    	                startActivity(MainMenuIntent);
    	            }
    	        };

    	        Timer timer = new Timer();
    	        timer.schedule(task, splashDelay);
            }
        };
        new LoadWorker().execute(this);
    }

    private static class LoadWorker extends AsyncTask<Object, Object, Object>	{

		@Override
		protected Object doInBackground(Object... params) {
			/*	Inicializar Banco de Dados	*/
			CommonHandler.InitDB((Context) params[0]);

			CommonHandler.dbman.LoginDB("SplashScreen");
			CommonHandler.LoadPreferences();
			
			/*	Inicializar Listas e Callbacks	*/
			CommonHandler.LoadLists();
			CommonHandler.InitLists();
			CommonHandler.InitCallbacks();
			
			CommonHandler.dbman.LogoutDB("SplashScreen");
			CommonHandler.dbman.Close();
			SplashHandler.sendEmptyMessage(0);
			return null;
		}
    	
    }
}
