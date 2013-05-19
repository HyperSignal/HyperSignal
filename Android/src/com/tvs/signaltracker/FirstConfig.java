package com.tvs.signaltracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;

public class FirstConfig extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.firstconfig_scroll);
		ScrollView scrollview = (ScrollView) findViewById(R.id.firsconfig_scrollview);
		LayoutInflater inflater = (LayoutInflater)this.getSystemService
			      (Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.firstconfig, scrollview);
		Button lightmode	=	(Button) findViewById(R.id.firstconfig_lightmode);
		Button fullmode		=	(Button) findViewById(R.id.firstconfig_fullmode);
		Button lightoffline	=	(Button) findViewById(R.id.firstconfig_lightoffline);
		Button fulloffline	=	(Button) findViewById(R.id.firstconfig_fulloffline);

		//0 => Sem Rodar, 1 => Modo Light, 2 => Modo Full, 3 => Modo Offline Light, 4 => Modo Offline Full
		lightmode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CommonHandler.dbman.LoginDB("FirstConfig");
				CommonHandler.dbman.setPreference("servicemode", "1");
				CommonHandler.dbman.setPreference("configured", "True");
				CommonHandler.dbman.LogoutDB("FirstConfig");
				CommonHandler.ServiceMode = 1;		
				EndTheConfig(v);
			}
		});
		fullmode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CommonHandler.dbman.LoginDB("FirstConfig");
				CommonHandler.dbman.setPreference("servicemode", "2");
				CommonHandler.dbman.setPreference("configured", "True");
				CommonHandler.dbman.LogoutDB("FirstConfig");
				CommonHandler.ServiceMode = 2;		
				EndTheConfig(v);
			}
		});
		lightoffline.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CommonHandler.dbman.LoginDB("FirstConfig");
				CommonHandler.dbman.setPreference("servicemode", "3");
				CommonHandler.dbman.setPreference("configured", "True");
				CommonHandler.dbman.LogoutDB("FirstConfig");
				CommonHandler.ServiceMode = 3;		
				EndTheConfig(v);
			}
		});
		fulloffline.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CommonHandler.dbman.LoginDB("FirstConfig");
				CommonHandler.dbman.setPreference("servicemode", "4");
				CommonHandler.dbman.setPreference("configured", "True");
				CommonHandler.dbman.LogoutDB("FirstConfig");
				CommonHandler.ServiceMode = 4;		
				EndTheConfig(v);
			}
		});
	}
	public void EndTheConfig(View v)	{
        Intent intent = new Intent(v.getContext(), ConfigDone.class);
        startActivity(intent);
	}
}
