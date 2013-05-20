package com.tvs.signaltracker;

public class TowerObject {
	public double latitude,longitude;
	public short state;
	public int id;
	
	public TowerObject(double latitude, double longitude)	{
		this.latitude = latitude;
		this.longitude = longitude;
		this.state = 0;
	}	
	public TowerObject(double latitude, double longitude, short state)	{
		this.latitude = latitude;
		this.longitude = longitude;
		this.state = state;
	}
	public double distance(TowerObject target) {
		double dLat = ( Math.PI / 180 ) * (this.latitude-target.latitude);
		double dLon = ( Math.PI / 180 ) * (this.longitude-target.longitude);
		double lat1 = ( Math.PI / 180 ) * this.latitude;
		double lat2 = ( Math.PI / 180 ) * target.latitude;
		
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
		
		return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	}
}
