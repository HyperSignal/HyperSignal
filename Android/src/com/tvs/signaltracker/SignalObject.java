package com.tvs.signaltracker;

public class SignalObject {
	public double latitude,longitude;
	public short signal;
	public short state;
	
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
}
