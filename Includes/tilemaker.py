#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import tool, MySQLdb, Image, ImageDraw, math, numpy as np, threading, signal, sys, time, manager, os
from datetime import datetime
from scipy import weave
from multiprocessing import Process, Value, Array


donetiles		=	Value('d', 0.0)
stopsignal		=	Value('d', 0.0)

tilesToDo		=	0
threadsperzoom	= 	4

def PrintProgress():
	global doneTiles
	global tilesToDo
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
			if not os.path.isdir(tile[3]):
				os.mkdir(tile[3])
			img.save("%s/%d-%d-%d.png" % (tile[3],tile[0],tile[1],tile[2]), "PNG")
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
		for n in range(threadsperzoom):
			self.__CheckSignal()
			start	=	(self.slicesize*n)
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

	signal.signal(signal.SIGINT, signal_handler)
	print "Conectando ao banco de dados"
	hsman 		= manager.HyperSignalManager()
	hsman.ConnectDB()
	print "Lendo Lista de Operadoras"
	oplist	=	hsman.FetchOperators()
	for operator in oplist:
		donetiles		=	Value('d', 0.0)
		print "Lendo lista de tiles para %s" %operator
		hsman.ConnectDB()
		tilelist,tilesToDo	=	hsman.FetchTilesToDo(operator)
		hsman.DisconnectDB()
		tps			=	[]
		print "Iniciando gerador"
		starttime	=	datetime.now()
		print "Tempo de inicio: "+starttime.ctime()
		for zoom in range(config.HYPER_ZOOM_RANGE[0],config.HYPER_ZOOM_RANGE[1]):
			print "Iniciando ZOOM: %d" %(zoom)
			zp = ZoomProcessor(zoom, tilelist[zoom], donetiles, stopsignal, tps)
			zp.run()

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
