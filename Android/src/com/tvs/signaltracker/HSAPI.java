package com.tvs.signaltracker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class HSAPI {
	public static String baseURL	=	"http://server-b02.tvssite.ws:81/hsapi/";
	/**
	 * Faz a chamada para API em modo assíncrono
	 * @param {String} params
	 * @return Nothing
	 */
	public static class CallAPI extends AsyncTask<String, Integer, Long> {
		/**
		 * Faz a chamada para API em modo assíncrono
		 * @param {String} params
		 * @return {Long} null
		 */
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
	
	public static class SendTower extends AsyncTask<TowerObject, Integer, Long> {
		/**
		 * Faz o envio de torre
		 * @param {SignalObject} params
		 * @return {Long} 0
		 */
		TowerObject tower;
		
		@Override
		protected Long doInBackground(TowerObject... params) {
			try {
				tower = params[0];
				tower.state = 1;
				String jsondata = "{\"metodo\":\"addtorre\",\"op\":\""+CommonHandler.Towers+"\",\"lat\":"+String.valueOf(tower.latitude)+",\"lon\":"+String.valueOf(tower.longitude)+", \"uid\":\""+CommonHandler.FacebookUID+"\"}";
				jsondata = TheUpCrypter.GenOData(jsondata);
				JSONObject out = Utils.getODataJSONfromURL(baseURL+"?odata="+URLEncoder.encode(jsondata, "UTF-8"));
				if(out != null)	{
					if(out.getString("result").indexOf("OK") > -1)	{
						//Log.i("SignalTracker::SendTower","OK");
						tower.state = 2;
					}else{
						Log.e("SignalTracker::SendTower","Error: "+out.getString("result"));
						tower.state = 0;
					}
				}else{
					tower.state = 0;
					Log.e("SignalTracker::SendTower","No Output");
				}
				CommonHandler.dbman.UpdateTower(tower.latitude, tower.longitude, tower.state);
			} catch (Exception e) {
				Log.e("SignalTracker::SendTower","Error: "+e.getMessage());
				tower.state = 0;
			}
			
			return null;
		}
	}
	
	public static class SendSignal extends AsyncTask<SignalObject, Integer, Long> {
		/**
		 * Faz o envio de sinal
		 * @param {SignalObject} params
		 * @return {Long} 0
		 */
		SignalObject signal;
		
		@Override
		protected Long doInBackground(SignalObject... params) {
			try {
				signal = params[0];
				signal.state = 1;
				String jsondata = "{\"metodo\":\"addsinal\",\"uid\":\""+CommonHandler.FacebookUID+"\",\"op\":\""+CommonHandler.Operator+"\",\"lat\":"+String.valueOf(signal.latitude)+",\"lon\":"+String.valueOf(signal.longitude)+",\"dev\":\""+Build.DEVICE+"\",\"man\":\""+Build.MANUFACTURER+"\",\"model\":\""+Build.MODEL+"\",\"brand\":\""+Build.BRAND+"\",\"rel\":\""+Build.VERSION.RELEASE+"\",\"and\":\""+Build.ID+"\",\"sig\":"+String.valueOf(signal.signal)+"}";
				jsondata = TheUpCrypter.GenOData(jsondata);
				JSONObject out = Utils.getODataJSONfromURL(baseURL+"?odata="+URLEncoder.encode(jsondata, "UTF-8"));
				if(out != null)	{
					if(out.getString("result").indexOf("OK") > -1)	{
						//Log.i("SignalTracker::SendSignal","OK");
						signal.state = 2;
					}else{
						Log.e("SignalTracker::SendSignal","Error: "+out.getString("result"));
						signal.state = 0;
					}
				}else{
					signal.state = 0;
					Log.e("SignalTracker::SendSignal","No Output");
				}
				CommonHandler.dbman.UpdateSignal(signal.latitude, signal.longitude, signal.signal, signal.state);
			} catch (Exception e) {
				Log.e("SignalTracker::SendSignal","Error: "+e.getMessage());
				signal.state = 0;
			}
			
			return null;
		}
	}
	/**
	 * Adiciona um ponto ao banco de dados
	 * @param {double} latitude
	 * @param {double} longitude
	 * @param {String} operatora
	 * @param {int} sinal
	 */
	
	public static void AddSignal(double latitude, double longitude, String operadora, short sinal) {
		String jsondata = "{\"metodo\":\"addsinal\",\"uid\":\""+CommonHandler.FacebookUID+"\",\"op\":\""+operadora+"\",\"lat\":"+String.valueOf(latitude)+",\"lon\":"+String.valueOf(longitude)+",\"dev\":\""+Build.DEVICE+"\",\"man\":\""+Build.MANUFACTURER+"\",\"model\":\""+Build.MODEL+"\",\"brand\":\""+Build.BRAND+"\",\"rel\":\""+Build.VERSION.RELEASE+"\",\"and\":\""+Build.ID+"\",\"sig\":"+String.valueOf(sinal)+"}";
		jsondata = TheUpCrypter.GenOData(jsondata);
		Log.i("SignalTracker::APICall","Adicionando chamada de sinal");
		new CallAPI().execute(jsondata);
	}
	/**
	 * Adiciona uma antena ao banco de dados
	 * @param {double} latitude
	 * @param {double} longitude
	 * @param {String} operatora
	 */
	public static void AddTower(double latitude, double longitude, String operadora) {
		String jsondata = "{\"metodo\":\"addtorre\",\"op\":\""+operadora+"\",\"lat\":"+String.valueOf(latitude)+",\"lon\":"+String.valueOf(longitude)+", \"uid\":\""+CommonHandler.FacebookUID+"\"}";
		jsondata = TheUpCrypter.GenOData(jsondata);
		new CallAPI().execute(jsondata);
		Log.i("SignalTracker::APICall","Adicionando chamada de torre");
	}
	
	public static void AddUser(String username, String name, String email, String city, String country)	{
		//TODO: Função pra adicionar usuário na API
		String jsondata = "{\"metodo\":\"adduser\",\"username\":\""+username+"\",\"email\":\""+email+"\",\"name\":\""+name+"\", \"uid\":\""+CommonHandler.FacebookUID+"\", \"city\":\""+city+"\", \"country\":\""+country+"\"}";
		jsondata = TheUpCrypter.GenOData(jsondata);
		new CallAPI().execute(jsondata);
		Log.i("SignalTracker::APICall","Adicionando usuário");
	}
}
