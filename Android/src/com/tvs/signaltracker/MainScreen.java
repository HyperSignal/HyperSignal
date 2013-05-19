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
import com.google.android.gms.maps.model.LatLngBounds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
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
	public TextView collectedData, connectionInfo, runMode;
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
		
		setUpMap();
		signals = new ArrayList<GroundOverlay>();
		towers = new ArrayList<GroundOverlay>();

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
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.64374f, -46.65306f), 15));
				
				try	{

					LatLngBounds.Builder boundaryBuilder = new LatLngBounds.Builder();
					for(int i=0;i<CommonHandler.Signals.size();i++)
						boundaryBuilder.include(new LatLng(CommonHandler.Signals.get(i).latitude, CommonHandler.Signals.get(i).longitude));
					
					LatLngBounds bounds = boundaryBuilder.build();
					map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
				}catch(Exception e) {}
				
				if(Utils.isBetterLocation(CommonHandler.GPSLocation, CommonHandler.NetLocation) && CommonHandler.GPSLocation != null)
					map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(CommonHandler.GPSLocation.getLatitude(), CommonHandler.GPSLocation.getLongitude())));
				else if (CommonHandler.NetLocation != null)
					map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(CommonHandler.NetLocation.getLatitude(), CommonHandler.NetLocation.getLongitude())));
				
				collectedData.setText("Sinais: "+CommonHandler.Signals.size()+" Torres: "+CommonHandler.Towers.size());
				if(Utils.isBetterLocation(CommonHandler.GPSLocation, CommonHandler.NetLocation))
					connectionInfo.setText("Conectado via GPS - "+CommonHandler.NumSattelites+" satélites conectados.");
				else
					connectionInfo.setText("Conectado via Rede - "+CommonHandler.NumSattelites+" satélites conectados.");
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
		        .image(BitmapDescriptorFactory.fromResource(lvl)).anchor(0, 1)
		        .position(new LatLng(msg.getData().getDouble("lat"), msg.getData().getDouble("lon")), 100f)); 
				signals.add(g);
            }
		};
		AddTower = new Handler() {
            @Override
            public void handleMessage(Message msg) {
				GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
		        .image(BitmapDescriptorFactory.fromResource(R.drawable.tower_75x75)).anchor(0, 1)
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
        if (map == null) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            map.setMyLocationEnabled(true);
            map.getUiSettings().setScrollGesturesEnabled(false);
            map.getUiSettings().setCompassEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(false);
            map.getUiSettings().setZoomGesturesEnabled(false);
    		for(int i=0;i<CommonHandler.Signals.size();i++)	{
    			SignalObject sig = CommonHandler.Signals.get(i);
    			int lvl	=	getDrawableIdentifier(MainScreen.this, "signal_"+sig.signal);
    			GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
    	        .image(BitmapDescriptorFactory.fromResource(lvl)).anchor(0, 1)
    	        .position(new LatLng(sig.latitude, sig.longitude), 100f)); 
    			signals.add(g);
    		}
    		for(int i=0;i<CommonHandler.Towers.size();i++)	{
    			TowerObject sig = CommonHandler.Towers.get(i);
    			GroundOverlay g = map.addGroundOverlay(new GroundOverlayOptions()
    	        .image(BitmapDescriptorFactory.fromResource(R.drawable.tower_75x75)).anchor(0, 1)
    	        .position(new LatLng(sig.latitude, sig.longitude), 100f)); 
    			towers.add(g);
    		}
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
