package com.tvs.signaltracker;

/**
 * @author Lucas Teske
 *  _   _                       ____  _                   _ 
 * | | | |_   _ _ __   ___ _ __/ ___|(_) __ _ _ __   __ _| |
 * | |_| | | | | '_ \ / _ \ '__\___ \| |/ _` | '_ \ / _` | |
 * |  _  | |_| | |_) |  __/ |   ___) | | (_| | | | | (_| | |
 * |_| |_|\__, | .__/ \___|_|  |____/|_|\__, |_| |_|\__,_|_|
 *       |___/|_|                      |___/               
 *  ____  _                   _ _____               _             
 * / ___|(_) __ _ _ __   __ _| |_   _| __ __ _  ___| | _____ _ __ 
 * \___ \| |/ _` | '_ \ / _` | | | || '__/ _` |/ __| |/ / _ \ '__|
 * _ __) | | (_| | | | | (_| | | | || | | (_| | (__|   <  __/ |   
 * |____/|_|\__, |_| |_|\__,_|_| |_||_|  \__,_|\___|_|\_\___|_|   
 *         |___/                                                 
 * 
 * Created by: Lucas Teske from Teske Virtual System
 * Package: com.tvs.signaltracker
 */


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class HSAPI {
	public static final String baseURL	=	"http://server-b02.tvssite.ws:81/hsapi/";		//	The Base URL for the API
    public static final String TILES_SYNTAX = baseURL+"?operadora=%s&tile=%d-%d-%d";		//	The Syntax for download tiles

	/**
	 * Do an Async API Call
	 * @param	params	ODATA Parameter
	 * @see AsyncTask
	 */
	public static class CallAPI extends AsyncTask<String, Integer, Long> {
		@Override
		protected Long doInBackground(String... params) {
			try {
				JSONObject out = Utils.getODataJSONfromURL(baseURL+"?odata="+URLEncoder.encode(params[0], "UTF-8"));
				if(out != null)	{
					if(out.getString("result").indexOf("OK") > -1)
						Log.i("SignalTracker::APICall","OK");
					else
						Log.e("SignalTracker::APICall","Error: "+out.getString("result"));
				}else
					Log.e("SignalTracker::APICall","No Output");
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			return null;
		}
	}
	/**
	 * Adds info about a tower in Database
	 * @param	tower	Tower data to send
	 * @see TowerObject
	 * @see AsyncTask
	 */
	public static class SendTower extends AsyncTask<TowerObject, Integer, Long> {
		TowerObject tower;
		
		@Override
		protected Long doInBackground(TowerObject... params) {
			try {
				tower = params[0];
				tower.state = 1;
				String jsondata = "{\"metodo\":\"addtorre\",\"op\":\""+CommonHandler.Operator+"\",\"lat\":"+String.valueOf(tower.latitude)+",\"lon\":"+String.valueOf(tower.longitude)+", \"uid\":\""+CommonHandler.FacebookUID+"\"}";
				jsondata = TheUpCrypter.GenOData(jsondata);
				JSONObject out = Utils.getODataJSONfromURL(baseURL+"?odata="+URLEncoder.encode(jsondata, "UTF-8"));
				if(out != null)	{
					if(out.getString("result").indexOf("OK") > -1)	{
						tower.state = 2;
						CommonHandler.dbman.deleteTower(tower.id);
					}else{
						Log.e("SignalTracker::SendTower","Error: "+out.getString("result"));
						tower.state = 0;
					}
				}else{
					tower.state = 0;
					Log.e("SignalTracker::SendTower","No Output");
				}
				if(tower.state != 2)
					CommonHandler.dbman.UpdateTower(tower.latitude, tower.longitude, tower.state);
			} catch (Exception e) {
				Log.e("SignalTracker::SendTower","Error: "+e.getMessage());
				tower.state = 0;
			}
			
			return null;
		}
	}
	
	/**
	 * Adds info about a signal in Database
	 * @param	signal	Signal data to be send
	 * @see	SignalObject
	 * @see AsyncTask
	 */
	public static class SendSignal extends AsyncTask<SignalObject, Integer, Long> {
		SignalObject signal;
		
		@Override
		protected Long doInBackground(SignalObject... params) {
			try {
				signal = params[0];
				signal.state = 1;
				String jsondata = "{\"metodo\":\"addsinal\",\"uid\":\""+CommonHandler.FacebookUID+"\",\"weight\":"+signal.weight+",\"mindistance\":"+CommonHandler.MinimumDistance+",\"op\":\""+CommonHandler.Operator+"\",\"lat\":"+String.valueOf(signal.latitude)+",\"lon\":"+String.valueOf(signal.longitude)+",\"dev\":\""+Build.DEVICE+"\",\"man\":\""+Build.MANUFACTURER+"\",\"model\":\""+Build.MODEL+"\",\"brand\":\""+Build.BRAND+"\",\"rel\":\""+Build.VERSION.RELEASE+"\",\"and\":\""+Build.ID+"\",\"sig\":"+String.valueOf(signal.signal)+"}";
				jsondata = TheUpCrypter.GenOData(jsondata);
				JSONObject out = Utils.getODataJSONfromURL(baseURL+"?odata="+URLEncoder.encode(jsondata, "UTF-8"));
				if(out != null)	{
					if(out.getString("result").indexOf("OK") > -1)	{
						signal.state = 2;
						CommonHandler.dbman.deleteSignal(signal.id);
					}else{
						Log.e("SignalTracker::SendSignal","Error: "+out.getString("result"));
						signal.state = 0;
					}
				}else{
					signal.state = 0;
					Log.e("SignalTracker::SendSignal","No Output");
				}
				if(signal.state != 2)
					CommonHandler.dbman.UpdateSignal(signal.latitude, signal.longitude, signal.signal, signal.state);
			} catch (Exception e) {
				Log.e("SignalTracker::SendSignal","Error: "+e.getMessage());
				signal.state = 0;
			}
			
			return null;
		}
	}

	/**
	 * Adds user to database
	 * @param	username	Username in Facebook
	 * @param	name		Name in Facebook
	 * @param	email		Email in Facebook
	 * @param	city		City in Facebook
	 * @param	country		Country in Facebook
	 */
	public static void AddUser(String username, String name, String email, String city, String country)	{
		String jsondata = "{\"metodo\":\"adduser\",\"username\":\""+username+"\",\"email\":\""+email+"\",\"name\":\""+name+"\", \"uid\":\""+CommonHandler.FacebookUID+"\", \"city\":\""+city+"\", \"country\":\""+country+"\"}";
		jsondata = TheUpCrypter.GenOData(jsondata);
		new CallAPI().execute(jsondata);
		Log.i("SignalTracker::APICall","User added");
	}
}
