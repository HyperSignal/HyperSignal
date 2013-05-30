package com.tvs.signaltracker;

/**
 * @author Lucas Teske
 *  _   _                       ____  _                   _ 
 * | | | |_   _ _ __   ___ _ __/ ___|(_) __ _ _ __   __ _| |
 * | |_| | | | | '_ \ / _ \ '__\___ \| |/ _` | '_ \ / _` | |
 * |  _  | |_| | |_) |  __/ |   ___) | | (_| | | | | (_| | |
 * |_| |_|\__, | .__/ \___|_|  |____/|_|\__, |_| |_|\__,_|_|
 *       |___/|_|                      |___/               
 *  ____  _                   _ _____               _             
 * / ___|(_) __ _ _ __   __ _| |_   _| __ __ _  ___| | _____ _ __ 
 * \___ \| |/ _` | '_ \ / _` | | | || '__/ _` |/ __| |/ / _ \ '__|
 * _ __) | | (_| | | | | (_| | | | || | | (_| | (__|   <  __/ |   
 * |____/|_|\__, |_| |_|\__,_|_| |_||_|  \__,_|\___|_|\_\___|_|   
 *         |___/                                                 
 * 
 * Created by: Lucas Teske from Teske Virtual System
 * Package: com.tvs.signaltracker
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
	
	/**
	 * Initializes a Database Manager
	 * @param context The Context that will initialize the database
	 */
	public DatabaseManager(Context context) {
		this.context = context;
	    OpenHelper openHelper = new OpenHelper(this.context);
	    this.db = openHelper.getWritableDatabase();
	    this.insSignal = this.db.compileStatement(INSERTSIGNAL);
	    this.insTower = this.db.compileStatement(INSERTTOWER);
	    this.users = new ArrayList<DBUsers>();
	}
	
	/**
	 * Do a login on database
	 * @param user	The Activity name that will use the database
	 */
	public void LoginDB(String user)	{
		this.users.add(new DBUsers(user));
	}
	
	/**
	 * Do a logout on database
	 * @param user	The Activity name that used the database
	 */
	public void LogoutDB(String user)	{
		for(int i=0;i<this.users.size();i++)	{
			if(this.users.get(i).name == user)	{
				this.users.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Deletes a Signal from database with a ID
	 * @param id	The Database ID of Signal
	 */
	public void deleteSignal(int id)	{ 
		this.db.delete("signals", "id=?", new String[] {Integer.toString(id) } );
	}
	
	/**
	 * Deletes a Tower from database with a ID
	 * @param id	The Database ID of Tower
	 */
	public void deleteTower(int id)	{ 
		this.db.delete("towers", "id=?", new String[] {Integer.toString(id) } );
	}
	
	/**
	 * 
	 * @param latitude		The Latitude of Signal
	 * @param longitude		The Longitude of Signal
	 * @param sinal			The Strenght of Signal
	 * @return	The Row ID of inserted Signal
	 */
	public long insertSignal (Double latitude, Double longitude, int sinal) {
		this.insSignal.bindDouble(1, latitude);
		this.insSignal.bindDouble(2, longitude);
		this.insSignal.bindLong(3, sinal);
		return this.insSignal.executeInsert();
	}
	
	/**
	 * 
	 * @param latitude		The Latitude of Tower
	 * @param longitude		The Longitude of Tower
	 * @return	The Row ID of inserted Tower
	 */
	public long insertTower (Double latitude, Double longitude) {
		this.insTower.bindDouble(1, latitude);
		this.insTower.bindDouble(2, longitude);
		return this.insTower.executeInsert();
	}
	
	/**
	 * 	Do a clean on Towers and Signals table.
	 * 	This will erase all tower and signal data
	 * 	stored on device.
	 */
	public void CleanTowerSignal() {
		this.db.delete("signals", null, null);
		this.db.delete("towers", null, null);
	}
	
	/**
	 * 	Do a clean on Preferences table.
	 * 	This will erase all preferences stored
	 * 	on device.
	 */
	public void CleanPreferences()	{
		this.db.delete("preferences", null, null);
	}
	
	/**
	 * 	Do a complete clean on database.
	 * 	This will reset the Database to original state.
	 */
	public void CleanAll()	{
		this.db.delete("signals", null, null);
		this.db.delete("towers", null, null);
		this.db.delete("preferences", null, null);		
	}
	
	/**
	 * 	Closes the database, if no users (activities) are logged
	 */
	public void Close() {
		if(this.users.size() == 0)
			this.db.close();
	}
	
	/**
	 * Check if the database is open.
	 * @return	<b>True</b> if database is open. <b>False</b> otherwise.
	 */
	public boolean isOpen()	{
		return this.db.isOpen();
	}
	
	/**
	 * Opens a database with the context.
	 * It will only open the database if it 
	 * is not open.
	 * @param context	The context that will open database
	 */
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
	
	/**
	 * Updates a signal data on database.
	 * This will delete the signal from database 
	 * if <b>state == 2</b>, or update all data if <b>signal != 2</b>
	 * @param lat	Latitude of Signal
	 * @param lon	Longitude of Signal
	 * @param signal	Strength of Signal
	 * @param state	State of Signal
	 * @return	Number of rows affected (if all right, 1, else 0)
	 */
	public long UpdateSignal(double lat, double lon, short signal, short state)	{
		if(state == 2)	{
			return this.db.delete("signals", "latitude=? and longitude=? and sinal=?", new String[] { Double.toString(lat), Double.toString(lon), Integer.toString(signal) });
		}else{
			ContentValues values = new ContentValues();
			values.put("state", state);
			return this.db.update("signals", values, "latitude=? and longitude=? and sinal=?", new String[] { Double.toString(lat), Double.toString(lon), Integer.toString(signal) });	
		}
	}
	/**
	 * Updates a tower data on database.
	 * This will delete the tower from database 
	 * if <b>state == 2</b>, or update all data if <b>signal != 2</b>
	 * @param lat	Latitude of Tower
	 * @param lon	Longitude of Tower
	 * @param state	State of Tower
	 * @return	Number of rows affected (if all right, 1, else 0)
	 */
	public long UpdateTower(double lat, double lon, short state)	{
		if(state == 2)	{
			return this.db.delete("towers", "latitude=? and longitude=? ", new String[] { Double.toString(lat), Double.toString(lon)});
		}else{
			ContentValues values = new ContentValues();
			values.put("state", state);
			return this.db.update("signals", values, "latitude=? and longitude=?", new String[] { Double.toString(lat), Double.toString(lon) });	
		}
	}
	
	/**
	 * 	Performes a cleanup for already sent signals
	 * 	that still remains on database.
	 */
	public void CleanDoneSignals()	{
		this.db.delete("signals", "state=?", new String[] { "2" });
	}
	
	/**
	 * 	Performes a cleanup for already sent towers
	 * 	that still remains on database.
	 */
	public void CleanDoneTowers()	{
		this.db.delete("towers", "state=?", new String[] { "2" });
	}
	
	/**
	 * Get the list of signals stored in database.
	 * @return	List of SignalObject
	 */
	public List<SignalObject> getSignals() {
		List<SignalObject> table = new ArrayList<SignalObject>();
		Cursor cursor = this.db.query("signals", new String[] { "latitude", "longitude", "sinal", "state", "id"}, null, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do {
				if(cursor.getShort(3)!=2)	{
					SignalObject tmp = new SignalObject(cursor.getDouble(0),cursor.getDouble(1),(short) cursor.getShort(2),(short) 0);
					tmp.id = cursor.getInt(4);
					table.add(tmp);
				}
			}while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return table;
	}
	
	/**
	 * Get the list of towers stored in database
	 * @return	List of TowerObject
	 */
	public List<TowerObject> getTowers() {
		List<TowerObject> table = new ArrayList<TowerObject>();
		Cursor cursor = this.db.query("towers", new String[] {"latitude", "longitude", "state", "id"}, null, null, null, null, null, null);
		if(cursor.moveToFirst()) {
			do {
				if(cursor.getShort(2) != 2)	{
					TowerObject tmp = new TowerObject(cursor.getDouble(0),cursor.getDouble(1),(short) 0);
					tmp.id = cursor.getInt(3);
					table.add(tmp);
				}
					
			}while(cursor.moveToNext());
		}
		if(cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return table;
	}	

	/**
	 * Sets a preference value on database
	 * @param key	The preference key that you want to set
	 * @param value	The preference value that set at key
	 * @return Number of Rows affected if preference exists, or Row ID if not.
	 */
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
	
	/**
	 * Get the value of preference key in database.
	 * @param key	The preference key
	 * @return	Returns the value of preference, or null if not found.
	 */
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
	
	/**
	 * Static DBUser Class for manage activities that is acessing database
	 * @author Lucas Teske
	 *
	 */
	private static class DBUsers {
		public String name;
		public DBUsers(String name)	{
			this.name = name;
		}
	}
	
	/**
	 * Static Helper Class do manage SQLite Database
	 * @author Lucas Teske
	 *
	 */
	private static class OpenHelper extends SQLiteOpenHelper {

		/**
		 * Opens the Helper
		 * @param context	The context that will initialize the helper
		 */
	    OpenHelper(Context context) {
	       super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) { 
	    	Log.i("Database Manager", "Creating base table");
	    	db.execSQL("CREATE TABLE signals (id INTEGER PRIMARY KEY, latitude DOUBLE, longitude DOUBLE, sinal INTEGER, state INTEGER)");
	    	db.execSQL("CREATE TABLE towers (id INTEGER PRIMARY KEY, latitude DOUBLE, longitude DOUBLE, state INTEGER)");
	    	db.execSQL("CREATE TABLE preferences (prefkey TEXT PRIMARY KEY, prefval TEXT)");
	    }
	    /**
	     * Fills the new database with data from the old database.
	     * @param db	The Database
	     * @param sigtable	The List of SignalObject
	     * @param towertable	The List of TowerObject
	     * @param preferences	The HashMap of preferences
	     */
	    private void FillNewDB(SQLiteDatabase db, List<SignalObject> sigtable, List<TowerObject> towertable, HashMap<String, String> preferences)	{
	    	SQLiteStatement insSignal = db.compileStatement(INSERTSIGNAL);
	    	SQLiteStatement insTower = db.compileStatement(INSERTTOWER);
			Iterator<Entry<String, String>> it = preferences.entrySet().iterator();
			while(it.hasNext())	{
		        HashMap.Entry<String, String> pairs = (HashMap.Entry<String, String>)it.next();
		        //System.out.println(pairs.getKey() + " = " + pairs.getValue());
		        Log.i("Database Manager", "Inserting preference ("+pairs.getKey()+"): "+pairs.getValue());
		    	ContentValues values = new ContentValues();
				values.put("prefkey", pairs.getKey());
				values.put("prefval", pairs.getValue());
				db.insert("preferences", null, values);
		        it.remove(); // avoids a ConcurrentModificationException
			}
			for(int i=0;i<sigtable.size();i++)	{
				SignalObject sig = sigtable.get(i);
				insSignal.bindDouble(1, sig.latitude);
				insSignal.bindDouble(2, sig.longitude);
				insSignal.bindLong(3, sig.signal);
				Log.i("Database Manager", "Inserting Signal: "+sig.toString());
				insSignal.executeInsert();
			}
			for(int i=0;i<towertable.size();i++)	{
				TowerObject sig = towertable.get(i);
				insTower.bindDouble(1, sig.latitude);
				insTower.bindDouble(2, sig.longitude);
				Log.i("Database Manager", "Inserting Tower: "+sig.toString());
				insTower.executeInsert();
			}
			sigtable = null;
			towertable = null;
			preferences = null;
	    }
	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	       Log.w("Database Manager", "Upgrading table.");
	       
			List<SignalObject> sigtable = new ArrayList<SignalObject>();
			List<TowerObject> towertable = new ArrayList<TowerObject>();
			Log.i("Database Manager", "Loading Signals");
			Cursor cursor = db.query("signals", new String[] { "latitude", "longitude", "sinal", "state", "id"}, null, null, null, null, null, null);
			if(cursor.moveToFirst()) {
				do {
					if(cursor.getShort(3)!=2)	{
						SignalObject tmp = new SignalObject(cursor.getDouble(0),cursor.getDouble(1),(short) cursor.getShort(2),(short) 0);
						tmp.id = cursor.getInt(4);
						sigtable.add(tmp);
					}
				}while(cursor.moveToNext());
			}
			if(cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			Log.i("Database Manager", "Loading Towers");
			cursor = db.query("towers", new String[] {"latitude", "longitude", "state", "id"}, null, null, null, null, null, null);
			if(cursor.moveToFirst()) {
				do {
					if(cursor.getShort(2) != 2)	{
						TowerObject tmp = new TowerObject(cursor.getDouble(0),cursor.getDouble(1),(short) 0);
						tmp.id = cursor.getInt(3);
						towertable.add(tmp);
					}
				}while(cursor.moveToNext());
			}
			if(cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			HashMap<String, String> preferences = new HashMap<String, String>();
			Log.i("Database Manager", "Loading preferences");
			cursor = db.query("preferences", new String[] 	{ "prefkey", "prefval" }, null , null, null, null, null);
			
			if(cursor.moveToFirst())	{
				do	{
					preferences.put(cursor.getString(cursor.getColumnIndex("prefkey")), cursor.getString(cursor.getColumnIndex("prefval")));
				}while(cursor.moveToNext());
			}
				
			if(cursor != null && !cursor.isClosed())
				cursor.close();
			
	       db.execSQL("DROP TABLE IF EXISTS signals");
	       db.execSQL("DROP TABLE IF EXISTS towers");
	       db.execSQL("DROP TABLE IF EXISTS preferences");
	       onCreate(db);
	       FillNewDB(db,sigtable,towertable,preferences);
	       sigtable = null;
	       towertable = null;
	       preferences = null;
	    }
	 }	
}