package com.tvs.signaltracker;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.security.KeyStore;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class Utils {
    public static HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new EasySSLSocketFactory(trustStore);
            //sf.setHostnameVerifier( SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }
	public static JSONObject getJSONfromURL(String url){
		if(! (url == "")) {
    		//initialize
    		InputStream is = null;
    		String result = "";
    		JSONObject jArray = null;
    		String error = "";
    		//http post
    		try{
    			HttpClient httpclient = getNewHttpClient();
    			HttpGet httpget = new HttpGet(url);
    			HttpResponse response = httpclient.execute(httpget);
    			HttpEntity entity = response.getEntity();
    			is = entity.getContent();

    			

    		}catch(Exception e){
    			Log.e("SignalTracker::getJSONfromURL", "Error in http connection "+e.toString());
    			error = e.toString();
    		}

    		//convert response to string
    		try{
    			if(error == null || error == "") {
	    			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"),8);
	    			StringBuilder sb = new StringBuilder();
	    			String line = null;
	    			while ((line = reader.readLine()) != null) {
	    				sb.append(line + "\n");
	    			}
	    			is.close();
	    			result=sb.toString();
    			}else{
    				result = "{result:'"+error+"'}";
    			}
    		}catch(Exception e){
    			Log.e("getJSONfromURL", "Error converting result "+e.toString());
    		}

    		//try parse the string to a JSON object
    		try{
    	        	jArray = new JSONObject(result);
    		}catch(JSONException e){
    			Log.e("SignalTracker::getJSONfromURL", "Error parsing data "+e.toString());
    			Log.e("SignalTracker::getJSONfromURL", "Site output: "+result);
    			Log.e("SignalTracker::getJSONfromURL", "URL: "+url);
    		}
    		return jArray;
		}else{
			return null;
		}
	}
	public static String DoOperator(String op) {
		op = op.replace("fVIVO","VIVO")
			.replace("TIM 62", "TIM")
			.replace("TIM 3G+", "TIM")
			.replace("TIM 3G +", "TIM")
			.replace("72402", "TIM")
			.replace("72403", "TIM")
			.replace("72404", "TIM")
			.replace("72405", "CLARO")
			.replace("72406", "VIVO")
			.replace("72408", "TIM")
			.replace("72410", "VIVO")
			.replace("72411", "VIVO")
			.replace("72415", "SCT")
			.replace("72416", "BRT")
			.replace("72423", "TIM")
			.replace("72431", "OI")
			.replace("Oi", "OI");
		
		return op;
	}
	public static void updateLocation(int cid2, int lac2, int mnc2, int mcc2)
			throws IOException {
		InputStream is = null;
		ByteArrayOutputStream bos = null;
		byte[] data = null;
		try {

			StringBuilder uri = new StringBuilder("http://www.opencellid.org/cell");

			uri.append("/get?cellid=").append(cid2);
			uri.append("&mnc=").append(mnc2);
			uri.append("&mcc=").append(mcc2);
			uri.append("&lac=").append(lac2);
			uri.append("&key=").append("3496f1ea3dca07b9608e5aeb79bf52f6");

			HttpGet request = new HttpGet(uri.toString());

			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(request);

			int status = response.getStatusLine().getStatusCode();
			if (status != HttpURLConnection.HTTP_OK) {
				switch (status) {
				case HttpURLConnection.HTTP_NO_CONTENT:
					throw new IOException("The cell could not be "
							+ "found in the database");
				case HttpURLConnection.HTTP_BAD_REQUEST:
					throw new IOException("Check if some parameter "
							+ "is missing or misspelled");
				case HttpURLConnection.HTTP_UNAUTHORIZED:
					throw new IOException("Make sure the API key is "
							+ "present and valid");
				case HttpURLConnection.HTTP_FORBIDDEN:
					throw new IOException("You have reached the limit"
							+ "for the number of requests per day. The "
							+ "maximum number of requests per day is "
							+ "currently 500.");
				case HttpURLConnection.HTTP_NOT_FOUND:
					throw new IOException("The cell could not be found"
							+ "in the database");
				default:
					throw new IOException("HTTP response code: " + status);
				}
			}
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
			bos = new ByteArrayOutputStream();
			byte buf[] = new byte[256];
			while (true) {
				int rd = is.read(buf, 0, 256);
				if (rd == -1)
					break;
				bos.write(buf, 0, rd);
			}
			bos.flush();
			data = bos.toByteArray();
			if (data != null) {
				Document dados = XMLfromString(new String(data));
				NodeList tmp = dados.getElementsByTagName("cell");
				double thisTowerLat = Double.parseDouble(tmp.item(0).getAttributes().getNamedItem("lat").getNodeValue());
				double thisTowerLon = Double.parseDouble(tmp.item(0).getAttributes().getNamedItem("lon").getNodeValue());
				//GetService.addTower(Utils.ctx,thisTowerLat, thisTowerLon);
			}
		} catch (MalformedURLException e) {
			Log.e("ERROR", e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new IOException(
					"URL was incorrect. Did you forget to set the API_KEY?");
		} finally {
			try {
				if (bos != null)
					bos.close();
			} catch (Exception e) {
			}
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}
	}
	public static boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	public static Document XMLfromString(String xml) {

		Document doc = null;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			doc = db.parse(is);

		} catch (ParserConfigurationException e) {
			System.out.println("XML parse error: " + e.getMessage());
			return null;
		} catch (SAXException e) {
			System.out.println("Wrong XML file structure: " + e.getMessage());
			return null;
		} catch (IOException e) {
			System.out.println("I/O exeption: " + e.getMessage());
			return null;
		}

		return doc;

	}
}
