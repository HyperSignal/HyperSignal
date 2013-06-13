#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import Image

im = Image.open("levels.png")
for x in range(8):
	for y in range(4):
		siglvl = x*4 + y
		print "Saving image signal_%d.png" % siglvl
		im.crop((x*200,y*200,200+x*200,200+y*200)).save("signal_%d.png" %siglvl);
