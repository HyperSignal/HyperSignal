package com.tvs.signaltracker;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import com.facebook.*;
import com.facebook.Session.*;
import com.facebook.model.*;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;


public class FacebookActivity extends Activity {	
	Button fblogin, fb_next;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.facebook_activity);
		fb_next	=	(Button)	findViewById(R.id.fb_nextbtn);
		fblogin = (Button) findViewById(R.id.fb_login);
		fblogin.setVisibility(View.INVISIBLE);
		fb_next.setVisibility(View.INVISIBLE);
		
		final Session session = Session.openActiveSessionFromCache(this);
		if (session.isOpened()) {
			Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
				@Override
				public void onCompleted(GraphUser user, Response response) {
					ProgressBar prg	=	(ProgressBar) findViewById(R.id.fb_loadspin);
					prg.setVisibility(View.INVISIBLE);
					if (user != null) {
						TextView welcome = (TextView) findViewById(R.id.fbauthorized);
						welcome.setText("Olá " + user.getName() + ", \n Você autorizou o SignalTracker!");
						CommonHandler.FacebookUID = user.getId();
						CommonHandler.FacebookName = user.getName();
						CommonHandler.InitDB(FacebookActivity.this);
						CommonHandler.dbman.setPreference("fbid", CommonHandler.FacebookUID);
						CommonHandler.dbman.setPreference("fbname", CommonHandler.FacebookName);
						CheckFBPerms(session);
						fblogin.setVisibility(View.INVISIBLE);
					}else
						fblogin.setVisibility(View.VISIBLE);
					fb_next.setVisibility(View.VISIBLE);
				}
			});
		}
		
		fblogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Session.openActiveSession(FacebookActivity.this, true, new Session.StatusCallback() {
					@Override
					public void call(final Session session, SessionState state, Exception exception) {
						if (session.isOpened()) {
							Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
								@Override
								public void onCompleted(GraphUser user, Response response) {
									if (user != null) {
										TextView welcome = (TextView) findViewById(R.id.fbauthorized);
										welcome.setText("Olá " + user.getName() + ", \n Você autorizou o SignalTracker!");
										CommonHandler.FacebookUID = user.getId();
										CommonHandler.FacebookName = user.getName();
										CommonHandler.InitDB(FacebookActivity.this);
										CommonHandler.dbman.setPreference("fbid", CommonHandler.FacebookUID);
										CommonHandler.dbman.setPreference("fbname", CommonHandler.FacebookName);
										CheckFBPerms(session);
										fblogin.setVisibility(View.INVISIBLE);
									}else
										fblogin.setVisibility(View.VISIBLE);
									fb_next.setVisibility(View.VISIBLE);
								}
							});
						}
					}
				});
			}
		});

		fb_next.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	public void CheckFBPerms(Session session)	{
		if(session != null)	{
		  	List<String> perms		=	session.getPermissions();
		  	List<String> pendingw	=	new ArrayList<String>();

	  		for(int p=0;p<CommonHandler.FB_permissions.length;p++)	{
	  			if(perms.indexOf(CommonHandler.FB_permissions[p]) == -1)
	  				pendingw.add(CommonHandler.FB_permissions[p]);
	  		}
	  		if(pendingw.size() > 0)
	  			session.requestNewPublishPermissions(new NewPermissionsRequest(FacebookActivity.this, pendingw));
	  		else{
		  		for(int p=0;p<CommonHandler.FB_read_perm.length;p++)	{
		  			if(perms.indexOf(CommonHandler.FB_read_perm[p]) == -1)
		  				pendingw.add(CommonHandler.FB_read_perm[p]);
		  		}	  			
		  		if(pendingw.size() > 0)
		  			session.requestNewReadPermissions(new NewPermissionsRequest(FacebookActivity.this, pendingw));
		  		else
		  			Log.i("FB Perms", "Permissões OK");
	  		}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	  	Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
}
