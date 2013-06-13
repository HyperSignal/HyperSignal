#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import math
import json
import urllib
import sys
import getopt
import os
import numpy as np
from scipy import weave

API_KEY			=	""
IMAGE_SIZE		=	(640,640)
DEFAULT_ZOOM	=	16
SCALE			=	2

tileSize = 256

initialResolution = 2 * math.pi * 6378137 / tileSize

originShift = 2 * math.pi * 6378137 / 2.0

'''
	Exceptions Handlers
'''

def PrintExcp(where, data, exception):
	stck = (where, exception, sys.exc_info()[2].tb_lineno, os.path.basename(sys.exc_info()[2].tb_frame.f_code.co_filename), sys.exc_info()[0].__name__, data)
	print "HyperSignal - {0} error: {1}\n--On Line {2} of {3}\n--Type: {4}\n--Data: {5}".format(*stck)


class HSLogger(object):
	def __init__(self, filename="hs.log"):
		directory = os.path.dirname(filename)
		if not os.path.exists(directory):
			os.makedirs(directory)
		#self.terminal = sys.stdout
		self.log = open(filename, "a")

	def write(self, message):
		#self.terminal.write(message)
		self.log.write(message)

'''
	Weaver
'''
def	WeaveBilinear(indata, target_width, target_height, in_width, in_height):
	'''
		Faz a escala de in(w,h) para target(w,h) usando interpolação bilinear
		indata uma matriz numpy com tipo uint8
	'''
	mx		=	in_width
	my		=	in_height
	mapdx	=	target_width
	mapdy	=	target_height
	outimg = np.zeros( (mapdy,mapdx,4), dtype=np.uint8)
	baseimg	=	indata

	bilinear = """
		int val(PyArrayObject* data_array, int x, int y, int z)	{
			npy_intp* Ndata = data_array->dimensions;
			npy_intp* Sdata = data_array->strides;
			npy_ubyte* datap = (npy_ubyte*) (data_array->data + x*Sdata[0] + y*Sdata[1] + z*Sdata[2]);
			return *datap;
		}
		int	bilinear(PyArrayObject* data, double x, double y, int offset)	{	
			int rx = floor(x);
			int ry = floor(y);
			double fracX = x - rx;
			double fracY = y - ry;
			double invfracX = 1 - fracX;
			double invfracY = 1 - fracY;
			return ( val(data,rx,ry,offset) * invfracX + val(data,rx+1,ry,offset) * fracX) * invfracY + ( val(data,rx,ry+1,offset) * invfracX + val(data,rx+1,ry+1,offset) * fracX) * fracY;
		}
	"""

	bilexpr = '''
	double px = mapdx;
	double py = mapdy;
	for(int y=0;y<mapdy;y++)	{
		for(int x=0;x<mapdx;x++)	{

			double x2 = ( x / px) * (mx-1);
			double y2 = ( y / py) * (my-1);
			OUTIMG3(x,y,0) = bilinear(baseimg_array,x2,y2,0);
			OUTIMG3(x,y,1) = bilinear(baseimg_array,x2,y2,1);
			OUTIMG3(x,y,2) = bilinear(baseimg_array,x2,y2,2);
			OUTIMG3(x,y,3) = bilinear(baseimg_array,x2,y2,3);
		}
	}
	'''
	weave.inline(bilexpr, ["baseimg", "outimg", "mapdx", "mapdy", "mx", "my"], support_code = bilinear)
	
	return outimg

'''
	Coordinate Tools
'''

def LatLonToMeters(lat, lon ):
        "Converts given lat/lon in WGS84 Datum to XY in Spherical Mercator EPSG:900913"

        mx = lon * originShift / 180.0
        my = math.log( math.tan((90 + lat) * math.pi / 360.0 )) / (math.pi / 180.0)

        my = my * originShift / 180.0
        return mx, my

def MetersToTile(mx, my, zoom):
        "Returns tile for given mercator coordinates"

        px, py = MetersToPixels( mx, my, zoom)
        return PixelsToTile( px, py)

