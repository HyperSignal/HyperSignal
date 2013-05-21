package com.tvs.signaltracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ConfigDone extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configdone);
		Button startuse	=	(Button) findViewById(R.id.startusing);
		startuse.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), MainScreen.class);
                startActivity(intent);
                CommonHandler.Configured = true;
			}
		});
	}
}
