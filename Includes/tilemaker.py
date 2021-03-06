#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import tool, MySQLdb, Image, ImageDraw, math, numpy as np, threading, signal, sys, time, manager, os,config, tweepy
from datetime import datetime
from scipy import weave
from multiprocessing import Process, Value, Array


donetiles		=	Value('d', 0.0)
stopsignal		=	Value('d', 0.0)

tilesToDo		=	0
threadsperzoom	= 	6

tilesetdones	=	0

def PrintProgress():
	global doneTiles
	global tilesToDo
	if tilesToDo == 0:
		sys.stdout.write("\rNada a fazer.")
		sys.stdout.flush()
	else:
		progress = int(( float(donetiles.value) / tilesToDo) * 100);
		sys.stdout.write("\rProgresso %d%% - Tiles feitos %d - Total %d " % (progress,donetiles.value,tilesToDo))
		sys.stdout.flush()


class TileMaker:
	
	def __init__(self, tilelist, tilesdone, stopsignal):
		self.tilelist	=	tilelist
		self.tilesdone	=	tilesdone
		self.stopsignal	=	stopsignal
		self.hsman		=	manager.HyperSignalManager()
	def __CheckSignal(self):
		if self.stopsignal.value > 0:
			sys.exit(0)
	
	def run(self):
		self.hsman.ConnectDB()
		for tile in self.tilelist:
			self.__CheckSignal()
			img = self.hsman.GenerateGoogleTile(tile[0],tile[1],tile[2], tile[3])
			if not os.path.isdir("tiles/%s"%tile[3]):
				os.mkdir("tiles/%s"%tile[3])
			img.save("tiles/%s/%d-%d-%d.png" % (tile[3],tile[0],tile[1],tile[2]), "PNG")
			self.hsman.RemoveTileToDo(tile[0],tile[1],tile[2], tile[3])
			self.hsman.CommitToDB()
			self.tilesdone.value = self.tilesdone.value + 1
		self.hsman.DisconnectDB()

def InitTile(tilelist,	tilesdone, stopsignal):
	tilp = TileMaker(tilelist,tilesdone,stopsignal)
	tilp.run()
	
class ZoomProcessor:
	
	def __init__(self, zoom, tiles, donetiles, stopsignal, tps):
		global threadsperzoom
		self.tiles = tiles
		self.zoom = zoom
		self.donetiles = donetiles
		self.stopsignal = stopsignal
		self.tps = tps
		self.slicesize = int(math.ceil(len(tiles) / threadsperzoom ))

	def __CheckSignal(self):
		if self.stopsignal.value > 0:
			sys.exit(0)

	def __PrintOut(self, msg):
		print	"PZoom(%d): %s" %(self.zoom,msg)

	def run(self):
		global threadsperzoom
		if len(self.tiles) == 0:
			self.__PrintOut("Nenhum tile a fazer")
		else:
			if self.slicesize == 0:
				self.__CheckSignal()
				p = Process(target=InitTile, args=( self.tiles, self.donetiles, self.stopsignal))
				p.start()
				self.tps.append(p)	
				self.__PrintOut("Rodando")
			else:			
				for n in range(threadsperzoom):
					self.__CheckSignal()
					start		=	(self.slicesize*n)
					end		=	(self.slicesize*(n+1))
					p = Process(target=InitTile, args=( self.tiles[start:end], self.donetiles, self.stopsignal))
					p.start()
					self.tps.append(p)
				self.__PrintOut("Rodando")

def signal_handler(signal, frame):
	print 'Você apertou Ctrl + C! Fechando todas as threads!'
	stopsignal.value = 1
	sys.exit(0)

if __name__ == '__main__':
	firsttime	=	datetime.now()	
	signal.signal(signal.SIGINT, signal_handler)
	print "Conectando ao banco de dados"
	hsman 		= manager.HyperSignalManager()
	hsman.ConnectDB()
	print "Lendo Lista de Operadoras"
	oplist	=	hsman.FetchOperators()
	print oplist
	for operator in oplist:
		donetiles		=	Value('d', 0.0)
		print "Lendo lista de tiles para %s" %operator
		hsman.ConnectDB()
		tilelist,tilesToDo	=	hsman.FetchTilesToDo(operator)
		hsman.DisconnectDB()
		tps			=	[]
		if tilesToDo == 0:
			print "Nenhum tile para fazer para operadora %s" %operator
		else:
			print "Iniciando gerador"
			starttime	=	datetime.now()
			print "Tempo de inicio: "+starttime.ctime()
			for zoom in range(config.HYPER_ZOOM_RANGE[0],config.HYPER_ZOOM_RANGE[1]):
				if len(tilelist[zoom]) != 0:
					print "Iniciando ZOOM: %d" %(zoom)
					zp = ZoomProcessor(zoom, tilelist[zoom], donetiles, stopsignal, tps)
					zp.run()
				else:
					print "Nenhum tile a fazer no ZOOM %d" %(zoom)
			while True:
				ok = True
				PrintProgress()	
				for p in tps:
					ok = ok &  (not p.is_alive())
				if ok:	
					print "ACABOU(?)"
					break
				time.sleep(0.1)	

			endtime = datetime.now()
			print "Tempo de fim: "+endtime.ctime()
			print "Tempo decorrido: "+(endtime-starttime).__str__()
			tilesetdones += tilesToDo
	lasttime	=	datetime.now()
	print "Tempo decorrido total: "+(lasttime-firsttime).__str__()	
	#auth = tweepy.OAuthHandler(config.TW_CONSUMER_KEY, config.TW_CONSUMER_SECRET)
	#auth.set_access_token(config.TW_ACCESS_KEY, config.TW_ACCESS_SECRET)
	#api = tweepy.API(auth)
	#api.update_status("Tiles Updated! Tiles made: %s - Time elapsed: %s #signaltracker #cellphones" %(tilesetdones,(lasttime-firsttime).__str__()))