def MetersToPixels(mx, my, zoom):
        "Converts EPSG:900913 to pyramid pixel coordinates in given zoom level"

        res = Resolution( zoom )
        px = (mx + originShift) / res
        py = (my + originShift) / res
        return px, py

def Resolution( zoom ):
        "Resolution (meters/pixel) for given zoom level (measured at Equator)"

        # return (2 * math.pi * 6378137) / (self.tileSize * 2**zoom)
        return initialResolution / (2**zoom)

def PixelsToTile(px, py):
        "Returns a tile covering region in given pixel coordinates"

        tx = int( math.ceil( px / float(tileSize) ) - 1 )
        ty = int( math.ceil( py / float(tileSize) ) - 1 )
        return tx, ty

def TileLatLonBounds(tx, ty, zoom ):
        "Returns bounds of the given tile in latutude/longitude using WGS84 datum"

        bounds = TileBounds( tx, ty, zoom)
        minLat, minLon = MetersToLatLon(bounds[0], bounds[1])
        maxLat, maxLon = MetersToLatLon(bounds[2], bounds[3])

        return ( minLat, minLon, maxLat, maxLon )

def TileBounds(tx, ty, zoom):
        "Returns bounds of the given tile in EPSG:900913 coordinates"

        minx, miny = PixelsToMeters( tx*tileSize, ty*tileSize, zoom )
        maxx, maxy = PixelsToMeters( (tx+1)*tileSize, (ty+1)*tileSize, zoom )
        return ( minx, miny, maxx, maxy )

def PixelsToMeters(px, py, zoom):
        "Converts pixel coordinates in given zoom level of pyramid to EPSG:900913"

        res = Resolution( zoom )
        mx = px * res - originShift
        my = py * res - originShift
        return mx, my

def MetersToLatLon(mx, my ):
        "Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84 Datum"

        lon = (mx / originShift) * 180.0
        lat = (my / originShift) * 180.0
        lat = 180 / math.pi * (2 * math.atan( math.exp( lat * math.pi / 180.0)) - math.pi / 2.0)
        return lat, lon


def GetTileBounds(lat, lon, zoom):
        mx, my = LatLonToMeters( lat, lon )
        tminx, tminy = MetersToTile( mx, my, zoom )
        return TileLatLonBounds( tminx, tminy, zoom )

def TruncSix(val):
	return math.ceil(val*1e6)/1e6

def GetMetersPerPixel(tileBounds,zoom):
	M1 = LatLonToMeters(tileBounds[0], tileBounds[1])
	M2 = LatLonToMeters(tileBounds[2], tileBounds[3])
	PX = MetersToPixels(M1[0], M1[1], zoom)
	PY = MetersToPixels(M2[0], M2[1], zoom)
	X = [ TruncSix(M2[0] - M1[0]), TruncSix(M2[1] - M1[1]) ]
	P = [ int(math.ceil(PY[0] - PX[0])), int(math.ceil(PY[1] - PX[1])) ]
	return [ X[0]/P[0] , X[1] / P[1] ]

def GetTilePixelSize(tileBounds,zoom):
	M1 = LatLonToMeters(tileBounds[0], tileBounds[1])
	M2 = LatLonToMeters(tileBounds[2], tileBounds[3])
	PX = MetersToPixels(M1[0], M1[1], zoom)
	PY = MetersToPixels(M2[0], M2[1], zoom)
	P = [ int(math.ceil(PY[0] - PX[0])), int(math.ceil(PY[1] - PX[1])) ]
	return [ P[0] , P[1] ]

def GetTilePixelBounds(tileBounds,zoom):
	M1 = LatLonToMeters(tileBounds[0], tileBounds[1])
	M2 = LatLonToMeters(tileBounds[2], tileBounds[3])
	PX = MetersToPixels(M1[0], M1[1], zoom)
	PY = MetersToPixels(M2[0], M2[1], zoom)
	return [ int(math.ceil(PX[0])), int(math.ceil(PX[1])), int(math.ceil(PY[0])), int(math.ceil(PY[1])) ]

def LatLonToPixels(lat, lon, zoom):
	meters = LatLonToMeters(lat, lon)
	pp = MetersToPixels(meters[0], meters[1], zoom)
	return [ int(pp[0]), int(pp[1]) ]

