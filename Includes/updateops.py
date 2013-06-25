#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import manager
hsman = manager.HyperSignalManager()
hsman.ConnectDB()

f = open("../Documents/network operator.list")
data = f.read()
f.close()
data = data.split("\n")			#	Break Lines
data = filter(None, data)		#	Filter blank lines
print "Updating Operators"
count = 0
for line in data:
	if line[0] != "#":		#	Filter Comments
		x = line.split(",")
		hsman.InsertOperatorToDB(x[0],x[1],x[2],x[3].decode("UTF-8"))
		count = count + 1
hsman.CommitToDB()
print "Operators: "+str(count)

