import sys, os, cgi, json

sys.stdout = sys.stderr

path	=	"/var/www/hypersignal"
if path not in sys.path:
	sys.path.append("/var/www/hypersignal")
	sys.path.append("/var/www/hypersignal/WebServices")
	sys.path.append("/var/www/hypersignal/Includes")

#print "Path: %s" % sys.path

import tool, manager, theupcrypter, config

def application(environ, start_response):

	output,status = ProcessPage(environ)
	
	response_headers = [('Content-type', 'text/html'),
						('Content-Length', str(len(output)))]
	start_response(status, response_headers)
	return [output]


def	ProcessPage(_SERVER):
	status = '200 OK'
	output = ""
	query	=	cgi.parse_qs(_SERVER["QUERY_STRING"])
	tup 	= 	theupcrypter.TheUpCrypter(config.STKEY,config.STIV)
	hsman	=	manager.HyperSignalManager()
	hsman.ConnectDB()
	if query.has_key("odata"):
		try:
			data	=	json.loads(tup.ODataDecrypt(query["odata"][0]))
			if	data["metodo"]	==	"addsinal":
				'''
					Adicionar sinal
				'''
				try:
					hsman.ProcessSignal(float(data["lat"]),float(data["lon"]),int(data["sig"]),data["op"])
					hsman.AddStatistics("apicall")
					hsman.CommitToDB()
					hsman.AddDevice(data["uid"], data["dev"], data["man"], data["model"], data["brand"], data["and"], data["rel"], data["sig"])
					hsman.CommitToDB()
				except Exception,e:
					print e
				output	=	tup.ODataEncrypt('{"result":"OK"}')

			elif data["metodo"] == "addtorre":
				'''
					Adicionar Torre
				'''
				try:
					hsman.AddAntenna(float(data["lat"]),float(data["lon"]),data["operator"])
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
					hsman.ProcessSignal(float(data["lat"]),float(data["lon"]),int(data["sig"]),data["op"])
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
					hsman.AddAntenna(float(data["lat"]),float(data["lon"]),data["operator"])
					hsman.AddStatistics("apicall")
					hsman.AddStatistics("tts")
					hsman.CommitToDB()
				except Exception,e:
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
	else:
		output	=	tup.ODataEncrypt('{"result":"INT_ERROR"}')
		status = '500 Internal Server Error'
	return output, status
