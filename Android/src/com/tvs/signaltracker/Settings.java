package com.tvs.signaltracker;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
	private Button	saveButton,cancelButton, facebookLogin;
	private SeekBar minDistance,minTime,lightmodeTime;
	private CheckBox WifiSend;
	private TextView minDistanceLabel, minTimeLabel, lightmodeTimeLabel, facebookName;
	private Spinner serviceMode;
	
	//	Variáveis temporárias
	private int v_minDistance, v_minTime, v_lightmodeTime;
	private Boolean resetFacebook;
	public static Boolean backtoSettings = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.settings_scroll);
			
			ScrollView scrollview = (ScrollView) findViewById(R.id.settings_scrollview);
			
			LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(R.layout.settings, scrollview);
			LinearLayout generalSettings = (LinearLayout) findViewById(R.id.generalSettingsView);
			LinearLayout facebookSettings = (LinearLayout) findViewById(R.id.facebookSettingsView);
			inflater.inflate(R.layout.settings_general, generalSettings, true);
			inflater.inflate(R.layout.settings_facebook, facebookSettings);
			
			saveButton			=	(Button) findViewById(R.id.settings_saveBtn);
			cancelButton		=	(Button) findViewById(R.id.settings_cancelBtn);
			facebookLogin		=	(Button) findViewById(R.id.settings_fblogin);

			minDistance			=	(SeekBar) findViewById(R.id.settings_minDistance);
			minTime				=	(SeekBar) findViewById(R.id.settings_minTime);
			lightmodeTime		=	(SeekBar) findViewById(R.id.settings_lightmodeTime);

			minDistanceLabel	=	(TextView)	findViewById(R.id.settings_minDistanceLabel);
			minTimeLabel		=	(TextView)	findViewById(R.id.settings_minTimeLabel);
			lightmodeTimeLabel	=	(TextView)	findViewById(R.id.settings_lightmodeTimeLabel);
			facebookName		=	(TextView)	findViewById(R.id.settings_facename);
			
			WifiSend			=	(CheckBox)	findViewById(R.id.settings_wifiSend);
			
			serviceMode			=	(Spinner)	findViewById(R.id.settings_serviceMode);

			facebookLogin.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(!CommonHandler.FacebookUID.contentEquals("0"))	{
						facebookName.setText("Será desconectado ao salvar.");
						resetFacebook = true;
						facebookLogin.setClickable(false);
						facebookLogin.setText("Será desconectado ao salvar.");
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
					Toast.makeText(Settings.this, "Parando serviço o SignalTracker", Toast.LENGTH_SHORT).show();
					CommonHandler.ServiceRunning = false;
					CommonHandler.KillService = true;
					Toast.makeText(Settings.this, "Salvando preferências", Toast.LENGTH_SHORT).show();
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
					CommonHandler.ServiceMode = (short) (serviceMode.getSelectedItemId()+1);

					CommonHandler.dbman.setPreference("mindistance", Integer.toString(v_minDistance));
					CommonHandler.dbman.setPreference("mintime", Integer.toString(v_minTime));
					CommonHandler.dbman.setPreference("lightmodedelay", Integer.toString(v_lightmodeTime));
										
					CommonHandler.dbman.LogoutDB("Settings");
					CommonHandler.LoadPreferences();
					Toast.makeText(Settings.this, "Preferências salvas. Iniciando serviço novamente.", Toast.LENGTH_SHORT).show();
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
					v_lightmodeTime = progress+15;
					lightmodeTimeLabel.setText(v_lightmodeTime+" m");
					
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				
			});
			if(CommonHandler.ServiceMode > 0)
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
				facebookLogin.setText("Desconectar do Facebook");
			}else{
				facebookName.setText(" "+"Não conectado");
				facebookLogin.setText("Login no Facebook");
			}
			WifiSend.setChecked(CommonHandler.WifiSend);
			resetFacebook		=	false;
	}
	@Override
	public void onResume()	{
		super.onResume();
		if(CommonHandler.ServiceMode > 0)
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
			facebookLogin.setText("Desconectar do Facebook");
		}else{
			facebookName.setText(" "+"Não conectado");
			facebookLogin.setText("Login no Facebook");
		}
		WifiSend.setChecked(CommonHandler.WifiSend);
		resetFacebook		=	false;
	}
}