def GoogleTile(lat, lon, zoom):	
	mx, my = LatLonToMeters( lat, lon )
	tx, ty = MetersToTile( mx, my, zoom )
	return zoom, tx, (2**zoom - 1) - ty

def GetTile(lat, lon, zoom):
	mx, my = LatLonToMeters( lat, lon )
	tx, ty = MetersToTile( mx, my, zoom )
	return tx, ty

def GoogleTile2Tile(z,x,y):
	return ( x, -y + 2 ** z -1 )

def GoogleTileLatLonBounds(z,x,y):
	tx,ty = GoogleTile2Tile(z,x,y)
	return TileLatLonBounds(tx, ty, z)

def TileLatLonPerPixel(tx,ty,zoom):
	bounds	=	TileLatLonBounds(tx, ty, zoom );
	#boundsm	=	(	LatLonToMeters(bounds[0], bounds[1] ), LatLonToMeters(bounds[2], bounds[3] ) )
	deltax	=	bounds[2] - bounds[0]	#	Latitude
	deltay	=	bounds[3] - bounds[1]	#	Longitude
	return	(	deltax / tileSize, deltay / tileSize )

'''
	URL STATIC MAPS: (center,zoom,maptype)
	center	=>	Map Center
	zoom	=>	Map Zoom
	maptype	=>	(roadmap, satellite, hybrid or terrain)

		URL_STATIC_MAPS_CONTRAST	=>	Contrasted Map
'''
URL_STATIC_MAPS				=	"http://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%s&size="+str(IMAGE_SIZE[0])+"x"+str(IMAGE_SIZE[1])+"&maptype=%s&sensor=false&scale="+str(SCALE)+"&key=" + API_KEY # (center,zoom,maptype)

URL_STATIC_MAPS_CONTRAST	=	"style=feature:landscape|element:geometry|lightness:-100&style=element:labels|visibility:off&style=feature:road|element:geometry|lightness:100"
URL_STATIC_MAPS_MARKER		=	"markers=color:blue|%s"

'''
	URL MAPS API:

'''
URL_MAPS_API	=	"http://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=false"

def GetURL(url):
	"""
		Get URL Content
	"""
	handler	= 	urllib.urlopen(url)
	return	handler.read()

def GetMap(center,zoom,maptype, filename, markers=[]):
	"""
		Gets the map from Google given center,zoom and maptype and saves to filename.
		Markers format: (lat,lng)
	"""
	center = "%s,%s" %(center[0],center[1])
	url = URL_STATIC_MAPS	% (center,zoom,maptype)
	for marker in markers:
		markcoord = "%s,%s" % marker
		url = url + "&"+ (URL_STATIC_MAPS_MARKER % markcoord)
	image=urllib.URLopener()
	image.retrieve(url, filename)

def GetContrastMap(center,zoom, filename):
	"""
		Gets the contrast map from Google given center,zoom  and saves to filename.
	"""
	center = "%s,%s" % (center[0],center[1])
	url = (URL_STATIC_MAPS	% (center,zoom,"roadmap") ) +"&"+URL_STATIC_MAPS_CONTRAST
	image=urllib.URLopener()
	image.retrieve(url, filename)


def GetAddressCoordinates(address):
	"""
		Get the address Lat/Lon
	"""
	url = URL_MAPS_API %(urllib.quote_plus(address))
	data	=	GetURL(url)
	data	=	json.loads(data)

	return (	data['results'][0]['geometry']['location']['lat'],data['results'][0]['geometry']['location']['lng'] )



def GetCenter(coord1, coord2):
	"""
		Return the medium of the two coordinates
	"""
	return ( (coord1[0]+coord2[0])/2, (coord1[1]+coord2[1])/2 )

def	IsInMap(coordloc, mapcoord):
	"""
		Check if the coordinates are on map
	"""
	s =  (coordloc[0]	-	mapcoord[0], mapcoord[3] - coordloc[1])
	return coordloc[0] > mapcoord[0] and coordloc[1] > mapcoord[1] and coordloc[0] < mapcoord[2] and coordloc[1] < mapcoord[3]

