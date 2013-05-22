#!/usr/bin/env python
#-*- coding: UTF-8 -*-

WSDIR			=	"/var/www/hypersignal/"	#	Diretório de trabalho do WebService
STKEY			=	"***REMOVED***"		#	Chave do Signal Tracker
STIV			=	"***REMOVED***"		#	IV do Signal Tracker
TILEPATH		=	"/var/www/hstiles/"		#	Caminho para os tiles
OPSPATH			=	"/var/www/hsops/"		#	Caminho para as imagens de operadora
JSPATH			=	"/var/www/hsjs/"		#	Caminho para os javascripts
CSSPATH			=	"/var/www/hscss/"		#	Caminho para os CSS
PROGVERSION		=	"1.7"					#	Versão do programa

MYHOST			=	"localhost"				#	Host de conexão do MySQL
MYUSER			=	"root"					#	Usuário de conexão do MySQL
MYPASS			=	"***REMOVED***"					#	Senha de conexão do MySQL
MYDB			=	"hypersignal"			#	Banco de dados de conexão do MySQL

HYPER_ZOOM_RANGE=	(12,17)					#	Range de Zoom para o Banco de Dados


JSREPLACES		=	{ "SITEURL" : "http://localhost/hypersignal/WebSite/", "APIURL" : "http://localhost/hypersignal/WebService/" }	#	Variáveis para se substituir nos arquivos JavaScript e CSS
OPSREPLACES		=	{
				"Oi"		:	"OI",
				"TIM 62"	:	"TIM",
				"fVIVO" 	:	"VIVO",
				"VIVO SP"	:	"VIVO",
				"TIM 3G+" 	:	"TIM",
				"TIM 3G +" 	:	"TIM",
				"72402" 	:	"TIM",
				"72403" 	:	"TIM",
				"72404" 	:	"TIM",
				"72405" 	:	"CLARO",
				"72406" 	:	"VIVO",
				"72408"		:	"TIM",
				"72410" 	:	"VIVO",
				"72411" 	:	"VIVO",
				"72415" 	:	"SCT",
				"72416" 	:	"BRT",
				"72423" 	:	"VIVO",
				"72431" 	:	"OI"
				}
