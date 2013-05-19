package com.tvs.signaltracker;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import android.util.Base64;

public class TheUpCrypter {
	public static byte[] crypted2;
	
	public static String GenOData(String text) {
		MCrypt crypter = new MCrypt();
		String output = "";
		try {
			byte[] crypted = crypter.encrypt(text);
			Deflater deflater = new Deflater();
			deflater.setInput(crypted);
			deflater.finish();
			crypted2 = crypted;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			while (!deflater.finished()) {
				int byteCount = deflater.deflate(buf);
				baos.write(buf, 0, byteCount);
			}
			deflater.end();
			byte[] compressed = baos.toByteArray();
			output =  Base64.encodeToString(compressed,Base64.DEFAULT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}
	public static String DecodeOData(String text) {
		MCrypt crypter = new MCrypt();
		try {		
			byte[] b64decoded = Base64.decode(text,Base64.DEFAULT);
			Inflater inflater = new Inflater();
			inflater.setInput(b64decoded);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			while (!inflater.finished()) {
				int byteCount = inflater.inflate(buf);
				baos.write(buf, 0, byteCount);
			}
			inflater.end();
			byte[] uncompressed = baos.toByteArray();
			return new String(crypter.decrypt(uncompressed));
		} catch(Exception e)	{
			e.printStackTrace();
		}
		return null;
	}
}