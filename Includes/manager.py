#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import tool, MySQLdb, Image, ImageDraw, math, numpy as np, numpy, scipy, scipy.ndimage
from scipy import weave

HYPER_STEP		=	0.0005	#	Step usado no banco de dados - 0.0005 dá uma precisão de ~100 metros
HYPER_BRUSH		=	2		#	Tamanho do Brush de interpolação local 
HYPER_GAP		=	5		#	Gap para interpolação entre tiles
HYPER_BLUR		=	3		#	Blur para suavização de bordas
HYPER_ZOOM_RANGE=	(12,17)	#	Range de Zoom para o Banco de Dados

def LatLonToHyper(lat,lon):
	'''
		Entra com Latitude e Longitude, retorna x e y nas coordenadas do HyperSignal
	'''
	return (int(math.ceil((lon+180)/HYPER_STEP)), int(math.ceil((lat+90)/HYPER_STEP)))

def HyperToLatLon(x,y):
	'''
		Entra com a coordenada do HyperSignal e retorna latitude,longitude
	'''
	return ( y * HYPER_STEP - 90,  x * HYPER_STEP - 180 )

def GetGoogleTileHSRange(z,x,y):
	'''
		Retorna range do Tile da google nas coordenadas do HyperSignal
		(xmin, xmax, ymin, ymax)
	'''
	bounds = tool.GoogleTileLatLonBounds(z,x,y)
	b1	=	LatLonToHyper(bounds[0],bounds[1])
	b2	=	LatLonToHyper(bounds[2],bounds[3])	
	return ( b1[0], b2[0], b1[1], b2[1] )

def GetGoogleTileFromHS(x,y,zoom):
	'''
		Retorna o z,y,x do tile da google onde o HS(x,y) está.
	'''
	lat, lon	=	HyperToLatLon(x,y)
	return tool.GoogleTile(lat, lon, zoom)

