import tool, MySQLdb, Image, math, numpy as np, numpy, os, json, re
import subprocess
from time import time

username = "hypersignal"
password = "hs1234"
database = "hypersignal"
host     = "10.0.5.180"

TWIDTH = 256
THEIGHT = 256
TA = 0
TB = 0
TC = TWIDTH
TD = THEIGHT

print "Connecting"
con = MySQLdb.connect(host,username,password)
con.select_db(database)
sumtime = 0

def FetchTilesToDo(operator,alltiles=False):
	cursor = con.cursor()
	if alltiles:
		cursor.execute("SELECT * FROM `tiles` WHERE `operator` = %s AND `z` = 15", (operator))
	else:
		cursor.execute("SELECT * FROM `tiles` WHERE `operator` = %s and `updated` = 0", (operator))

	row		=	cursor.fetchone()
	tiles	=	{}

	for zoom in range(15,16):
		tiles[zoom] = []
	numtiles	=	0

	while row is not None:
		tiles[row[2]].append( (row[2],row[0],row[1],row[3]) )
		row = cursor.fetchone()
		numtiles	=	numtiles + 1

	return tiles,numtiles

def FetchBlock(start,end,operator):
	cursor = con.cursor()
	cursor.execute("SELECT * FROM datamatrix WHERE x >= %s and x < %s and y >= %s and y < %s and operator = %s",(start[0],end[0],start[1],end[1],operator))
	blockdata = cursor.fetchall()
	block = numpy.zeros((end[0]-start[0],end[1]-start[1]),dtype=numpy.uint8)
	block.fill(-1)
	for data in blockdata:
		x			=	int(data[0]) - start[0]
		y			=	int(data[1]) - start[1]
		sig			=	int(data[2])
		block[x,y]	=	sig
	return block



def DoTile(tx,ty,tz,operator):
	global sumtime
	print "Doing tile %s,%s,%s for %s" %(tx,ty,tz,operator)
	start = time()
	xmin,xmax,ymin,ymax		=	tool.GetGoogleTileHSRange(tz,tx,ty)
	dx	=	xmax-xmin
	dy	=	ymax-ymin

	#print "Fetching Block"
	block = FetchBlock( (xmin,ymin),(xmax,ymax), operator)

	#print "Block Shape: %s,%s" %block.shape

	data = bytearray()
	#print "Generating buffer"
	for y in range(0,dy):
		for x in range(0,dx):
			data += chr(block[x,y])


	#print "Opening process and writting"
	proc = subprocess.Popen(['./run.sh', '-tckz', '-r', '%s'%block.shape[0],'-u', '%s'%block.shape[1] , '-w', '%s'%TWIDTH , '-h', '%s'%THEIGHT , '-A', '%s'%TA, '-B', '%s'%TB, '-C', '%s'%TC, '-D', '%s'%TD],stdout=subprocess.PIPE,stdin=subprocess.PIPE)
	proc.stdin.write(data)
	proc.stdin.close()
	result = proc.stdout.read(TWIDTH*THEIGHT*4)
	proc.wait()

	#print "Got %s bytes. Generating image." %(len(result))

	arr = np.fromstring(result, dtype=np.uint8)
	arr = np.reshape(arr, (THEIGHT, TWIDTH, 4))

	image = Image.fromarray(arr)
	image.save("%s/%s-%s-%s-%s.png" % (operator,tz,tx,ty,operator))

	delta = time() - start
	sumtime += delta
	print "Done in %s seconds!" %delta

donetiles = 0
tileslist, numtiles = FetchTilesToDo("VIVO",True)
starttime = time()
print "%s tiles to do" %numtiles
for tile in tileslist[15]:
	DoTile(tile[1],tile[2],tile[0], tile[3])

delta = starttime - time()
sumtime /= numtiles
print "Done %s tiles in %s seconds" %(donetiles,delta)
print "Average of %s per tile" %(sumtime)
con.close()