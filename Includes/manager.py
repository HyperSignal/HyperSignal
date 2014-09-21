#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import tool, MySQLdb, Image, ImageDraw, math, numpy as np, numpy, scipy, scipy.ndimage, config, os, json, re
from scipy import weave

HYPER_STEP		=	0.0005					#	Step usado no banco de dados - 0.0005 dá uma precisão de ~100 metros
HYPER_BRUSH		=	2						#	Tamanho do Brush de interpolação local 
HYPER_GAP			=	5 						#	Gap para interpolação entre tiles
HYPER_BLUR		=	5						#	Blur para suavização de bordas

HYPER_BRUSH_INT	=	[	
						[0	,0	,0	,0	,0],		#	|
						[0	,0.5,0.5,0.5,0],		#	|
						[0	,0.5,1	,0.5,0],		#	|---- Brush de Interpolação
						[0	,0.5,0.5,0.5,0],		#	|
						[0	,0	,0	,0	,0]			#	|
					]						

'''
	Funções para o site
'''

def	LangReplace(text,lang="default"):
	'''
		Troca tags [KEY] por VALUE no texto baseado na linguagem pedida.
	'''
	langfile	=	"%s/%s/keywords.json" %( config.BASEPATH, lang )
	if os.path.exists(langfile):
		f			=	open(langfile)
		langdata	=	f.read()
		f.close();
		langdata	=	json.loads(langdata)
		for key, value in langdata.iteritems():
			pattern = re.compile(re.escape("["+str(key)+"]"), re.IGNORECASE)
			text = pattern.sub(value, text)
	else:
		langfile	=	"%s/default/keywords.json" %( config.BASEPATH )
		f			=	open(langfile)
		langdata	=	f.read()
		f.close();
		langdata	=	json.loads(unicode(langdata, 'utf-8'))
		for key, value in langdata.iteritems():
			pattern = re.compile(re.escape("["+str(key)+"]"), re.IGNORECASE)
			text = pattern.sub(value, text)
	
	return text

'''
	Funções do HyperSignal
'''

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

def OperatorCorrect(operator):
	operator = operator.replace("'","").replace('"',"").replace("`","").replace("´","")
	for key, value in config.OPSREPLACES.iteritems():
		operator	=	operator.replace(key,value)
	return operator