class	HyperSignalManager:
	MYHOST			=	"localhost"
	MYUSER			=	"root"
	MYPASS			=	"***REMOVED***"
	MYDB			=	"hypersignal"
	con				=	None

	def ConnectDB(self):
		self.con = MySQLdb.connect(self.MYHOST,self.MYUSER,self.MYPASS)
		self.con.select_db(self.MYDB)

	def DisconnectDB(self):
		self.con.close()

	def GenerateGoogleTile(self, z, x, y, operator):
		xmin,xmax,ymin,ymax		=	GetGoogleTileHSRange(z,x,y)
		dx	=	xmax-xmin
		dy	=	ymax-ymin
		block					=	self.FetchBlock( (xmin-HYPER_GAP,ymin-HYPER_GAP),(xmax+HYPER_GAP,ymax+HYPER_GAP), operator)
		w,h						=	block.shape
		tileraw					=	numpy.zeros((w,h,4))

		newTileSize	=	int(math.ceil(tool.tileSize + ( 2 * tool.tileSize * HYPER_GAP ) / dx))
		dts			=	( newTileSize - tool.tileSize ) / 2		
		for y in range(h):
			for x in range(w):
				if block[x, y] == -1 or block[x, y] > 31:	
					tileraw[x,y,0] = 128
					tileraw[x,y,1] = 128
					tileraw[x,y,2] = 128
					tileraw[x,y,3] = 0
				else:
					cdata		=	((block[x, y]/32.0)*120)
					colorrgb 	=	tool.hsv2rgb(cdata,1,0.902)
					tileraw[x,y,0] = colorrgb[0]
					tileraw[x,y,1] = colorrgb[1]
					tileraw[x,y,2] = colorrgb[2]
					tileraw[x,y,3] = 192
		tiledata	=	tool.WeaveBilinear(tileraw.astype('uint8'), newTileSize, newTileSize, w, h)
		tiledata	=	scipy.ndimage.gaussian_filter(tiledata, (HYPER_BLUR,HYPER_BLUR,0))
		return Image.fromarray(np.array(tiledata, dtype=np.uint8), "RGBA").transpose(Image.ROTATE_90).crop((dts,dts,tool.tileSize+dts,tool.tileSize+dts))

	def InsertToDB(self,x,y,value,operator):
		if value < 32:
			self.cursor = self.con.cursor()
			self.cursor.execute("INSERT INTO datamatrix VALUES (%s,%s,%s,%s) ON DUPLICATE KEY UPDATE `value`=(`value`+VALUES(`value`))/2",(x,y,value,operator))

	def	InsertTileToDB(self,z,x,y,operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO tiles VALUES(%s,%s,%s,%s) ON DUPLICATE KEY UPDATE `x`=`x`", (x,y,z,operator))

	def ProcessSignal(self,lat,lon,value,operator):
		value	=	int(value)
		x,	y	=	LatLonToHyper(lat,lon)
		signals	=	[(x,y,value,operator)]

		for i in range(1,HYPER_BRUSH+1):
			for j in range(1,HYPER_BRUSH+1):

				signals.append( (x+j,y,value,operator) )
				signals.append( (x,y+i,value,operator) )
				signals.append( (x+j,y+i,value,operator) )

				if x-j > -1:
					signals.append( (x-j,y,value,operator) )
					signals.append( (x-j,y+i,value,operator) )

				if y-i > -1:
					signals.append( (x,y-i,value,operator) )

					if x-j > -1:
						signals.append( (x-j,y-i,value,operator) )

				if x-j > -1:
					signals.append( (x-j,y,value,operator) )
		
		for signal in signals:
			self.InsertToDB(signal[0],signal[1],signal[2],signal[3])

		tiles	=	[]
		for zoom in range(HYPER_ZOOM_RANGE[0],HYPER_ZOOM_RANGE[1]):
			tile1 = GetGoogleTileFromHS(x,y+HYPER_BRUSH,zoom)
			tile2 = GetGoogleTileFromHS(x+HYPER_BRUSH,y,zoom)
			tile3 = GetGoogleTileFromHS(x+HYPER_BRUSH,y+HYPER_BRUSH,zoom)
			tile4 = GetGoogleTileFromHS(x,y-HYPER_BRUSH,zoom)
			tile5 = GetGoogleTileFromHS(x-HYPER_BRUSH,y,zoom)
			tile6 = GetGoogleTileFromHS(x-HYPER_BRUSH,y-HYPER_BRUSH,zoom)
			if tile1 not in tiles:
				tiles.append(tile1)
			if tile2 not in tiles:
				tiles.append(tile2)
			if tile3 not in tiles:
				tiles.append(tile3)
			if tile4 not in tiles:
				tiles.append(tile4)
			if tile5 not in tiles:
				tiles.append(tile5)
			if tile6 not in tiles:
				tiles.append(tile6)

		for tile in tiles:
			z,x,y = tile
			self.InsertTileToDB(z,x,y,operator)

		self.AddStatistics("signal",len(signals))

		return len(signals)

	def CommitToDB(self):
		self.con.commit()
	
	def AddStatistics(self,stype,count=1):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO statistics VALUES (%s,%s,CURRENT_DATE()) ON DUPLICATE KEY UPDATE `count`= `count` + VALUES(`count`)", (stype,count))
		 
	def AddUser(self,username,uid,name,email,lastip,city,country):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `users` VALUES(NULL, %s, %s, %s, %s, CURDATE(), %s, 0, %s, %s, NOW())", (username,uid,name,email,lastip,city,country))

	def	IncUserKM(self, uid, val=0.1):
		self.cursor = self.con.cursor()
		self.cursor.execute("UPDATE `users` SET `sentkm` = `sentkm` + %s WHERE `uid` = %s", (val,uid))
	
	def AddAntenna(self,lat,lon,operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `antennas` VALUES(%s,%s,%s) ON DUPLICATE KEY UPDATE `lat`=`lat`",(lat,lon,operator))
		self.AddStatistics("tower")

	def AddDevice(self, uid, device, manufacturer, model, brand, android, release, signal):	
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `devices` VALUES(%s,%s,%s,%s,%s,%s,%s,%s) ON DUPLICATE KEY UPDATE `signal` = (VALUES(`signal`) + `signal`) / 2", (uid, device, manufacturer, model, brand, android, release, signal))
	
	def FetchTilesToDo(self,operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT * FROM tiles WHERE operator = %s", (operator))
		row		=	self.cursor.fetchone()
		tiles	=	{}

		for zoom in range(HYPER_ZOOM_RANGE[0],HYPER_ZOOM_RANGE[1]):
			tiles[zoom] = []
		numtiles	=	0

		while row is not None:
			tiles[row[2]].append( (row[2],row[0],row[1],row[3]) )
			row = self.cursor.fetchone()
			numtiles	=	numtiles + 1

		return tiles,numtiles

	def FetchBlock(self,start,end,operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT * FROM datamatrix WHERE x >= %s and x < %s and y >= %s and y < %s and operator = %s",(start[0],end[0],start[1],end[1],operator))
		blockdata = self.cursor.fetchall()
		block = numpy.zeros((end[0]-start[0],end[1]-start[1]),dtype=numpy.uint8)
		block.fill(-1)
		for data in blockdata:
			x			=	int(data[0]) - start[0]
			y			=	int(data[1]) - start[1]
			sig			=	int(data[2])
			block[x,y]	=	sig
		return block

