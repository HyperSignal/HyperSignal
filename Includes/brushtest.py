#!/usr/bin/env python
#-*- coding: UTF-8 -*-
import math
HYPER_BRUSH		=	2						#	Tamanho do Brush de interpolação local 
HYPER_BRUSH_INT	=	[	
						[1,1,1,1,1],		#	|
						[1,1,1,1,1],		#	|
						[1,1,1,1,1],		#	|---- Brush de Interpolação
						[1,1,1,1,1],		#	|
						[1,1,1,1,1]			#	|
					]					

def ProcessSignal(self,lat,lon,value,operator,weight=1.0):
	value	=	int(value)

	bx = HYPER_BRUSH-1
	by = HYPER_BRUSH-1	

	outbrush = 	[	[0,0,0,0,0],
					[0,0,0,0,0],
					[0,0,value,0,0],
					[0,0,0,0,0],
					[0,0,0,0,0]
				]
	def OutBrushAdd(ob,x,y,val,w):
		ob[y][x] = int(val if ob[y][x] == 0 else ob[y][x]*(1.0-w) + val*w)
		return ob

	for i in range(0,HYPER_BRUSH+2):
		for j in range(0,HYPER_BRUSH+2):
			w 	= 	(1 - math.sqrt((i-1)*(i-1)+(j-1)*(j-1))/math.sqrt(2*HYPER_BRUSH*HYPER_BRUSH)) * weight
			wi	=	(1 - (i-1)/math.sqrt(2*HYPER_BRUSH*HYPER_BRUSH)) * weight
			wj	=	(1 - (j-1)/math.sqrt(2*HYPER_BRUSH*HYPER_BRUSH)) * weight

			
			#outbrush = OutBrushAdd(outbrush,bx,by,value,wj);

			if	HYPER_BRUSH_INT[by][bx+j] == 1:
				outbrush = OutBrushAdd(outbrush,bx+j,by,value,wj);
			if	HYPER_BRUSH_INT[by+i][bx] == 1:
				outbrush = OutBrushAdd(outbrush,bx,by+i,value,wi);
			
			if	HYPER_BRUSH_INT[by-i][bx] == 1:
				outbrush = OutBrushAdd(outbrush,bx,by-i,value,wi);
			if	HYPER_BRUSH_INT[by][bx-j] == 1:
				outbrush = OutBrushAdd(outbrush,bx-j,by,value,wi);


			if	HYPER_BRUSH_INT[by+i][bx-j] == 1:
				outbrush = OutBrushAdd(outbrush,bx-j,by+i,value,w);
			if	HYPER_BRUSH_INT[by-i][bx+j] == 1:
				outbrush = OutBrushAdd(outbrush,bx+j,by-i,value,w);

			if	HYPER_BRUSH_INT[by-i][bx-j] == 1:
				outbrush = OutBrushAdd(outbrush,bx-j,by-i,value,w);
			if	HYPER_BRUSH_INT[by+i][bx+j] == 1:
				outbrush = OutBrushAdd(outbrush,bx+j,by+i,value,w);

	for i in outbrush:
		print i

print ProcessSignal(None,0,0,2,"")
