#-*- coding: UTF-8 -*-

'''
	Dependências:
	pip install slimit
	pip install cssmin
'''
import sys, os, cgi, json, cssmin

from slimit import minify

sys.stdout = sys.stderr

path	=	"/var/www/hypersignal"
if path not in sys.path:
	sys.path.append("/var/www/hypersignal")
	sys.path.append("/var/www/hypersignal/WebServices")
	sys.path.append("/var/www/hypersignal/Includes")

os.environ['PYTHON_EGG_CACHE'] = '/var/www/hypersignal/.python-egg'

#print "Path: %s" % sys.path

import tool, manager, theupcrypter, config

def application(environ, start_response):

	output,status,content = ProcessPage(environ)
	
	response_headers = [('Content-type', content),
						('Content-Length', str(len(output)))]
	start_response(status, response_headers)
	return [output]


def	ProcessPage(_SERVER):
	status = '200 OK'				#	Status a ser retornado
	output = ""						#	Valor a ser retornado
	content = 'text/html'			#	Tipo de retorno

	query	=	cgi.parse_qs(_SERVER["QUERY_STRING"])
	tup 	= 	theupcrypter.TheUpCrypter(config.STKEY,config.STIV)
	hsman	=	manager.HyperSignalManager()
	hsman.ConnectDB()

	if query.has_key("odata"):
		'''
			Processamento de OData
		'''
		try:
			data	=	json.loads(tup.ODataDecrypt(query["odata"][0]))
			if	data["metodo"]	==	"addsinal":
				'''
					Adicionar sinal
				'''
				try:
					if  not data["op"].strip() == "":
						sigs = hsman.ProcessSignal(float(data["lat"]),float(data["lon"]),int(data["sig"]),data["op"].strip())
						hsman.AddStatistics("apicall")
						hsman.CommitToDB()
						hsman.AddDevice(data["uid"].strip(), data["dev"].strip(), data["man"].strip(), data["model"].strip(), data["brand"].strip(), data["and"].strip(), data["rel"].strip(), data["sig"])
						if data.has_key("uid"):
							hsman.IncUserKM(data["uid"], 1)
						hsman.CommitToDB()
				except Exception,e:
					print e
				output	=	tup.ODataEncrypt('{"result":"OK"}')

			elif data["metodo"] == "addtorre":
				'''
					Adicionar Torre
				'''
				try:
					if  not data["op"].strip() == "":
						hsman.AddAntenna(float(data["lat"]),float(data["lon"]),data["op"].strip())
						hsman.AddStatistics("apicall")
						hsman.CommitToDB()
				except Exception,e:
					print e
				output	=	tup.ODataEncrypt('{"result":"OK"}')

			elif data["metodo"] == "ttsadd":
				'''
					Adicionar ponto de sinal via TeskeTrackingSystem
				'''
				try:
					if  not data["op"].strip() == "":
						hsman.ProcessSignal(float(data["lat"]),float(data["lon"]),int(data["sig"]),data["op"].strip())
						hsman.AddStatistics("apicall")
						hsman.AddStatistics("tts")
						hsman.CommitToDB()
				except Exception,e:
					print e
				output	=	tup.ODataEncrypt('{"result":"OK"}')
		
			elif data["metodo"]	==	"ttstoweradd":
				'''
					Adiciona torre via TeskeTrackingSystem
				'''
				try:
					hsman.AddAntenna(float(data["lat"]),float(data["lon"]),data["op"].strip())
					hsman.AddStatistics("apicall")
					hsman.AddStatistics("tts")
					hsman.CommitToDB()
				except Exception,e:
					print e
				output	=	tup.ODataEncrypt('{"result":"OK"}')
			elif data["metodo"] == "adduser":
				'''
					Adiciona um usuário
				'''
				try:	
					hsman.AddUser(data["username"],data["uid"],data["name"],data["email"],_SERVER["REMOTE_ADDR"],data["city"],data["country"])
					hsman.AddStatistics("apicall")
					hsman.CommitToDB()
				except Exception, e:
					print e
				output	=	tup.ODataEncrypt('{"result":"OK"}')
			else:
				'''
					Chamada da API errada!
				'''
				try:
					hsman.AddStatistics("apicallerror")
					hsman.CommitToDB()
				except Exception,e:
					print e
				output	=	tup.ODataEncrypt('{"result":"INT_ERROR"}')
				status = '500 Internal Server Error'

		except Exception,e:
			print e
			output	=	tup.ODataEncrypt('{"result":"INT_ERROR"}')
			status = '500 Internal Server Error'

	elif query.has_key("method"):
		'''
			Outras chamadas de API
		'''
		query["method"] = query["method"][0]
		if query["method"] == "antenas" and query.has_key("lat1") and query.has_key("lon1") and query.has_key("lat2") and query.has_key("lon2") and query.has_key("operator"):
			'''
				Retornar lista de antenas
			'''
			antenas = hsman.FetchAntenas(query["lat2"][0],query["lon2"][0],query["lat1"][0],query["lon1"][0],query["operator"][0].strip())
			output = json.dumps({"antenas":antenas,"results":len(antenas)})
		
		elif query["method"] == "operators":
			'''
				Retornar lista de operadoras
			'''
			operators	=	hsman.FetchOperators()
			output = json.dumps({"data":operators,"results":len(operators)})

		else:	
			output = '{"result":"INT_ERROR"}'

	elif query.has_key("tile") and query.has_key("operadora"):
		'''
			Retornar tile correspondente
		'''
		tile		=	query["tile"][0]
		op			=	query["operadora"][0]
		tilefile	=	("%s/%s/%s.png"%(config.TILEPATH,op,tile))
		if os.path.exists(tilefile):
			f		=	open(tilefile, "r")
		else:
			f		=	open(config.TILEPATH+"/blank.png")
		data	=	f.read()
		f.close()	
		content = "image/png"
		output	= data
	elif query.has_key("mode") and query.has_key("operadora"):
		'''
			Retornar o logo da operadora"
		'''
		op			=	query["operadora"][0]
		opfile		=	"%s/%s.png" %(config.OPSPATH,op)
		if os.path.exists(opfile):
			f		=	open(opfile, "r")
		else:
			f		=	open(config.OPSPATH+"/none.png", "r")
		data	=	f.read()
		f.close()	
		content = "image/png"
		output	= data

	elif query.has_key("jscript"):
		'''
			Retornar o javascript correspondente"
		'''
		content = "text/javascript"
		jsfile	=	"%s/%s.js" %(config.JSPATH,query["jscript"][0])
		if os.path.exists(jsfile):
			f		=	open(jsfile)
			output = f.read()
			f.close()
			for key, value in config.JSREPLACES.iteritems():
				output	=	output.replace("{"+key+"}",value)
			output = "\n\n" + minify(output, mangle=True)
		else:
			status	=	"404 Not Found"
			output	=	"404 Not Found"

	elif query.has_key("css"):
		'''
			Retornar o css correspondente"
		'''
		content = "text/css"
		jsfile	=	"%s/%s.css" %(config.CSSPATH,query["css"][0])
		if os.path.exists(jsfile):
			f		=	open(jsfile)
			output = f.read()
			f.close()
			for key, value in config.JSREPLACES.iteritems():
				output	=	output.replace("{"+key+"}",value)
			output = cssmin.cssmin(output)
		else:
			status	=	"404 Not Found"
			output	=	"404 Not Found"


	else:
		output	=	tup.ODataEncrypt('{"result":"INT_ERROR"}')
		status = '500 Internal Server Error'

	hsman.DisconnectDB()
	return output, status, content