def FindZoom(coord1, coord2):
	"""
		Iterates the zoom, to find minimum zoom level to fit the two points in the image.
		Returns zoom level.
	"""
	center	=	GetCenter(coord1,coord2)
	definitivezoom	=	19
	tilecenter		=	LatLonToMeters(	center[0], center[1]	)
	coord1loc		=	LatLonToMeters(	coord1[0], coord1[1]	)
	coord2loc		=	LatLonToMeters(	coord2[0], coord2[1]	)
	for zoom in range(19,0,-1):	#	Google Maps Zoom Range from 0 to 19
		res	= Resolution(zoom)	
		tilesize		=	(	IMAGE_SIZE[0]*res* SCALE, IMAGE_SIZE[1]*res	* SCALE)
		mapcoord		=	(	tilecenter[0] - (tilesize[0]/2), tilecenter[1] - (tilesize[1]/2), tilecenter[0] + (tilesize[0]/2), tilecenter[1] + (tilesize[1]/2) 	)
		if IsInMap(coord1loc, mapcoord) and  IsInMap(coord2loc, mapcoord):
			definitivezoom = zoom
			break
	return definitivezoom - 1
		

def LocateInMap(coordloc, mapcoord, resolution):
	"""
		Locates the point in map given also Map Coordinates and Resolution.
		Returns the X,Y pixel coordinates
	"""
	return ( int(round(( coordloc[0]	-	mapcoord[0]) / resolution)), int(round((mapcoord[3] - coordloc[1]) / resolution)) )

def LocateLatLng(lat,lng,zoom, center):
	"""
		Locate an Latitude and Longitude on a map image given also zoom and center.
		Returns an tuple with pixel coordinates, or False if not in image.
	"""
	resolution		=	Resolution( zoom ) 	/ SCALE						#	Meters/Pixel
	tilesize		=	(	IMAGE_SIZE[0]*resolution* SCALE, IMAGE_SIZE[1]*resolution* SCALE	)	#	Width,Height
	tilecenter		=	LatLonToMeters(	center[0], center[1] )				#	On Global Position
	mapcoord		=	(	tilecenter[0] - (tilesize[0]/2), tilecenter[1] - (tilesize[1]/2), tilecenter[0] + (tilesize[0]/2), tilecenter[1] + (tilesize[1]/2) 	)	#	X0,Y0,X1,Y1 in meters
	coordloc		=	LatLonToMeters(	lat, lng	)
	return LocateInMap(coordloc, mapcoord, resolution)

def MapLatLngBounds(zoom, center):
	resolution		=	Resolution( zoom ) 	/ SCALE						#	Meters/Pixel
	tilesize		=	(	IMAGE_SIZE[0]*resolution* SCALE, IMAGE_SIZE[1]*resolution* SCALE	)	#	Width,Height
	tilecenter		=	LatLonToMeters(	center[0], center[1] )				#	On Global Position
	p1				=	MetersToLatLon(tilecenter[0] - (tilesize[0]/2), tilecenter[1] - (tilesize[1]/2) )
	p2				=	MetersToLatLon(tilecenter[0] + (tilesize[0]/2), tilecenter[1] + (tilesize[1]/2) )
	return (p1,p2)

def hsv2rgb(h, s, v):
    h = float(h)
    s = float(s)
    v = float(v)
    h60 = h / 60.0
    h60f = math.floor(h60)
    hi = int(h60f) % 6
    f = h60 - h60f
    p = v * (1 - s)
    q = v * (1 - f * s)
    t = v * (1 - (1 - f) * s)
    r, g, b = 0, 0, 0
    if hi == 0: r, g, b = v, t, p
    elif hi == 1: r, g, b = q, v, p
    elif hi == 2: r, g, b = p, v, t
    elif hi == 3: r, g, b = p, q, v
    elif hi == 4: r, g, b = t, p, v
    elif hi == 5: r, g, b = v, p, q
    r, g, b = int(r * 255), int(g * 255), int(b * 255)
    return r, g, b
