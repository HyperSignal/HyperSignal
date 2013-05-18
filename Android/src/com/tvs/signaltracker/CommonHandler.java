package com.tvs.signaltracker;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class CommonHandler {
	/*	Constantes	*/
	public static final String[] FB_permissions =	{	"publish_stream"	};	//	Permissões padrões do Facebook 
													 
	public static final String[] FB_read_perm	=	{	"email",			//	Permissões de Leitura no Face
														"photo_upload"	};		
	
	
	/*	Variáveis de funcionamento	*/
	
	public static DatabaseManager dbman;
	public static Boolean	PreferencesLoaded	=	false;
	
	/*	Listas	*/
	public static List<SignalObject>	Signals;
	public static List<TowerObject>		Towers;
	
	/*	Callbacks	*/
	private static List<STCallBack>		SignalCallbacks;
	private static List<STCallBack>		TowerCallbacks;
	
	/*	Preferências	*/
	public static Boolean Configured 			= 	false;			//	Se o cliente foi configurado
	public static Boolean WakeLock				=	false;			//	A tela permanecerá ativa até fechar o aplicativo
	public static String FacebookUID			= 	"0";			//	UID do Facebook, caso logado
	public static String FacebookName			=	"Anônimo";		//	Nome no Facebook, caso logado
	public static String LastOperator			=	"";				//	Ultima operadora
	public static short ServiceMode				=	0;				//	0 => Sem Rodar, 1 => Modo Light, 2 => Modo Full
	public static int	MinimumDistance			=	100;			//	Metros
	
	/*	Métodos	*/
	public static void InitLists()	{
		if(Signals == null)
			Signals = new ArrayList<SignalObject>();
		if(Towers == null)
			Towers = new ArrayList<TowerObject>();
	}
	
	public static void InitCallbacks()	{
		if(SignalCallbacks == null)
			SignalCallbacks = new ArrayList<STCallBack>();
		if(TowerCallbacks == null)
			TowerCallbacks = new ArrayList<STCallBack>();
	}
	
	public static void AddSignalCallback(STCallBack cb)	{
		if(SignalCallbacks != null)
			SignalCallbacks.add(cb);
		else
			Log.e("SignalTracker::AddSignalCallback","Lista de Callbacks de sinais é nula!");
	}	
	
	public static void AddTowerCallback(STCallBack cb)	{
		if(TowerCallbacks != null)
			TowerCallbacks.add(cb);
		else
			Log.e("SignalTracker::AddTowerCallback","Lista de Callbacks de torres é nula!");
	}
	public static void DelSignalCallback(int id)	{
		try	{
			if(SignalCallbacks != null)	{
				SignalCallbacks.remove(id);
				Log.i("SignalTracker::DelSignalCallback","Removendo ("+id+")");
			}
		}catch(Exception e)	{
			Log.e("SignalTracker::DelSignalCallback", "Erro ao remover ("+id+"): "+e.getMessage());
		}
	}
	public static void DelTowerCallback(int id)	{
		try{
			if(TowerCallbacks != null)	{
				TowerCallbacks.remove(id);
				Log.i("SignalTracker::DelTowerCallback","Removendo ("+id+")");
			}
		}catch(Exception e)	{
			Log.e("SignalTracker::DelTowerCallback", "Erro ao remover ("+id+"): "+e.getMessage());
		}
			
	}
	
	public static void AddSignal(double lat, double lon, short signal)	{
		if(Signals != null)	{
			SignalObject tmp	=	new SignalObject(lat,lon,signal);
			boolean add = true;
			for(int i=0; i<Signals.size();i++)	{
				if(tmp.distance(Signals.get(i)) < MinimumDistance)	{
					add = false;
					break;
				}
			}
			if(add)	{
				Signals.add(tmp);
				Log.i("SignalTracker::AddSignal","Sinal Adicionado: ("+lat+","+lon+")["+signal+"]");
				if(SignalCallbacks != null)	{
					boolean[]	removeItens = new boolean[SignalCallbacks.size()];
					for(int i=0;i<SignalCallbacks.size();i++)	{
						try{
							SignalCallbacks.get(i).Call(tmp);
							removeItens[i] = false;
						}catch(Exception e)	{
							Log.i("SignalTracker::AddSignal","Erro ao processar callback("+i+"): "+e.getMessage());
							removeItens[i] = true;
						}
					}
					for(int i=removeItens.length-1;i>=0;i--)	
						DelSignalCallback(i);
				}
			}
		}else
			Log.e("SignalTracker::AddSignal","Lista de Sinais é nula!");
	}
	
	public static void AddTower(double lat, double lon)	{
		if(Towers != null)	{
			TowerObject tmp	=	new TowerObject(lat,lon);
			boolean add = true;
			for(int i=0; i<Towers.size();i++)	{
				if(tmp.distance(Towers.get(i)) < MinimumDistance)	{
					add = false;
					break;
				}
			}
			if(add)	{
				Towers.add(tmp);
				Log.i("SignalTracker::AddTower","Torre Adicionado: ("+lat+","+lon+")");
				if(TowerCallbacks != null)	{
					boolean[]	removeItens = new boolean[TowerCallbacks.size()];
					for(int i=0;i<TowerCallbacks.size();i++)	{
						try{
							TowerCallbacks.get(i).Call(tmp);
							removeItens[i] = false;
						}catch(Exception e)	{
							Log.i("SignalTracker::AddTower","Erro ao processar callback("+i+"): "+e.getMessage());
							removeItens[i] = true;
						}
					}
					for(int i=removeItens.length-1;i>=0;i--)	
						DelTowerCallback(i);
				}
			}		
		}else
			Log.e("SignalTracker::AddTower","Lista de torres é nula!");
	}
	
	public static void InitDB(Context ctx)	{
		if(CommonHandler.dbman != null)	{
			if(!CommonHandler.dbman.isOpen())
				CommonHandler.dbman.Open(ctx);
		}else{
			CommonHandler.dbman = new DatabaseManager(ctx);
		}
	}
	public static void LoadPreferences()	{
		/*	Carregar Preferências	*/
		if(dbman != null)	{
			
			String fbid			=	dbman.getPreference("fbid");
			String fbname		=	dbman.getPreference("fbname");	
			String configured	=	dbman.getPreference("configured");
			String servicemode	=	dbman.getPreference("servicemode");
			String mindistance	=	dbman.getPreference("mindistance");
			String wakelock		=	dbman.getPreference("wakelock");
			
			if(fbid != null)
				FacebookUID		=	fbid;
			if(fbname != null)
				FacebookName	=	fbname;
			if(configured != null)
				Configured		=	(configured=="True"?true:false);
			if(servicemode != null)
				ServiceMode		=	Short.parseShort(servicemode);
			if(mindistance != null)
				MinimumDistance	=	Integer.parseInt(mindistance);
			if(wakelock != null)
				WakeLock		=	(wakelock=="True"?true:false);
			
			PreferencesLoaded = true;
			Log.i("SignalTracker::LoadPreferences", "Preferências carregadas.");
		}else
			Log.e("SignalTracker::LoadPreferences", "Banco de Dados não inicializado!");
	}
}
