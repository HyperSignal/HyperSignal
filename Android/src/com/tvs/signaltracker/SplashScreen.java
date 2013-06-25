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
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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
            	switch(msg.what)	{
            	case 0:
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
            		break;
            	case 1:
            		Toast.makeText(SplashScreen.this, SplashScreen.this.getResources().getString(R.string.downloadingops), Toast.LENGTH_LONG).show();
            		TextView msglbl = (TextView) findViewById(R.id.splash_screen_msg);
            		msglbl.setText(SplashScreen.this.getResources().getString(R.string.downloadingops));
            		msglbl.setTextSize(20);
            		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            			new DownLoadWorker().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
            		} else {
            			new DownLoadWorker().execute(this);
            		}
            		break;
            	case 2:
  	            	if(!CommonHandler.ServiceRunning)	{
	            		Intent myIntent = new Intent(SplashScreen.this, STService.class);
	            		startService(myIntent);
	            	}

	                finish();
	                Intent MainMenuIntent  = new Intent().setClass(SplashScreen.this, MainMenu.class);
	                startActivity(MainMenuIntent);
	                break;
            	}

            }
        };

		CommonHandler.InitDB(this);		
		CommonHandler.InitOperatorData(this);	/*	Inicializar Dados da Operadora	*/
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

				/*	Inicializar Lista de Operadora	*/

				CommonHandler.dbman.LogoutDB("SplashScreen");
			}
			if(CommonHandler.OperatorList == null)	{
				SplashHandler.sendEmptyMessage(1);
			}else{
				Operator x = CommonHandler.dbman.getOperator(CommonHandler.MCC, CommonHandler.MNC);
				if(x != null)
					CommonHandler.Operator = x.name;
				else
					CommonHandler.Operator = CommonHandler.MCC+""+CommonHandler.MNC;
				SplashHandler.sendEmptyMessage(0);
			}
			return null;
		}
    	
    }
    private static class DownLoadWorker extends AsyncTask<Object, Object, Object>	{

		@Override
		protected Object doInBackground(Object... params) {
			HSAPI.DownloadOperatorList();
			if(CommonHandler.OperatorList == null)	{
				Log.i("SignalTracker::DownloadWorker","Cannot download operator list");
				CommonHandler.OperatorList = CommonHandler.dbman.getOperatorList();
			}else{
				for(int i=0,len=CommonHandler.OperatorList.length;i<len;i++)	{
					Operator op = CommonHandler.OperatorList[i];
					CommonHandler.dbman.insertOperator(op.mcc, op.mnc, op.name, op.fullname);
				}
			}
			Operator x = CommonHandler.dbman.getOperator(CommonHandler.MCC, CommonHandler.MNC);
			if(x != null)
				CommonHandler.Operator = x.name;
			else
				CommonHandler.Operator = CommonHandler.MCC+""+CommonHandler.MNC;
			SplashHandler.sendEmptyMessage(2);
			return null;
		}
    	
    }
}
