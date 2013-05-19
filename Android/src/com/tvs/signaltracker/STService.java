package com.tvs.signaltracker;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.support.v4.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;
import android.location.GpsSatellite;
import android.location.GpsStatus;

public class STService extends Service{
	
	//	Notificações do serviço
	private static final String TAG = "SignalTracker";
	private static final int NOTIFICATION = R.string.app_name;

	//	Controle do Celular
	private TelephonyManager Tel;
	private MyPhoneStateListener MyListener;
	
	//	Controle do GPS
	private LocationManager mlocManager;	
	private LocationListener GPSLocListener;
	private LocationListener NetLocListener;
	private GPSStatusListener GPSs;
	private long mLastLocationTime;
	
	//	Internos do Serviço
	private boolean LocalRunning = false;
	private Timer	RunCheck;
	private TimerTask	RunCheckTask;
	private Handler	RunCheckHandler;
	
	private Timer	LightModeTimer;
	private TimerTask LightModeTask;
	private Handler LightModeHandler;
	
	@Override
	public IBinder onBind(Intent intent) {return null;}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Log.i("SignalTracker::STService", "Serviço Iniciado");
		if(RunCheck != null)	{
			RunCheck.cancel();
			RunCheck.purge();
		}
		RunCheck = new Timer();
		if(RunCheckTask != null)
			RunCheckTask.cancel();
		RunCheckTask	=	new TimerTask()	{

			@Override
			public void run() {
				RunCheckHandler.sendEmptyMessage(0);
			}
			
		};
		RunCheck.schedule(RunCheckTask, 1000);
	}
	@SuppressLint("HandlerLeak")
	@Override
	public void onCreate() {
		RunCheckHandler = new Handler()	{
			@Override
			public void handleMessage(Message msg)	{
				CheckRunning();
			}
		};
		LightModeHandler = new Handler()	{
			@Override
			public void handleMessage(Message msg)	{
				if(msg.what == 0)	{
					Tel						=	(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
					MyListener = new MyPhoneStateListener();
					Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
					CommonHandler.Operator	=	Utils.DoOperator(Tel.getNetworkOperatorName());
					
					mlocManager				=	(LocationManager) getSystemService(Context.LOCATION_SERVICE);
					GPSLocListener			=	new GPSLocationListener();
					NetLocListener			=	new NETLocationListener();
					mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, CommonHandler.MinimumTime, CommonHandler.MinimumDistance,	GPSLocListener);
					mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, CommonHandler.MinimumTime, CommonHandler.MinimumDistance,	NetLocListener);		
					GPSs					=	new GPSStatusListener();
					mlocManager.addGpsStatusListener(GPSs);	
					LocalRunning = true;
					showServiceNotification();	
				}else{
					Log.i("SignalTracker::STService", "Parando trabalhos");
					mlocManager.removeGpsStatusListener(GPSs);
					mlocManager.removeUpdates(GPSLocListener);
					mlocManager.removeUpdates(NetLocListener);
					Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
					MyListener 		=	null;
					Tel				=	null;
					mlocManager		=	null;
					GPSs			=	null;
					GPSLocListener	=	null;
					NetLocListener	=	null;
					CommonHandler.GPSFix = false;
					CommonHandler.NumSattelites = 0;
					try {
						String ns = Context.NOTIFICATION_SERVICE;
					    NotificationManager nMgr = (NotificationManager) STService.this.getSystemService(ns);
					    nMgr.cancel(NOTIFICATION);
					}catch(Exception e) {}
					LightModeTimer.cancel();
					LightModeTimer.purge();
					LightModeTask.cancel();
					LightModeTask = null;
					LightModeTimer = new Timer();
					LightModeTask = new TimerTask()	{

						@Override
						public void run() {
							LightModeHandler.sendEmptyMessage(0);
						}
						
					};
					LightModeTimer.schedule(LightModeTask, 60 * 1000 *  CommonHandler.LightModeDelayTime);			
				}
			}
		};
		RunCheck = new Timer();
		RunCheckTask	=	new TimerTask()	{

			@Override
			public void run() {
				RunCheckHandler.sendEmptyMessage(0);
			}
		};
	}
	@Override
	public void onDestroy() {
		RunCheck.cancel();
		StopWorks();
		RunCheck = null;
		RunCheckTask = null;
		Log.i("SignalTracker::STService","Parada não esperada! Reiniciando em 5 segundos");
    	Intent myIntent = new Intent(STService.this, STService.class);
    	PendingIntent pendingIntent = PendingIntent.getService(STService.this, 0, myIntent, 0);
    	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 5);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);  
	}
	private void UpdateData()	{
		CommonHandler.dbman.Open(this);
		if(CommonHandler.Signal > -1 & CommonHandler.Signal <= 31)	{
			if(CommonHandler.GPSFix)
				CommonHandler.AddSignal(CommonHandler.GPSLocation.getLatitude(), CommonHandler.GPSLocation.getLongitude(), CommonHandler.Signal);
			else
				CommonHandler.AddSignal(CommonHandler.NetLocation.getLatitude(), CommonHandler.NetLocation.getLongitude(), CommonHandler.Signal);
		}
		if(CommonHandler.ServiceMode == 1 || CommonHandler.ServiceMode == 3)
			LightModeHandler.sendEmptyMessage(1);
		else if(CommonHandler.ServiceRunning)
			showServiceNotification();
	}
	
	private void CheckRunning()	{
		if(LocalRunning & !CommonHandler.ServiceRunning)	{
			Log.i("SignalTracker::STService","Parando trabalhos do serviço");
			StopWorks();
		}
		if(!LocalRunning & CommonHandler.ServiceRunning)	{
			Log.i("SignalTracker::STService","Iniciando trabalhos do serviço");
			StartWorks();
		}
		RunCheck.cancel();
		RunCheck.purge();
		RunCheck = new Timer();
		RunCheckTask.cancel();
		RunCheckTask	=	new TimerTask()	{

			@Override
			public void run() {
				RunCheckHandler.sendEmptyMessage(0);
			}
			
		};
		RunCheck.schedule(RunCheckTask, 1000);
	}
	private void StopWorks()	{
		if(CommonHandler.ServiceMode == 2 || CommonHandler.ServiceMode == 4){
			Log.i("SignalTracker::STService", "Parando trabalhos");
			mlocManager.removeGpsStatusListener(GPSs);
			mlocManager.removeUpdates(GPSLocListener);
			mlocManager.removeUpdates(NetLocListener);
			Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
			MyListener 		=	null;
			Tel				=	null;
			mlocManager		=	null;
			GPSs			=	null;
			GPSLocListener	=	null;
			NetLocListener	=	null;
			CommonHandler.GPSFix = false;
			CommonHandler.NumSattelites = 0;
			try {
				String ns = Context.NOTIFICATION_SERVICE;
			    NotificationManager nMgr = (NotificationManager) this.getSystemService(ns);
			    nMgr.cancel(NOTIFICATION);
			}catch(Exception e) {}
		}else{
			LightModeTimer.cancel();
			LightModeTask.cancel();
			LightModeTimer.purge();
			LightModeTimer = null;
			LightModeTask = null;
		}
		Toast.makeText(getApplicationContext(), "Serviço Signal Tracker Parado!", Toast.LENGTH_LONG).show();
		LocalRunning = false;
	}
	
	private void StartWorks()	{
		if(CommonHandler.ServiceMode == 2 || CommonHandler.ServiceMode == 4){
			Tel						=	(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			MyListener = new MyPhoneStateListener();
			Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			CommonHandler.Operator	=	Utils.DoOperator(Tel.getNetworkOperatorName());
			
			mlocManager				=	(LocationManager) getSystemService(Context.LOCATION_SERVICE);
			GPSLocListener			=	new GPSLocationListener();
			NetLocListener			=	new NETLocationListener();
			mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, CommonHandler.MinimumTime, CommonHandler.MinimumDistance,	GPSLocListener);
			mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, CommonHandler.MinimumTime, CommonHandler.MinimumDistance,	NetLocListener);		
			GPSs					=	new GPSStatusListener();
			mlocManager.addGpsStatusListener(GPSs);	
			showServiceNotification();
		}else{
			LightModeTimer = new Timer();
			LightModeTask = new TimerTask()	{

				@Override
				public void run() {
					LightModeHandler.sendEmptyMessage(0);
				}
				
			};
			LightModeTimer.schedule(LightModeTask, 1000);
		}
		Toast.makeText(getApplicationContext(), "Serviço Signal Tracker Iniciado!", Toast.LENGTH_LONG).show();
		LocalRunning = true;
	}
    private void showServiceNotification() {
    	int signals = 0, towers = 0;
    	if(CommonHandler.Signals != null)
    		signals = CommonHandler.Signals.size();
    	if(CommonHandler.Towers != null)
    		towers = CommonHandler.Towers.size();
    	
    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(this)
    	        .setSmallIcon(R.drawable.ic_stat_service)
    	        .setContentTitle(TAG)
    	        .setContentText("Pontos("+signals+") Torres("+towers+") Sats("+CommonHandler.NumSattelites+")");
    	Intent resultIntent = new Intent(this, MainMenu.class);
    	TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    	stackBuilder.addParentStack(MainMenu.class);
    	stackBuilder.addNextIntent(resultIntent);
    	PendingIntent resultPendingIntent =
    	        stackBuilder.getPendingIntent(
    	            0,
    	            PendingIntent.FLAG_UPDATE_CURRENT
    	        );
    	mBuilder.setContentIntent(resultPendingIntent);
    	NotificationManager mNotificationManager =
    	    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	mNotificationManager.notify(NOTIFICATION, mBuilder.build());   	

    }
    
    /*	Classes auxiliares	*/
	private class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			CommonHandler.Signal = (short) signalStrength.getGsmSignalStrength();
			GsmCellLocation tower = (GsmCellLocation) Tel.getCellLocation();
			String networkOperator = Tel.getNetworkOperator();
			if(tower != null & networkOperator != null & networkOperator.length() > 0)	{
				int cid = tower.getCid();
				int lac = tower.getLac();
				int mcc = Integer.parseInt(networkOperator.substring(0, 3));
				int mnc = Integer.parseInt(networkOperator.substring(3));
				int[] data	=	{ cid,lac,mcc,mnc };
				new Utils.TowerFetchTask().execute(STService.this.getResources().getString(R.string.opencell_id), data);
			}
			UpdateData();
		}
	}

	// GPS
	public class GPSStatusListener	implements GpsStatus.Listener {
		@Override
		public void onGpsStatusChanged(int event) {
			switch(event)	{
				case GpsStatus.GPS_EVENT_FIRST_FIX:
	                CommonHandler.GPSFix = true;
	                break;
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					if(CommonHandler.GPSLocation != null)	{
							CommonHandler.GPSFix = (SystemClock.elapsedRealtime() - mLastLocationTime) < 10000;
							//Log.i("SignalTracker::STService","DeltaTime: "+(SystemClock.elapsedRealtime() - mLastLocationTime));
					}
					//if(CommonHandler.GPSFix)
					//	Log.i("SignalTracker::STService","Conexão com GPS recuperada!");
					//else
					//	Log.i("SignalTracker::STService","Conexão com GPS perdida!");
					GpsStatus status = mlocManager.getGpsStatus(null);
					if(status != null)	{
						Iterable<GpsSatellite>satellites = status.getSatellites();
						Iterator<GpsSatellite> sat = satellites.iterator();
						int count = 0;
						while(sat.hasNext())	{
							count++;
							sat.next();
						}
						CommonHandler.NumSattelites = count;
						//Log.i("SignalTracker::STService", "Conectado a "+count+" satélites.");
					}
					break;
			}
		}
	}
	public class GPSLocationListener implements LocationListener {
		
		public GPSLocationListener() {
			CommonHandler.GPSLocation = new Location("");			
		}
		@Override
		public void onLocationChanged(Location loc) {
			CommonHandler.GPSLocation = loc;
			mLastLocationTime = SystemClock.elapsedRealtime();
			UpdateData();
		}
		@Override
		public void onProviderDisabled(String provider) {
			CommonHandler.GPSEnabled = false;
			Toast.makeText(getApplicationContext(), "Gps Desativado, por favor ative-o.", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			CommonHandler.GPSEnabled = true;
			Toast.makeText(getApplicationContext(), "Gps Ativado", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	}

	//Network
	public class NETLocationListener implements LocationListener {
		public NETLocationListener() {
			CommonHandler.NetLocation = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if(CommonHandler.NetLocation == null) 
				CommonHandler.NetLocation = new Location("");			
		}
		@Override
		public void onLocationChanged(Location loc) {
			CommonHandler.NetLocation = loc;
		}
		@Override
		public void onProviderDisabled(String arg0) {}
		@Override
		public void onProviderEnabled(String provider) {}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	}	
	
}