class	HyperSignalManager:

	con				=	None

	def ConnectDB(self):
		self.con = MySQLdb.connect(config.MYHOST,config.MYUSER,config.MYPASS)
		self.con.select_db(config.MYDB)

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
					cdata		=	((block[x, y]/31.0)*120)
					colorrgb 	=	tool.hsv2rgb(cdata,1,0.902)
					tileraw[x,y,0] = colorrgb[0]
					tileraw[x,y,1] = colorrgb[1]
					tileraw[x,y,2] = colorrgb[2]
					tileraw[x,y,3] = 192
		tiledata	=	tool.WeaveBilinear(tileraw.astype('uint8'), newTileSize, newTileSize, w, h)
		#tiledata	=	Image.fromarray(np.array(tileraw, dtype=numpy.uint8), "RGBA").resize((newTileSize,newTileSize), HYPER_FILTER)
		#tiledata	=	np.array(tiledata.getdata(), numpy.uint8).reshape(newTileSize, newTileSize, 4)
		tiledata	=	scipy.ndimage.gaussian_filter(tiledata, (HYPER_BLUR*(z/10.0),HYPER_BLUR*(z/10.0),0))
		return Image.fromarray(np.array(tiledata, dtype=np.uint8), "RGBA").transpose(Image.ROTATE_90).crop((dts,dts,tool.tileSize+dts,tool.tileSize+dts))

	def InsertToDB(self,x,y,value,operator,weight=1.0):
		if value < 32:
			self.cursor = self.con.cursor()
			self.cursor.execute("INSERT INTO datamatrix VALUES (%s,%s,%s,%s) ON DUPLICATE KEY UPDATE `value`=( ((2-%s)*`value`)+(%s*VALUES(`value`)))/2",(x,y,value,operator,weight,weight))

	def	InsertTileToDB(self,z,x,y,operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO tiles VALUES(%s,%s,%s,%s,0) ON DUPLICATE KEY UPDATE `updated`=0", (x,y,z,operator))

	def	InsertOperatorToDB(self,mcc,mnc,name,fullname=""):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `operators` VALUES(%s,%s,%s,%s) ON DUPLICATE KEY UPDATE `fullname`=VALUES(`fullname`)", (mcc,mnc,name,fullname))

	def ProcessSignal(self,lat,lon,value,operator,weight=1.0):
		if operator.strip() == "" or operator == "None" or operator == None or value < 0 or value > 31:
			return 0
		operator = OperatorCorrect(operator)
		value	=	int(value)
		x,	y	=	LatLonToHyper(lat,lon)
		signals	=	[(x,y,value,operator,weight)]
		
		bx = HYPER_BRUSH
		by = HYPER_BRUSH	
		x = x - HYPER_BRUSH / 2
		y = y - HYPER_BRUSH / 2
		for i in range(0,HYPER_BRUSH):
			for j in range(0,HYPER_BRUSH):
				
				if	x > -1:
					if y+i > -1:
						signals.append( (x,y+i,value,operator,HYPER_BRUSH_INT[by+i][bx]*weight) )
					if y-i > -1:
						signals.append( (x,y-i,value,operator,HYPER_BRUSH_INT[by-i][bx]*weight) )

				if	y > -1:
					if	x-j > -1:
						signals.append( (x-j,y,value,operator,HYPER_BRUSH_INT[by][bx-j]*weight) )
					if	x+j > -1:
						signals.append( (x+j,y,value,operator,HYPER_BRUSH_INT[by][bx+j]*weight) )


				if	x-j > -1 and y+i > -1:
					signals.append( (x-j,y+i,value,operator,HYPER_BRUSH_INT[by+i][bx-j]) )
				if	y-i > -1 and x+j > -1:
					signals.append( (x+j,y-i,value,operator,HYPER_BRUSH_INT[by-i][bx+j]) )

				if	x-j > -1 and y-i > -1:
					signals.append( (x-j,y-i,value,operator,HYPER_BRUSH_INT[by-i][bx-j]) )

				if	x+j > -1 and y+i > -1:
					signals.append( (x+j,y+i,value,operator,HYPER_BRUSH_INT[by+i][bx+j]) )

		
		for signal in signals:
			self.InsertToDB(signal[0],signal[1],signal[2],signal[3],signal[4])

		tiles	=	[]
		for zoom in range(config.HYPER_ZOOM_RANGE[0],config.HYPER_ZOOM_RANGE[1]):
			for i in range(HYPER_BRUSH*2+1):
				tiles.append(GetGoogleTileFromHS(x+i,y,zoom))
				tiles.append(GetGoogleTileFromHS(x-i,y,zoom))

				tiles.append(GetGoogleTileFromHS(x,y+i,zoom))
				tiles.append(GetGoogleTileFromHS(x,y-i,zoom))

				tiles.append(GetGoogleTileFromHS(x+i,y-i,zoom))
				tiles.append(GetGoogleTileFromHS(x-i,y+i,zoom))

		for tile in tiles:
			z,x,y = tile
			self.InsertTileToDB(z,x,y,operator)

		self.AddStatistics("signal",len(signals))
		self.AddStatistics("abssignal",1)

		return len(signals)

	def CommitToDB(self):
		self.con.commit()
	
	def AddStatistics(self,stype,count=1):
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO statistics VALUES (%s,%s,CURRENT_DATE()) ON DUPLICATE KEY UPDATE `count`= `count` + VALUES(`count`)", (stype,count))
		 
	def AddUser(self,username,uid,name,email,lastip,city,country):
		print "ADDUSER - %s-%s-%s-%s-%s-%s-%s" %(uid,username,name,email,lastip,city,country)
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `users`(`uid`,`username`,`name`,`email`,`date`,`lastip`,`sentkm`,`city`,`country`,`lastaccess`) VALUES(%s, %s, %s, %s, CURDATE(), %s, 0, %s, %s, NOW()) ON DUPLICATE KEY UPDATE `uid`=`uid`, `username`= VALUES(`username`), `name` = VALUES(`name`), `email` = VALUES(`email`), `date` = `date`, `lastip` = VALUES(`lastip`), `sentkm` = `sentkm`, `city` = VALUES(`city`), `country` = VALUES(`country`), `lastaccess` = NOW()", (uid,username,name,email,lastip,city,country))

	def	IncUserKM(self, uid, val=0.1):
		self.cursor = self.con.cursor()
		self.cursor.execute("UPDATE `users` SET `sentkm` = `sentkm` + %s WHERE `uid` = %s", (val,uid))
	
	def AddAntenna(self,lat,lon,operator):
		operator = OperatorCorrect(operator)
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `antennas` VALUES(%s,%s,%s) ON DUPLICATE KEY UPDATE `lat`=`lat`",(lat,lon,operator))
		self.AddStatistics("tower")

	def AddDevice(self, uid, device, manufacturer, model, brand, android, release, signal):	
		self.cursor = self.con.cursor()
		self.cursor.execute("INSERT INTO `devices` VALUES(%s,%s,%s,%s,%s,%s,%s,%s) ON DUPLICATE KEY UPDATE `signal` = (VALUES(`signal`) + `signal`) / 2", (uid, device, manufacturer, model, brand, android, release, signal))

	def FetchAntenas(self,minlat,minlon,maxlat,maxlon,operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT * FROM antennas	WHERE `lat` >= %s and `lon` >= %s and `lat` < %s and `lon` < %s and `operator` = %s", (minlat,minlon,maxlat,maxlon,operator))
		row		=	self.cursor.fetchone()
		antenas	=	[]
		while row is not None:
			antenas.append({ "lat" : float(row[0]), "lon" : float(row[1]), "operator" : row[2] })
			row = self.cursor.fetchone()
		return antenas

	def FetchOperators(self):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT `operator` FROM `tiles` GROUP BY `operator`")
		operators	=	[]
		row			=	self.cursor.fetchone()
		while row is not None:
			operators.append(row[0])
			row			=	self.cursor.fetchone()
		return operators

	def FetchDayStatistics(self):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT * FROM `statistics` WHERE `date` = CURDATE()")
		statistics	=	{"apicall":0,"tower":0,"signal":0,"tts":0}
		row			=	self.cursor.fetchone()
		while row is not None:
			statistics[row[0]] = int(row[1])
			row			=	self.cursor.fetchone()
		return statistics

	def FetchNumOperators(self):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT COUNT(*) FROM (SELECT `operator` FROM `tiles` GROUP BY `operator`) tbl1 ")
		row		=	self.cursor.fetchone()
		numops = int(row[0])
		return numops
	
	def FetchNumTiles(self):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT COUNT(*) FROM `tiles`")
		row		=	self.cursor.fetchone()
		numtiles = int(row[0])
		return numtiles
		
	def FetchTilesToDo(self,operator,alltiles=False):
		self.cursor = self.con.cursor()
		if alltiles:
			self.cursor.execute("SELECT * FROM `tiles` WHERE `operator` = %s", (operator))
		else:
			self.cursor.execute("SELECT * FROM `tiles` WHERE `operator` = %s and `updated` = 0", (operator))
 
		row		=	self.cursor.fetchone()
		tiles	=	{}

		for zoom in range(config.HYPER_ZOOM_RANGE[0],config.HYPER_ZOOM_RANGE[1]):
			tiles[zoom] = []
		numtiles	=	0

		while row is not None:
			tiles[row[2]].append( (row[2],row[0],row[1],row[3]) )
			row = self.cursor.fetchone()
			numtiles	=	numtiles + 1

		return tiles,numtiles

	def FetchOperatorName(self,mcc,mnc):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT * FROM `operators`	WHERE `mcc` = %s and `mnc` = %s", (mcc,mnc))
		row		=	self.cursor.fetchone()
		if row != None:
			return row[2].decode("ISO-8859-1").encode("UTF-8")
		else:
			return str(mcc)+str(mnc)

	def FetchOperatorList(self):
		self.cursor = self.con.cursor()
		self.cursor.execute("SELECT * FROM `operators`")
		data = self.cursor.fetchall()
		newdata = []
		for i in range(len(data)):
			newdata.append((data[i][0],data[i][1],data[i][2],data[i][3].decode("ISO-8859-1").encode("UTF-8")))
		return newdata

	def RemoveTileToDo(self, z, x, y, operator):
		self.cursor = self.con.cursor()
		self.cursor.execute("UPDATE tiles SET `updated` = 1 WHERE x = %s and y = %s and z = %s and operator = %s", (x,y,z,operator))

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

