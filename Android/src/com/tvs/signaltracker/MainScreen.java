package com.tvs.signaltracker;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainScreen  extends FragmentActivity {	
	//	Callbacks
	public static STCallBack SignalCallBack;
	public static STCallBack TowerCallBack;
	
	//	Localização
	public static Location lastLocation;
	
	//	Objectos Gráficos
	public GoogleMap map;
	public ProgressBar signalBar;
	public TextView collectedData, connectionInfo, runMode, signalPercent;
	public ToggleButton tileView, controlLock;
	public List<GroundOverlay> signals;
	public List<GroundOverlay> towers;
	public TileOverlay STOverlay;

	//	Booleans
	private boolean controlLocked, tileViewing;
	
	//String messages
	private static String gpssatmsg, netsatmsg;
	
	//	Handlers
	@SuppressLint("HandlerLeak")
	private Handler MainScreenHandler = new Handler()	{
		@Override
		public void handleMessage(Message msg)	{
			switch(msg.what)	{
				case 0:	//	AddSignal
					int lvl	=	getDrawableIdentifier(MainScreen.this, "signal_"+msg.getData().getShort("signal"));
					GroundOverlay sig = map.addGroundOverlay(new GroundOverlayOptions()
			        .image(BitmapDescriptorFactory.fromResource(lvl)).anchor(0.5f, 0.5f)
			        .position(new LatLng(msg.getData().getDouble("lat"), msg.getData().getDouble("lon")), 100f)); 
					signals.add(sig);
					break;
				case 1:	//	AddTower
					GroundOverlay tow = map.addGroundOverlay(new GroundOverlayOptions()
			        .image(BitmapDescriptorFactory.fromResource(R.drawable.tower_75x75)).anchor(0.5f, 0.5f)
			        .position(new LatLng(msg.getData().getDouble("lat"), msg.getData().getDouble("lon")), 100f)); 
					towers.add(tow);
					break;
			}
		}
		
	};
	
	//	Tasks
	private Runnable UpdateUI	=	new Runnable()	{

		@Override
		public void run() {
			if(CommonHandler.Signals != null & CommonHandler.Towers != null)
				collectedData.setText(getResources().getString(R.string.signals)+": "+(CommonHandler.Signals.size()*CommonHandler.MinimumDistance/1000.0f)+" km "+getResources().getString(R.string.towers)+": "+CommonHandler.Towers.size()+" - ("+CommonHandler.Operator+")");
			if(controlLocked)	{
				if(lastLocation != null )	{
					if(CommonHandler.GPSLocation != null)
						if(lastLocation.getLatitude() != CommonHandler.GPSLocation.getLatitude() || lastLocation.getLongitude() != CommonHandler.GPSLocation.getLongitude() )
							lastLocation = CommonHandler.GPSLocation;
				}else{
					if(CommonHandler.GPSLocation != null)	
						lastLocation = CommonHandler.GPSLocation;
				}
			}
			if(Utils.isBetterLocation(CommonHandler.GPSLocation, CommonHandler.NetLocation))
				connectionInfo.setText(String.format(Locale.getDefault(), gpssatmsg, CommonHandler.NumSattelites));
			else
				connectionInfo.setText(String.format(Locale.getDefault(), netsatmsg, CommonHandler.NumSattelites));
			signalBar.setProgress(CommonHandler.Signal);
			signalPercent.setText((Math.round((CommonHandler.Signal/31.0f)*100))+"%");
			if(lastLocation != null && controlLocked)
				UpdateMapPosition(lastLocation.getLatitude(), lastLocation.getLongitude(), 16);
			MainScreenHandler.postDelayed(this, 2000);
		}
		
	};
	
	
	private void UpdateMapPosition(double latitude, double longitude, int zoom)	{
		if(map != null)	{
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),zoom));
			LatLngBounds bounds = this.map.getProjection().getVisibleRegion().latLngBounds;
	
			if(!bounds.contains(new LatLng(latitude, longitude)))        {
				CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(zoom).build(); 
				map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainscreen);
		if(STService.Opened == false)	{
    		Intent myIntent = new Intent(MainScreen.this, STService.class);
        	PendingIntent pendingIntent = PendingIntent.getService(MainScreen.this, 0, myIntent, 0);
        	AlarmManager alarmManager = (AlarmManager)MainScreen.this.getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.SECOND, 1);
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
		}
		
		gpssatmsg = getResources().getString(R.string.gpssatmsg);
		netsatmsg = getResources().getString(R.string.netsatmsg);

		controlLocked = true;
		tileViewing = false;
		
		signalBar		=	(ProgressBar)	findViewById(R.id.signalBar);
		collectedData	=	(TextView)		findViewById(R.id.collectedData);
		connectionInfo	=	(TextView)		findViewById(R.id.connectionInfo);
		runMode			=	(TextView)		findViewById(R.id.runMode);
		signalPercent	=	(TextView)		findViewById(R.id.signalPercent);
		tileView		=	(ToggleButton)	findViewById(R.id.tileViewBtn);
		controlLock		=	(ToggleButton)	findViewById(R.id.controlLockBtn);
		
		controlLock.setOnCheckedChangeListener( new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				controlLocked = !isChecked;
				if(map != null)	{
		            map.getUiSettings().setAllGesturesEnabled(!controlLocked);
					if(STOverlay != null)
						STOverlay.setVisible(tileViewing);
				}
				
			}
		});
		
		tileView.setOnCheckedChangeListener( new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				tileViewing = isChecked;
				if(STOverlay != null)
					STOverlay.setVisible(tileViewing);
				
			}
		});
		
		setUpMap();

		switch(CommonHandler.ServiceMode)	{
			case 0:	runMode.setText(getResources().getString(R.string.service_disabled));	break;
			case 1:	runMode.setText(getResources().getString(R.string.lightmode));			break;
			case 2:	runMode.setText(getResources().getString(R.string.fullmode));			break;
			case 3:	runMode.setText(getResources().getString(R.string.lightmodeoff));		break;
			case 4:	runMode.setText(getResources().getString(R.string.fullmodeoff));		break;
		}

		if(CommonHandler.ServiceMode <3 )	{
			SignalCallBack = new STCallBack()	{

				@Override
				public void Call(Object argument) {
					SignalObject sig = (SignalObject) argument;
					Bundle data = new Bundle();
					data.putDouble("lat", sig.latitude);
					data.putDouble("lon", sig.longitude);
					data.putShort("signal", sig.signal);
					Message msg = new Message();
					msg.setData(data);
					msg.what = 0;
					MainScreenHandler.sendMessage(msg);
				}
				
			};
			TowerCallBack = new STCallBack()	{

				@Override
				public void Call(Object argument) {
					TowerObject sig = (TowerObject) argument;
					Bundle data = new Bundle();
					data.putDouble("lat", sig.latitude);
					data.putDouble("lon", sig.longitude);
					Message msg = new Message();
					msg.setData(data);
					msg.what = 1;
					MainScreenHandler.sendMessage(msg);
				}
				
			};
		}

		MainScreenHandler.postDelayed(UpdateUI, 1000);
		CommonHandler.ServiceRunning = true;
	}

	private void setUpMap() {
		signals = new ArrayList<GroundOverlay>();
		towers = new ArrayList<GroundOverlay>();

		try {
	        if (map == null) {
	    		if(CommonHandler.Signals == null || CommonHandler.Towers == null)	{
	                Intent MainMenuIntent  = new Intent().setClass(MainScreen.this, SplashScreen.class);
	                startActivity(MainMenuIntent);
	                finish();
	    		}
	            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.seemap))
	                    .getMap();
	            map.setMyLocationEnabled(false);
	            map.getUiSettings().setScrollGesturesEnabled(!controlLocked);
	            map.getUiSettings().setZoomGesturesEnabled(!controlLocked);
	            map.getUiSettings().setZoomControlsEnabled(false);
	            map.getUiSettings().setCompassEnabled(false);
	            int minSig = (CommonHandler.Signals.size()-CommonHandler.MaxMapContent)>-1?CommonHandler.Signals.size()-CommonHandler.MaxMapContent:0;
	            int minTow = (CommonHandler.Towers.size()-CommonHandler.MaxMapContent)>-1?CommonHandler.Towers.size()-CommonHandler.MaxMapContent:0;
	    		for(int i=CommonHandler.Signals.size()-1;i>minSig;i--)	{
	    			SignalObject sig = CommonHandler.Signals.get(i);
	    			int lvl	=	getDrawableIdentifier(MainScreen.this, "signal_"+sig.signal);
	    			GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
	    	        .image(BitmapDescriptorFactory.fromResource(lvl)).anchor(0.5f, 0.5f)
	    	        .position(new LatLng(sig.latitude, sig.longitude), 100f)); 
	    			signals.add(g);
	    		}
	    		for(int i=CommonHandler.Towers.size()-1;i>minTow;i--)	{
	    			TowerObject sig = CommonHandler.Towers.get(i);
	    			GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
	    	        .image(BitmapDescriptorFactory.fromResource(R.drawable.tower_75x75)).anchor(0.5f, 0.5f)
	    	        .position(new LatLng(sig.latitude, sig.longitude), 100f)); 
	    			towers.add(g);
	    		}
	            TileProvider tileProvider = new UrlTileProvider(256, 256) {
	                @Override
	                public synchronized URL getTileUrl(int x, int y, int zoom) {
	                    URL url = null;
	                    try {
		                    String s = String.format(Locale.US, HSAPI.TILES_SYNTAX , URLEncoder.encode(CommonHandler.Operator, "UTF-8"), zoom, x, y);
	                        url = new URL(s);
	                    } catch (Exception e) {
	                        throw new AssertionError(e);
	                    }
	                    return url;
	                }
	            };

	            STOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
				STOverlay.setVisible(tileViewing);
	        }
		}catch(Exception e)	{
			Log.e("SignalTracker::setUpMap", "Error: "+e.getMessage());
		}
	}
	public static int getDrawableIdentifier(Context context, String name) {
	    return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
	}
    @Override
    protected void onResume() {
        super.onResume();
        try {
			if(STService.Opened == false)	{
	    		Intent myIntent = new Intent(MainScreen.this, STService.class);
	        	PendingIntent pendingIntent = PendingIntent.getService(MainScreen.this, 0, myIntent, 0);
	        	AlarmManager alarmManager = (AlarmManager)MainScreen.this.getSystemService(Context.ALARM_SERVICE);
	            Calendar calendar = Calendar.getInstance();
	            calendar.setTimeInMillis(System.currentTimeMillis());
	            calendar.add(Calendar.SECOND, 1);
	            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
			}
        }catch(Exception e)	{
        	Log.e("SignalTracker::onResume(MainScreen)","Erro ao iniciar serviço: "+e.getMessage());
        }
        if(!CommonHandler.ServiceRunning)	
			CommonHandler.LoadLists();
        setUpMap();
		CommonHandler.AddTowerCallback(TowerCallBack);
		CommonHandler.AddSignalCallback(SignalCallBack);
    }
	@Override
	protected void onDestroy()	{
		super.onDestroy();
		CommonHandler.DelSignalCallback(SignalCallBack);
		CommonHandler.DelTowerCallback(TowerCallBack);
		MainScreenHandler.removeCallbacks(UpdateUI);
		MainScreenHandler.removeMessages(0);
		MainScreenHandler.removeMessages(1);
	}
	
}
