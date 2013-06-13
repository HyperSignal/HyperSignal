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


sys.stdout = sys.stderr = tool.HSLogger(config.BASEPATH+"/hslogs/hs.log")

def application(environ, start_response):

	output,status,content = ProcessPage(environ)
	if "text" in content:
		output = output.encode("utf-8")
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
			for k, v in data.iteritems():
				data[k] = v.encode("utf-8").strip() if isinstance(v, str) or isinstance(v, unicode) else data[k]

			if	data["metodo"]	==	"addsinal":
				'''
					Adicionar sinal
				'''
				try:
					if  not data["op"] == "":
						if data.has_key("weight"):
							weight = float(data["weight"])
						else:
							weight = 1.0
						sigs = hsman.ProcessSignal(float(data["lat"]),float(data["lon"]),int(data["sig"]),data["op"], weight)
						hsman.AddStatistics("apicall")
						hsman.CommitToDB()
						if data.has_key("dev"):
							hsman.AddDevice(data["uid"], data["dev"], data["man"], data["model"], data["brand"], data["and"], data["rel"], data["sig"])
						if data.has_key("uid"):
							hsman.IncUserKM(data["uid"], 1)
						hsman.CommitToDB()
						output	=	tup.ODataEncrypt('{"result":"OK"}')
				except Exception,e:
					tool.PrintExcp("AddSinal", (data["lat"],data["lon"],data["sig"],data["op"]), e)
					output	=	tup.ODataEncrypt('{"result":"INTERNAL_ERROR"}')
					status = '500 Internal Server Error'

			elif data["metodo"] == "addtorre":
				'''
					Adicionar Torre
				'''
				try:
					if  not data["op"] == "":
						hsman.AddAntenna(float(data["lat"]),float(data["lon"]),data["op"])
						hsman.AddStatistics("apicall")
						hsman.CommitToDB()
						output	=	tup.ODataEncrypt('{"result":"OK"}')
				except Exception,e:
					tool.PrintExcp("addtorre", (data["lat"],data["lon"],data["op"]), e)
					output	=	tup.ODataEncrypt('{"result":"INTERNAL_ERROR"}')
					status = '500 Internal Server Error'

			elif data["metodo"] == "ttsadd":
				'''
					Adicionar ponto de sinal via TeskeTrackingSystem
				'''
				try:
					if  not data["op"].strip() == "":
						hsman.ProcessSignal(float(data["lat"]),float(data["lon"]),int(data["sig"]),data["op"])
						hsman.AddStatistics("apicall")
						hsman.AddStatistics("tts")
						hsman.CommitToDB()
						output	=	tup.ODataEncrypt('{"result":"OK"}')
				except Exception,e:
					tool.PrintExcp("ttsadd", (data["lat"],data["lon"],data["sig"],data["op"]), e)
					output	=	tup.ODataEncrypt('{"result":"INTERNAL_ERROR"}')
					status = '500 Internal Server Error'

			elif data["metodo"]	==	"ttstoweradd":
				'''
					Adiciona torre via TeskeTrackingSystem
				'''
				try:
					hsman.AddAntenna(float(data["lat"]),float(data["lon"]),data["op"].strip())
					hsman.AddStatistics("apicall")
					hsman.AddStatistics("tts")
					hsman.CommitToDB()
					output	=	tup.ODataEncrypt('{"result":"OK"}')
				except Exception,e:
					tool.PrintExcp("ttstoweradd", (data["lat"],data["lon"],data["op"]), e)
					output	=	tup.ODataEncrypt('{"result":"INTERNAL_ERROR"}')
					status = '500 Internal Server Error'

			elif data["metodo"] == "adduser":
				'''
					Adiciona um usuário
				'''
				try:	
					hsman.AddUser(data["username"],data["uid"],data["name"],data["email"],_SERVER["REMOTE_ADDR"],data["city"],data["country"])
					hsman.AddStatistics("apicall")
					hsman.CommitToDB()
					output	=	tup.ODataEncrypt('{"result":"OK"}')
				except Exception, e:
					tool.PrintExcp("adduser", (data["username"],data["uid"],data["name"],data["email"],_SERVER["REMOTE_ADDR"],data["city"],data["country"]), e)
					output	=	tup.ODataEncrypt('{"result":"INTERNAL_ERROR"}')
					status = '500 Internal Server Error'

			else:
				'''
					Chamada da API errada!
				'''
				try:
					hsman.AddStatistics("apicallerror")
					hsman.CommitToDB()
				except Exception,e:
					tool.PrintExcp("Wrong API Call", (data), e)
				output	=	tup.ODataEncrypt('{"result":"INT_ERROR"}')
				status = '500 Internal Server Error'

		except Exception,e:
			tool.PrintExcp("OData Decode", query["odata"][0], e)
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
		if	query.has_key("lang"):
			lang	=	query["lang"][0]
		else:
			lang	=	"default"

		content		=	"text/javascript"
		jsfile		=	"%s/%s.js" %(config.JSPATH,query["jscript"][0])
		if os.path.exists(jsfile):
			f		=	open(jsfile)
			output = f.read()
			f.close()
			for key, value in config.JSREPLACES.iteritems():
				output	=	output.replace("{"+key+"}",value)
			output	=	manager.LangReplace(output,lang)
			output	=	"\n\n" + minify(output, mangle=True)
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
