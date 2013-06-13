#!/usr/bin/env python
#-*- coding: UTF-8 -*-

WSDIR			=	"/var/www/hypersignal/"			#	Diretório de trabalho do WebService
STKEY			=	"***REMOVED***"				#	Chave do Signal Tracker
STIV			=	"***REMOVED***"				#	IV do Signal Tracker
TILEPATH		=	"/var/www/hstiles/"				#	Caminho para os tiles
OPSPATH			=	"/var/www/hsops/"				#	Caminho para as imagens de operadora
JSPATH			=	"/var/www/hsjs/"				#	Caminho para os javascripts
CSSPATH			=	"/var/www/hscss/"				#	Caminho para os CSS
BASEPATH		=	"/var/www/hsbase/"				#	Caminho para a base do site

PROGVERSION		=	"1.7"							#	Versão do programa

MYHOST			=	"localhost"						#	Host para conexão com banco de dados
MYUSER			=	"root"							#	Usuário para conexão
MYPASS			=	"***REMOVED***"							#	Senha para conexão
MYDB			=	"hypersignal"					#	Banco de Dados

HYPER_ZOOM_RANGE=	(10,16)							#	Range de Zoom para o Banco de Dados

#	Chaves do Twitter API
TW_CONSUMER_KEY 	=	'***REMOVED***'
TW_CONSUMER_SECRET	=	'***REMOVED***'
TW_ACCESS_KEY		=	'***REMOVED***'
TW_ACCESS_SECRET	=	'***REMOVED***'


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
