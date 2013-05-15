#!/usr/bin/env python
#-*- coding: UTF-8 -*-d 

import manager, Image

hsman = manager.HyperSignalManager()
hsman.ConnectDB()
'''
for y in range(20):
	for x in range(20):
		print "%d-%d" %(x,y)
		img = hsman.GenerateGoogleTile(14,6061+y,9288+x, "VIVO")
		img.save("out/14-%d-%d.png" %(6061+y,9288+x), "PNG")
'''
for y in range(5):
	for x in range(5):
		print "%d-%d" %(x,y)
		img = hsman.GenerateGoogleTile(12,1507+y,2314+x, "VIVO")
		img.save("out/12-%d-%d.png" %(1507+y,2314+x), "PNG")
img = hsman.GenerateGoogleTile(12,1517,2324, "VIVO")
img.save("out/12-1517-2324.png", "PNG")
hsman.DisconnectDB()

