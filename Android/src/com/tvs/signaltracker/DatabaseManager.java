package com.tvs.signaltracker;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DatabaseManager {
	private static final String DATABASE_NAME = "signaltracker.db";
	private static final int DATABASE_VERSION = 3;
	
	private Context context;
	private SQLiteDatabase db;
	private SQLiteStatement insSignal;
	private SQLiteStatement insTower;
	private static final String INSERTSIGNAL = "insert into signals(latitude,longitude,sinal,state) values (?,?,?,0)";
	private static final String INSERTTOWER = "insert into towers(latitude,longitude,state) values (?,?,0)";
	private List<DBUsers> users;
	
	public DatabaseManager(Context context) {
		this.context = context;
	    OpenHelper openHelper = new OpenHelper(this.context);
	    this.db = openHelper.getWritableDatabase();
	    this.insSignal = this.db.compileStatement(INSERTSIGNAL);
	    this.insTower = this.db.compileStatement(INSERTTOWER);
	    this.users = new ArrayList<DBUsers>();
	}
	public void LoginDB(String user)	{
		this.users.add(new DBUsers(user));
	}
	public void LogoutDB(String user)	{
		for(int i=0;i<this.users.size();i++)	{
			if(this.users.get(i).name == user)	{
				this.users.remove(i);
				break;
			}
		}
	}
	public long insertSignal (Double latitude, Double longitude, int sinal) {
		this.insSignal.bindDouble(1, latitude);
		this.insSignal.bindDouble(2, longitude);
		this.insSignal.bindLong(3, sinal);
		return this.insSignal.executeInsert();
	}
	
	public long insertTower (Double latitude, Double longitude) {
		this.insTower.bindDouble(1, latitude);
		this.insTower.bindDouble(2, longitude);
		return this.insTower.executeInsert();
	}
	public long setSignalState(int id, int state)	{
		ContentValues values = new ContentValues();
		values.put("state", state);		
		return this.db.update("signals", values, "id=?", new String[] { Integer.toString(id) });
	}
	public long setTowerState(int id, int state)	{
		ContentValues values = new ContentValues();
		values.put("state", state);		
		return this.db.update("towers", values, "id=?", new String[] { Integer.toString(id) });
	}
	public void CleanTowerSignal() {
		this.db.delete("signals", null, null);
		this.db.delete("towers", null, null);
	}
	public void CleanPreferences()	{
		this.db.delete("preferences", null, null);
	}
	public void CleanAll()	{
		this.db.delete("signals", null, null);
		this.db.delete("towers", null, null);
		this.db.delete("preferences", null, null);		
	}
	public void Close() {
		if(this.users.size() == 0)
			this.db.close();
	}
	public boolean isOpen()	{
		return this.db.isOpen();
	}
	public void Open(Context context)	{
		if(!this.db.isOpen())	{
			this.context = context;
		    OpenHelper openHelper = new OpenHelper(this.context);
		    this.db = openHelper.getWritableDatabase();
		    this.insSignal = this.db.compileStatement(INSERTSIGNAL);
		    this.insTower = this.db.compileStatement(INSERTTOWER);
		    if(this.users == null)
		    	this.users = new ArrayList<DBUsers>();
		}
	}
	public List<SignalObject> getSignals() {
		List<SignalObject> table = new ArrayList<SignalObject>();
		Cursor cursor = this.db.query("signals", new String[] { "latitude", "longitude", "sinal", "state"}, null, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do {
				table.add(new SignalObject(cursor.getDouble(0),cursor.getDouble(1),(short) cursor.getShort(2),cursor.getShort(3)));
			}while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return table;
	}
	
	public List<TowerObject> getTowers() {
		List<TowerObject> table = new ArrayList<TowerObject>();
		Cursor cursor = this.db.query("towers", new String[] { "latitude", "longitude", "state"}, null, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do {
				table.add(new TowerObject(cursor.getDouble(0),cursor.getDouble(1),cursor.getShort(2)));
			}while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return table;
	}	

	public long setPreference(String key, String value)	{
		if(key != null & value != null)	{
			if(getPreference(key) != null)	{
				ContentValues values = new ContentValues();
				values.put("prefval", value);		
				return this.db.update("preferences", values, "prefkey=?", new String[] { key });			
			}else{
				ContentValues values = new ContentValues();
				values.put("prefkey", key);
				values.put("prefval", value);
				return this.db.insert("preferences", null, values);
			}
		}else
			return -1;
	}
	public String	getPreference(String key)	{
		if(key != null)	{
			Cursor cursor = this.db.query("preferences", new String[] 	{ "prefkey", "prefval" }, "prefkey = ?" , new String[] { key }, null, null, null);
			if(cursor.moveToFirst())	{
				String result = cursor.getString(cursor.getColumnIndex("prefval"));
				cursor.close();
				return result;
			}else{
				cursor.close();
				return null;
			}
		}else{
			return null;
		}
	}
	private static class DBUsers {
		public String name;
		public DBUsers(String name)	{
			this.name = name;
		}
	}
	private static class OpenHelper extends SQLiteOpenHelper {

	    OpenHelper(Context context) {
	       super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) { 
	    	Log.i("Database Manager", "Criando Tabela Inicial");
	    	db.execSQL("CREATE TABLE signals (id INTEGER PRIMARY KEY, latitude DOUBLE, longitude DOUBLE, sinal INTEGER, state INTEGER)");
	    	db.execSQL("CREATE TABLE towers (id INTEGER PRIMARY KEY, latitude DOUBLE, longitude DOUBLE, state INTEGER)");
	    	db.execSQL("CREATE TABLE preferences (prefkey TEXT PRIMARY KEY, prefval TEXT)");
	    }

	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	       Log.w("Database Manager", "Fazendo upgrade da tabela. Todos os dados serão apagados.");
	       db.execSQL("DROP TABLE IF EXISTS signals");
	       db.execSQL("DROP TABLE IF EXISTS towers");
	       db.execSQL("DROP TABLE IF EXISTS preferences");
	       onCreate(db);
	    }
	 }	
}