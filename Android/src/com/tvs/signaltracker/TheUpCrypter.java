package com.tvs.signaltracker;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;


public class TheUpCrypter {
	
	public static String GenOData(String text) {
		MCrypt crypter = new MCrypt();
		String output = "";
		try {
			byte[] crypted = crypter.encrypt(text);
			Deflater deflater = new Deflater();
			deflater.setInput(crypted);
			deflater.finish();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			while (!deflater.finished()) {
				int byteCount = deflater.deflate(buf);
				baos.write(buf, 0, byteCount);
			}
			deflater.end();
			byte[] compressed = baos.toByteArray();
			output =  Base64.encode(compressed);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}
}