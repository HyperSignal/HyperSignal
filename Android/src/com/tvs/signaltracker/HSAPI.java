package com.tvs.signaltracker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class HSAPI {
	public static String baseURL	=	"http://10.0.5.138/hypersignal/WebService/";
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
				JSONObject out = Utils.getJSONfromURL(baseURL+"?odata="+URLEncoder.encode(params[0], "UTF-8"));
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
	 * Adiciona um ponto ao banco de dados
	 * @param {double} latitude
	 * @param {double} longitude
	 * @param {String} operatora
	 * @param {int} sinal
	 */
	
	public static void AddSignal(double latitude, double longitude, String operadora, int sinal) {
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
	
}
