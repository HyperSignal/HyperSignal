package com.tvs.signaltracker;

public class TowerObject {
	public double latitude,longitude;
	public short state;
	
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
}
