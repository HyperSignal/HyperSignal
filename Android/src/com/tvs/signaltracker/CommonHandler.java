package com.tvs.signaltracker;

import android.content.Context;
import android.util.Log;

public class CommonHandler {
	/*	Constantes	*/
	public static final String[] FB_permissions = {	"publish_stream"	};	//	Permissões padrões do Facebook 
													 
	public static final String[] FB_read_perm	=	{	"email",			//	Permissões de Leitura no Face
														"photo_upload"	};		
	
	
	/*	Variáveis de funcionamento	*/
	
	public static DatabaseManager dbman;
	public static Boolean	PreferencesLoaded	=	false;
	
	/*	Preferências	*/
	public static Boolean Configured 			= 	false;			//	Se o cliente foi configurado
	public static String FacebookUID			= 	"0";			//	UID do Facebook, caso logado
	public static String FacebookName			=	"Anônimo";		//	Nome no Facebook, caso logado
	public static short ServiceMode				=	0;				//	0 => Sem Rodar, 1 => Modo Light, 2 => Modo Full
	public static int	MinimumDistance			=	100;			//	Metros
	
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
			
			String fbid		=	dbman.getPreference("fbid");
			String fbname	=	dbman.getPreference("fbname");	
			String configured	=	dbman.getPreference("configured");
			String servicemode	=	dbman.getPreference("servicemode");
			String mindistance	=	dbman.getPreference("mindistance");
			
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
			PreferencesLoaded = true;
			Log.i("SignalTracker::LoadPreferences", "Preferências carregadas.");
		}else
			Log.e("SignalTracker::LoadPreferences", "Banco de Dados não inicializado!");
	}
}
