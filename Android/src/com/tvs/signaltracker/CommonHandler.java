package com.tvs.signaltracker;

import java.util.ArrayList;
import java.util.List;

import com.facebook.model.GraphLocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class CommonHandler {
	/*	Constantes	*/
	public static final String[] FB_permissions =	{	"publish_stream"	};	//	Permissões padrões do Facebook 
													 
	public static final String[] FB_read_perm	=	{	"email",				//	Permissões de Leitura no Face
														"photo_upload"	};

	public static final int	MaxMapContent		=	50;							//	
	
	/*	Variáveis de funcionamento	*/
	
	public static DatabaseManager dbman;
	public static Boolean	PreferencesLoaded	=	false;			//	Se as preferências foram carregadas
	public static Boolean	ServiceRunning		=	false;			//	Se o serviço está rodando
	public static Boolean	GPSFix				=	false;			//	Se o GPS está com uma posição
	public static Boolean	GPSEnabled			=	false;			//	Se o GPS está ativado
	
	public static Location 	GPSLocation;							//	Localização pelo GPS
	public static Location 	NetLocation;							//	Localização pela Rede
	public static int		NumSattelites;							//	Número de Satélites Conectados
	public static short		Signal;									//	Sinal do celular
	
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
	public static String FacebookEmail			=	"";				//	Email no Facebook, caso logado
	public static String LastOperator			=	"";				//	Ultima operadora
	public static String Operator				=	"";				//	Operadora atual
	public static short ServiceMode				=	0;				//	0 => Sem Rodar, 1 => Modo Light, 2 => Modo Full, 3 => Modo Offline Light, 4 => Modo Offline Full
	public static int	MinimumDistance			=	50;				//	Distancia Mínima entre pontos em Metros
	public static int	MinimumTime				=	0;				//	Tempo mínimo entre procuras do GPS em Segundos
	public static int 	LightModeDelayTime		=	30;				//	Tempo de espera do modo Light
	public static GraphLocation FacebookLocation;					//	Localização no Facebook, caso logado
	
	/*	Métodos	*/
	public static void InitLists()	{
		if(Signals == null)
			Signals = new ArrayList<SignalObject>();
		if(Towers == null)
			Towers = new ArrayList<TowerObject>();
	}
	public static void LoadLists()	{
		if(dbman != null)	{
			Log.i("SignalTracker::LoadLists","Limpando sinais já enviados.");
			dbman.CleanDoneSignals();
			Log.i("SignalTracker::LoadLists","Limpando torres já enviados.");
			dbman.CleanDoneTowers();
			Signals = dbman.getSignals();
			Towers = dbman.getTowers();
		}else
			Log.e("SignalTracker::LoadLists","DatabaseManager é nulo! ");
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
	public static void DelSignalCallback(STCallBack cb)	{
		try	{
			if(SignalCallbacks != null)	{
				SignalCallbacks.remove(cb);
				Log.i("SignalTracker::DelSignalCallback","Removendo callback");
			}
		}catch(Exception e)	{
			Log.e("SignalTracker::DelSignalCallback", "Erro ao remover callback: "+e.getMessage());
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
	public static void DelTowerCallback(STCallBack cb)	{
		try{
			if(TowerCallbacks != null)	{
				TowerCallbacks.remove(cb);
				Log.i("SignalTracker::DelTowerCallback","Removendo callback");
			}
		}catch(Exception e)	{
			Log.e("SignalTracker::DelTowerCallback", "Erro ao remover callback: "+e.getMessage());
		}
			
	}
	@SuppressLint("NewApi")
	public static void DoResend()	{
		int count = 0, rawcount = 0;
		for(int i=0;i<Signals.size();i++)	{
			SignalObject sig = Signals.get(i);
			if(sig == null)
				Log.i("ST","NULL ERROR");
			if(sig.state == 0)	{
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
					new HSAPI.SendSignal().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sig);
				} else {
					new HSAPI.SendSignal().execute(sig);
				}
				count++;
				rawcount++;
			}else if (Signals.get(i).state == 1)
				count++;
			else if (Signals.get(i).state == 2 & CommonHandler.dbman != null)
				CommonHandler.dbman.UpdateSignal(Signals.get(i).latitude, Signals.get(i).longitude, Signals.get(i).signal, (short) 2);
				
			if(count == 100)
				break;
		}
		if(rawcount > 0)
			Log.i("SignalTracker::DoResend","Reenviando "+rawcount+" sinais. ("+count+")");
		
		count = 0;
		rawcount = 0;
		for(int i=0;i<Towers.size();i++)	{
			TowerObject tower = Towers.get(i);
			if(tower.state == 0)	{
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
					new HSAPI.SendTower().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tower);
				} else {
					new HSAPI.SendTower().execute(tower);
				}
				count++;
				rawcount++;
			}else if (Towers.get(i).state == 1)
				count++;
			else if (Towers.get(i).state == 2 & CommonHandler.dbman != null)
				CommonHandler.dbman.UpdateTower(Towers.get(i).latitude,Towers.get(i).longitude, (short) 2);
				
			if(count == 100)
				break;
		}
		if(rawcount > 0)
			Log.i("SignalTracker::DoResend","Reenviando "+rawcount+" torres. ("+count+")");
		
	}
	@SuppressLint("NewApi")
	public static void AddSignal(double lat, double lon, short signal)	{
		if(Signals != null)	{
			SignalObject tmp	=	new SignalObject(lat,lon,signal);
			boolean add = true;
			for(int i=0; i<Signals.size();i++)	{
				if(tmp.distance(Signals.get(i)) < MinimumDistance)	{
					add = false;
					Signals.get(i).signal = (short) ((Signals.get(i).signal + signal) / 2);
					break;
				}
			}
			if(ServiceMode < 3)	{
				//HSAPI.AddSignal(lat,lon,Operator,signal);
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
					new HSAPI.SendSignal().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tmp);
				} else {
					new HSAPI.SendSignal().execute(tmp);
				}
			}
			if(add)	{
				Signals.add(tmp);
				dbman.insertSignal(lat, lon, signal);
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
						if(removeItens[i] == true)
							DelSignalCallback(i);
				}
			}
		}else
			Log.e("SignalTracker::AddSignal","Lista de Sinais é nula!");
	}
	
	@SuppressLint("NewApi")
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
			if(ServiceMode < 3)	{
				//HSAPI.AddTower(lat,lon,Operator);
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
					new HSAPI.SendTower().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tmp);
				} else {
					new HSAPI.SendTower().execute(tmp);
				}
			}
			if(add)	{
				Towers.add(tmp);
				dbman.insertTower(lat, lon);
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
						if(removeItens[i] == true)
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
			
			String fbid				=	dbman.getPreference("fbid");
			String fbname			=	dbman.getPreference("fbname");	
			String configured		=	dbman.getPreference("configured");
			String servicemode		=	dbman.getPreference("servicemode");
			String mindistance		=	dbman.getPreference("mindistance");
			String mintime			=	dbman.getPreference("mintime");
			String wakelock			=	dbman.getPreference("wakelock");
			String lightmodet		=	dbman.getPreference("lightmodedelay");
			
			if(fbid != null)
				FacebookUID			=	fbid;
			if(fbname != null)
				FacebookName		=	fbname;
			if(configured != null)
				Configured			=	(configured.contains("True")?true:false);
			if(servicemode != null)
				ServiceMode			=	Short.parseShort(servicemode);
			if(mindistance != null)
				MinimumDistance		=	Integer.parseInt(mindistance);
			if(mintime != null)
				MinimumTime			=	Integer.parseInt(mintime);
			if(wakelock != null)	
				WakeLock			=	(wakelock.contains("True")?true:false);
			if(lightmodet != null)
				LightModeDelayTime	=	Integer.parseInt(lightmodet);
			
			PreferencesLoaded = true;
			Log.i("SignalTracker::LoadPreferences", "Preferências carregadas.");
		}else
			Log.e("SignalTracker::LoadPreferences", "Banco de Dados não inicializado!");
	}
}
