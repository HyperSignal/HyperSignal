package com.tvs.signaltracker;

import java.util.TimerTask;

import com.google.android.gms.maps.MapView;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainScreen extends Activity {
	
	//	Callbacks
	public static STCallBack SignalCallBack;
	
	//	Objectos Gráficos
	public MapView map;
	public ProgressBar signalBar;
	public TextView collectedData, connectionInfo;
	
	//Tasks
	public TimerTask UpdateUI;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		map				=	(MapView)		findViewById(R.id.map);
		signalBar		=	(ProgressBar)	findViewById(R.id.signalBar);
		collectedData	=	(TextView)		findViewById(R.id.collectedData);
		connectionInfo	=	(TextView)		findViewById(R.id.connectionInfo);
		
		if(CommonHandler.ServiceMode > 2)	{
			((RelativeLayout) map.getParent()).removeView(map);
			map = null;
		}else{
			SignalCallBack = new STCallBack()	{

				@Override
				public void Call(Object argument) {
					// TODO Adicionar pontos no mapa
					
				}
				
			};
			CommonHandler.AddSignalCallback(SignalCallBack);
		}
		UpdateUI	=	new TimerTask()	{

			@Override
			public void run() {
				// TODO Atualizar UI
				
			}
			
		};
	}
	@Override
	protected void onDestroy()	{
		super.onDestroy();
		CommonHandler.DelSignalCallback(SignalCallBack);
	}
	
}
