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


import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends Activity {
	
	//	Elementos visuais
	private Button	saveButton,cancelButton, facebookLogin, senddata;
	private SeekBar minDistance,minTime,lightmodeTime;
	private CheckBox WifiSend;
	private TextView minDistanceLabel, minTimeLabel, lightmodeTimeLabel, facebookName, signals, towers;
	private Spinner serviceMode;
	
	//	Variáveis temporárias
	private int v_minDistance, v_minTime, v_lightmodeTime;
	private Boolean resetFacebook;
	public static Boolean backtoSettings = false;
	
	//	Tasks
	private AsyncTask<List<TowerObject>, Integer, Long> stt;
	private AsyncTask<List<SignalObject>, Double, Long> sst;
	private boolean towersent, signalsent;
	private static Handler SettingsHandler = new Handler();
	private boolean RUN = false;
	private Runnable StartService = new Runnable()	{

		@Override
		public void run() {
			CommonHandler.KillService = false;
			Intent myIntent = new Intent(Settings.this, STService.class);
			startService(myIntent);
			CommonHandler.ServiceRunning = RUN;	
		}
		
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.settings_scroll);
			
			ScrollView scrollview = (ScrollView) findViewById(R.id.settings_scrollview);
			
			LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(R.layout.settings, scrollview);
			LinearLayout generalSettings = (LinearLayout) findViewById(R.id.generalSettingsView);
			LinearLayout facebookSettings = (LinearLayout) findViewById(R.id.facebookSettingsView);
			LinearLayout saveddataSettings = (LinearLayout) findViewById(R.id.saveddataSettingsView);
			
			inflater.inflate(R.layout.settings_general, generalSettings, true);
			inflater.inflate(R.layout.settings_facebook, facebookSettings);
			inflater.inflate(R.layout.settings_saveddata, saveddataSettings);
			
			saveButton			=	(Button) findViewById(R.id.settings_saveBtn);
			cancelButton		=	(Button) findViewById(R.id.settings_cancelBtn);
			facebookLogin		=	(Button) findViewById(R.id.settings_fblogin);
			senddata			=	(Button) findViewById(R.id.settings_senddata);

			minDistance			=	(SeekBar) findViewById(R.id.settings_minDistance);
			minTime				=	(SeekBar) findViewById(R.id.settings_minTime);
			lightmodeTime		=	(SeekBar) findViewById(R.id.settings_lightmodeTime);

			minDistanceLabel	=	(TextView)	findViewById(R.id.settings_minDistanceLabel);
			minTimeLabel		=	(TextView)	findViewById(R.id.settings_minTimeLabel);
			lightmodeTimeLabel	=	(TextView)	findViewById(R.id.settings_lightmodeTimeLabel);
			facebookName		=	(TextView)	findViewById(R.id.settings_facename);
			signals				=	(TextView)	findViewById(R.id.settings_signals);
			towers				=	(TextView)	findViewById(R.id.settings_towers);
			
			WifiSend			=	(CheckBox)	findViewById(R.id.settings_wifiSend);
			
			serviceMode			=	(Spinner)	findViewById(R.id.settings_serviceMode);
	        if(!CommonHandler.ServiceRunning)	{
				CommonHandler.LoadLists();
	        }
			senddata.setOnClickListener(new View.OnClickListener() {
				
				@SuppressLint("NewApi")
				@SuppressWarnings("unchecked")
				@Override
				public void onClick(View v) {
					RUN = CommonHandler.ServiceRunning;
					CommonHandler.KillService = true;
					Intent myIntent = new Intent(Settings.this, STService.class);
					stopService(myIntent);
					senddata.setText(getResources().getString(R.string.sending));
					senddata.setClickable(false);
					towersent = false;
					signalsent = false;
					if(CommonHandler.Signals != null)
						if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
							sst = new SendSignalTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, CommonHandler.Signals);
						} else {
							sst = new SendSignalTask().execute(CommonHandler.Signals);
						}
					
					if(CommonHandler.Towers != null)
						if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
							stt = new SendTowerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, CommonHandler.Towers);
						} else {
							stt = new SendTowerTask().execute(CommonHandler.Towers);
						}

				}
			});
			facebookLogin.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(!CommonHandler.FacebookUID.contentEquals("0"))	{
						facebookName.setText(getResources().getString(R.string.willdisconnectonsave));
						resetFacebook = true;
						facebookLogin.setClickable(false);
						facebookLogin.setText(getResources().getString(R.string.willdisconnectonsave));
					}else{
						backtoSettings = true;
						Intent intent = new Intent(v.getContext(), FacebookActivity.class);
						startActivity(intent);
					}
				}
			});
			saveButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Toast.makeText(Settings.this, getResources().getString(R.string.stoppingst), Toast.LENGTH_SHORT).show();
					CommonHandler.ServiceRunning = false;
					CommonHandler.KillService = true;
					Toast.makeText(Settings.this, getResources().getString(R.string.savingpref), Toast.LENGTH_SHORT).show();
					CommonHandler.dbman.LoginDB("Settings");
					if(resetFacebook)	{
						CommonHandler.FacebookEmail = "";
						CommonHandler.FacebookUID = "0";
						CommonHandler.FacebookName = "Anonymous";
						CommonHandler.dbman.setPreference("fbid", "0");
						CommonHandler.dbman.setPreference("fbname","Anonymous");
						CommonHandler.dbman.setPreference("fbemail", "");
					}

					CommonHandler.WifiSend = WifiSend.isChecked();
					if(WifiSend.isChecked())
						CommonHandler.dbman.setPreference("wifisend", "True");
					else
						CommonHandler.dbman.setPreference("wifisend", "False");
					
					CommonHandler.MinimumDistance = v_minDistance;
					CommonHandler.MinimumTime = v_minTime;
					CommonHandler.LightModeDelayTime = v_lightmodeTime;
					CommonHandler.ServiceMode = (short) (serviceMode.getSelectedItemPosition()+1);
					
					CommonHandler.dbman.setPreference("servicemode", Integer.toString(CommonHandler.ServiceMode));
					CommonHandler.dbman.setPreference("mindistance", Integer.toString(v_minDistance));
					CommonHandler.dbman.setPreference("mintime", Integer.toString(v_minTime));
					CommonHandler.dbman.setPreference("lightmodedelay", Integer.toString(v_lightmodeTime));
										
					CommonHandler.dbman.LogoutDB("Settings");
					CommonHandler.LoadPreferences();
					Toast.makeText(Settings.this, getResources().getString(R.string.prefsavedrestarting), Toast.LENGTH_SHORT).show();
            		Intent myIntent = new Intent(Settings.this, STService.class);
	            	PendingIntent pendingIntent = PendingIntent.getService(Settings.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)Settings.this.getSystemService(Context.ALARM_SERVICE);
	                Calendar calendar = Calendar.getInstance();
	                calendar.setTimeInMillis(System.currentTimeMillis());
	                calendar.add(Calendar.SECOND, 2);
	                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
					finish();
				}
			});
			cancelButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					finish();
				}
			});
			
			minDistance.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					v_minDistance = progress + 30;
					minDistanceLabel.setText(v_minDistance+" m");
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				
			});
			
			minTime.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					v_minTime = progress;
					minTimeLabel.setText(v_minTime+" ms");
					
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				
			});
			
			lightmodeTime.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					v_lightmodeTime = progress+2;
					lightmodeTimeLabel.setText(v_lightmodeTime+" m");
					
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				
			});
			if(CommonHandler.Signals != null)
				signals.setText((CommonHandler.Signals.size()*CommonHandler.MinimumDistance/1000.0f)+" km");
			else
				signals.setText("0.0 km");
				
			if(CommonHandler.Towers != null)
				towers.setText((CommonHandler.Towers.size()+" "+getResources().getString(R.string.towers)));
			else
				towers.setText("0 "+getResources().getString(R.string.towers));
			
			if(CommonHandler.ServiceMode > 0 & CommonHandler.ServiceMode < 5)
				serviceMode.setSelection(CommonHandler.ServiceMode-1);

			if(CommonHandler.MinimumDistance-30 > 0)	{
				minDistance.setProgress(CommonHandler.MinimumDistance-30);
				minDistanceLabel.setText(CommonHandler.MinimumDistance+" m");
			}else{
				minDistance.setProgress(0);
				minDistanceLabel.setText("30 m");
			}
			minTime.setProgress(CommonHandler.MinimumTime);
			minTimeLabel.setText(CommonHandler.MinimumTime+" ms");
			
			if(CommonHandler.LightModeDelayTime-2 > 0)
				lightmodeTime.setProgress(CommonHandler.LightModeDelayTime-2);
			else
				lightmodeTime.setProgress(0);
			
			lightmodeTimeLabel.setText(CommonHandler.LightModeDelayTime+" m");

			if(!CommonHandler.FacebookUID.contentEquals("0"))	{
				facebookName.setText(getResources().getString(R.string.notconnected));
				facebookLogin.setText(getResources().getString(R.string.disconnectefacebook));
			}else{
				facebookName.setText(getResources().getString(R.string.notconnected));
				facebookLogin.setText(getResources().getString(R.string.fblogin));
			}
			WifiSend.setChecked(CommonHandler.WifiSend);
			resetFacebook		=	false;
	}
	private class SendSignalTask extends AsyncTask<List<SignalObject>, Double, Long> {
	     protected Long doInBackground(List<SignalObject>... objectlist) {
	         int count = objectlist[0].size();
	         int sent = 0;
	         for (int i = 0; i < count; i++) {
				SignalObject signal = objectlist[0].get(i);
	 			try {
					signal.state = 1;
					String jsondata = "{\"metodo\":\"addsinal\",\"uid\":\""+CommonHandler.FacebookUID+"\",\"op\":\""+CommonHandler.Operator+"\",\"lat\":"+String.valueOf(signal.latitude)+",\"lon\":"+String.valueOf(signal.longitude)+",\"dev\":\""+Build.DEVICE+"\",\"man\":\""+Build.MANUFACTURER+"\",\"model\":\""+Build.MODEL+"\",\"brand\":\""+Build.BRAND+"\",\"rel\":\""+Build.VERSION.RELEASE+"\",\"and\":\""+Build.ID+"\",\"sig\":"+String.valueOf(signal.signal)+"}";
					jsondata = TheUpCrypter.GenOData(jsondata);
					JSONObject out = Utils.getODataJSONfromURL(HSAPI.baseURL+"?odata="+URLEncoder.encode(jsondata, "UTF-8"));
					if(out != null)	{
						if(out.getString("result").indexOf("OK") > -1)	{
							signal.state = 2;
							CommonHandler.dbman.deleteSignal(signal.id);
						}else{
							Log.e("SignalTracker::SendSignal","Error: "+out.getString("result"));
							signal.state = 0;
						}
					}else{
						signal.state = 0;
						Log.e("SignalTracker::SendSignal","No Output");
					}
					if(signal.state != 2)
						CommonHandler.dbman.UpdateSignal(signal.latitude, signal.longitude, signal.signal, signal.state);
				} catch (Exception e) {
					Log.e("SignalTracker::SendSignal","Error: "+e.getMessage());
					signal.state = 0;
				}
	 			if(signal.state == 2)
	 				sent += 1;
	             publishProgress((double) ((count - sent)*CommonHandler.MinimumDistance)/1000.0);
	             // Escape early if cancel() is called
	             if (isCancelled()) break;
	         }
	         return (long) sent;
	     }
	     protected void onProgressUpdate(Double... progress) {
			signals.setText(progress[0]+" km");
	        // setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Long result) {
	         Toast.makeText(Settings.this, getResources().getString(R.string.senddata)+" ("+((result*CommonHandler.MinimumDistance)/1000.0f)+" km)",Toast.LENGTH_LONG).show();
	         signalsent = true;
	         if(towersent & signalsent)	{
	        	 senddata.setClickable(true);
	        	 senddata.setText(getResources().getString(R.string.datasent));
	        	 CommonHandler.LoadLists();
	        	 SettingsHandler.post(StartService);
	         }
	     }
	}
	 	private class SendTowerTask extends AsyncTask<List<TowerObject>, Integer, Long> {
		     protected Long doInBackground(List<TowerObject>... objectlist) {
		         int count = objectlist[0].size();
		         int sent = 0;
		         for (int i = 0; i < count; i++) {
					TowerObject tower = objectlist[0].get(i);
					try {
						tower.state = 1;
						String jsondata = "{\"metodo\":\"addtorre\",\"op\":\""+CommonHandler.Operator+"\",\"lat\":"+String.valueOf(tower.latitude)+",\"lon\":"+String.valueOf(tower.longitude)+", \"uid\":\""+CommonHandler.FacebookUID+"\"}";
						jsondata = TheUpCrypter.GenOData(jsondata);
						JSONObject out = Utils.getODataJSONfromURL(HSAPI.baseURL+"?odata="+URLEncoder.encode(jsondata, "UTF-8"));
						if(out != null)	{
							if(out.getString("result").indexOf("OK") > -1)	{
								tower.state = 2;
								CommonHandler.dbman.deleteTower(tower.id);
							}else{
								Log.e("SignalTracker::SendTower","Error: "+out.getString("result"));
								tower.state = 0;
							}
						}else{
							tower.state = 0;
							Log.e("SignalTracker::SendTower","No Output");
						}
						if(tower.state != 2)
							CommonHandler.dbman.UpdateTower(tower.latitude, tower.longitude, tower.state);
					} catch (Exception e) {
						Log.e("SignalTracker::SendTower","Error: "+e.getMessage());
						tower.state = 0;
					}
		 			if(tower.state == 2)
		 				sent += 1;
		             publishProgress(count - sent);
		             // Escape early if cancel() is called
		             if (isCancelled()) break;
		         }
		         return (long) sent;
		     }
	     protected void onProgressUpdate(Integer... progress) {
			towers.setText(progress[0]+" "+getResources().getString(R.string.towers));
	        // setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Long result) {
	         Toast.makeText(Settings.this, getResources().getString(R.string.datasent)+" ("+result+" "+getResources().getString(R.string.towers)+")", Toast.LENGTH_LONG).show();
	         towersent = true;
	         if(towersent & signalsent)	{
	        	 senddata.setClickable(true);
	        	 senddata.setText(getResources().getString(R.string.senddata));
	        	 CommonHandler.LoadLists();
	        	 SettingsHandler.post(StartService);
	         }
	     }
	}
	@Override
	public void onDestroy()	{
		super.onDestroy();
		
		if(stt != null)
			if(!stt.isCancelled())
				stt.cancel(true);
		if(sst != null)
			if(!sst.isCancelled())
				sst.cancel(true);
	}
	@Override
	public void onResume()	{
		super.onResume();
		
		if(CommonHandler.ServiceMode > 0 & CommonHandler.ServiceMode < 5)
			serviceMode.setSelection(CommonHandler.ServiceMode-1);
		
		if(CommonHandler.MinimumDistance-30 > 0)	{
			minDistance.setProgress(CommonHandler.MinimumDistance-30);
			minDistanceLabel.setText(CommonHandler.MinimumDistance+" m");
		}else{
			minDistance.setProgress(0);
			minDistanceLabel.setText("30 m");
		}
		minTime.setProgress(CommonHandler.MinimumTime);
		minTimeLabel.setText(CommonHandler.MinimumTime+" ms");
		
		if(CommonHandler.LightModeDelayTime-15 > 0)
			lightmodeTime.setProgress(CommonHandler.LightModeDelayTime-15);
		else
			lightmodeTime.setProgress(0);
		
		lightmodeTimeLabel.setText(CommonHandler.LightModeDelayTime+" m");

		if(!CommonHandler.FacebookUID.contentEquals("0"))	{
			facebookName.setText(" "+CommonHandler.FacebookName);
			facebookLogin.setText(getResources().getString(R.string.disconnectefacebook));
		}else{
			facebookName.setText(getResources().getString(R.string.notconnected));
			facebookLogin.setText(getResources().getString(R.string.fblogin));
		}
		WifiSend.setChecked(CommonHandler.WifiSend);
		resetFacebook		=	false;
	}
}
