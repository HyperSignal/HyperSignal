package com.tvs.signaltracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainScreen  extends FragmentActivity {
	
	//	Callbacks
	public static STCallBack SignalCallBack;
	public static STCallBack TowerCallBack;
	
	//	Objectos Gráficos
	public MapView mapView;
	public GoogleMap map;
	public ProgressBar signalBar;
	public TextView collectedData, connectionInfo, runMode, signalPercent;
	public List<GroundOverlay> signals;
	public List<GroundOverlay> towers;
	
	//Tasks
	public Timer	 UpdateTimer;
	public TimerTask UpdateUI;

	//Handlers
	private Handler AddSignal;
	private Handler AddTower;
	private Handler UpdateUIHandler;
	
	@SuppressLint("HandlerLeak")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainscreen);

		signalBar		=	(ProgressBar)	findViewById(R.id.signalBar);
		collectedData	=	(TextView)		findViewById(R.id.collectedData);
		connectionInfo	=	(TextView)		findViewById(R.id.connectionInfo);
		runMode			=	(TextView)		findViewById(R.id.runMode);
		signalPercent	=	(TextView)		findViewById(R.id.signalPercent);

		setUpMap();

		switch(CommonHandler.ServiceMode)	{
			case 0:	runMode.setText("Serviço Desativado");		break;
			case 1:	runMode.setText("Modo Light");				break;
			case 2:	runMode.setText("Modo Full");				break;
			case 3:	runMode.setText("Modo Light Offline");		break;
			case 4:	runMode.setText("Modo Full Offline");		break;
		}
		
		UpdateUIHandler = new Handler()	{
			@Override
			public void handleMessage(Message msg)	{
				collectedData.setText("Sinais: "+(CommonHandler.Signals.size()/10f)+" km Torres: "+CommonHandler.Towers.size()+" - ("+CommonHandler.Operator+")");
				if(Utils.isBetterLocation(CommonHandler.GPSLocation, CommonHandler.NetLocation) && CommonHandler.GPSLocation != null)
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(CommonHandler.GPSLocation.getLatitude(), CommonHandler.GPSLocation.getLongitude()),16));
				else if (CommonHandler.NetLocation != null)
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(CommonHandler.NetLocation.getLatitude(), CommonHandler.NetLocation.getLongitude()),16));
				
				if(Utils.isBetterLocation(CommonHandler.GPSLocation, CommonHandler.NetLocation))
					connectionInfo.setText("Conectado via GPS - "+CommonHandler.NumSattelites+" satélites conectados.");
				else
					connectionInfo.setText("Conectado via Rede - "+CommonHandler.NumSattelites+" satélites conectados.");
				signalBar.setProgress(CommonHandler.Signal);
				signalPercent.setText((Math.round((CommonHandler.Signal/31.0f)*100))+"%");
				UpdateTimer.cancel();
				UpdateTimer.purge();
				UpdateTimer = new Timer();
				UpdateUI.cancel();
				UpdateUI	=	new TimerTask()	{

					@Override
					public void run() {
						UpdateUIHandler.sendEmptyMessage(0);
					}
					
				};
				UpdateTimer.schedule(UpdateUI, 1000);
			}
		};
		AddSignal = new Handler() {
            @Override
            public void handleMessage(Message msg) {
				int lvl	=	getDrawableIdentifier(MainScreen.this, "signal_"+msg.getData().getShort("signal"));
				GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
		        .image(BitmapDescriptorFactory.fromResource(lvl)).anchor(0.5f, 0.5f)
		        .position(new LatLng(msg.getData().getDouble("lat"), msg.getData().getDouble("lon")), 100f)); 
				signals.add(g);
            }
		};
		AddTower = new Handler() {
            @Override
            public void handleMessage(Message msg) {
				GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
		        .image(BitmapDescriptorFactory.fromResource(R.drawable.tower_75x75)).anchor(0.5f, 0.5f)
		        .position(new LatLng(msg.getData().getDouble("lat"), msg.getData().getDouble("lon")), 100f)); 
				towers.add(g);
            }
		};
		if(CommonHandler.ServiceMode > 2)	{
			((RelativeLayout) mapView.getParent()).removeView(mapView);
			map = null;
		}else{
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
					AddSignal.sendMessage(msg);
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
					AddTower.sendMessage(msg);
				}
				
			};
			CommonHandler.AddTowerCallback(TowerCallBack);
			CommonHandler.AddSignalCallback(SignalCallBack);
		}

		UpdateUI	=	new TimerTask()	{

			@Override
			public void run() {
				UpdateUIHandler.sendEmptyMessage(0);
			}
			
		};
		UpdateTimer = new Timer();
		UpdateTimer.schedule(UpdateUI, 1000);
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
	            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
	                    .getMap();
	            map.setMyLocationEnabled(false);
	            map.getUiSettings().setScrollGesturesEnabled(false);
	            map.getUiSettings().setCompassEnabled(false);
	            map.getUiSettings().setZoomControlsEnabled(false);
	            map.getUiSettings().setZoomGesturesEnabled(false);
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
	        }
		}catch(Exception e)	{
			Log.e("SignalTracker::setUpMap", "Erro: "+e.getMessage());
		}
	}
	public static int getDrawableIdentifier(Context context, String name) {
	    return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
	}
    @Override
    protected void onResume() {
        super.onResume();
        setUpMap();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
	@Override
	protected void onDestroy()	{
		super.onDestroy();
		CommonHandler.DelSignalCallback(SignalCallBack);
		CommonHandler.DelTowerCallback(TowerCallBack);
	}
	
}
