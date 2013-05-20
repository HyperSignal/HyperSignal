package com.tvs.signaltracker;

public class SignalObject {
	public double latitude,longitude;
	public short signal;
	public short state;
	public int id;
	
	public SignalObject(double latitude, double longitude, short signal)	{
		this.latitude = latitude;
		this.longitude = longitude;
		this.signal = signal;
		this.state = 0;
	}	
	public SignalObject(double latitude, double longitude, short signal, short state)	{
		this.latitude = latitude;
		this.longitude = longitude;
		this.signal = signal;
		this.state = state;
	}
	public double distance(SignalObject target) {
		double dLat = ( Math.PI / 180 ) * (this.latitude-target.latitude);
		double dLon = ( Math.PI / 180 ) * (this.longitude-target.longitude);
		double lat1 = ( Math.PI / 180 ) * this.latitude;
		double lat2 = ( Math.PI / 180 ) * target.latitude;
		
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
		
		return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	}
}
