package com.tvs.signaltracker;

import com.facebook.Session;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainMenu extends Activity {
	Button	main_startbutton;
	Button	main_stopbutton;
	Button	main_configbutton;
	Button	main_exitbutton;
	Button	main_seemap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		main_startbutton	=	(Button) findViewById(R.id.main_startbutton);
		main_stopbutton		=	(Button) findViewById(R.id.main_stopbutton);
		main_configbutton	=	(Button) findViewById(R.id.main_configbutton);
		main_exitbutton		=	(Button) findViewById(R.id.main_exitbutton);
		main_seemap			=	(Button) findViewById(R.id.main_seemap);
		
		main_startbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(CommonHandler.Configured){
	                Intent intent = new Intent(v.getContext(), MainScreen.class);
	                startActivity(intent);
				}else{
	                Intent intent = new Intent(v.getContext(), FacebookActivity.class);
	                startActivity(intent);
				}
			}
		});
		main_stopbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CommonHandler.ServiceRunning = false;
				/*	Será que tem mais algo pra fazer aqui? */
			}
		});
		main_configbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(CommonHandler.Configured)	{
	                Intent intent = new Intent(v.getContext(), Settings.class);
	                startActivity(intent);		
				}else{
	                Intent intent = new Intent(v.getContext(), FacebookActivity.class);
	                startActivity(intent);					
				}
			}
		});
		main_exitbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		main_seemap.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SeeMap.class);
                startActivity(intent);
			}
		});
	}
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}*/
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	  super.onActivityResult(requestCode, resultCode, data);
	  Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	  Log.i("AcResult","requestCode: "+requestCode+" resultCode: "+resultCode+" data: "+data.describeContents());
	}

}
