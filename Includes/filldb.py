#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import tool, MySQLdb, Image, ImageDraw, math, numpy as np, manager
from scipy import weave


myhost			=	"voz.com.br"
myuser			=	"signalsampler"
mypass			=	"sig***REMOVED***"
mydb			=	"signalsampler"

print "Conectando SignalTracker"
con = MySQLdb.connect(myhost,myuser,mypass)
con.select_db(mydb)
cursor = con.cursor()
cursor.execute("SELECT * FROM signaldata")
count = 0
wcount = 0
print "Conectando HyperSignal"
hsman = manager.HyperSignalManager()
hsman.ConnectDB()
row = cursor.fetchone()

while row is not None:
	wcount = wcount + hsman.ProcessSignal(row[1],row[2],row[4],row[3])
	count = count + 1
	row = cursor.fetchone()
	#(36L, -23.645076, -46.658583, 'VIVO', 18L, 'SYSTEM')

print "Adicionado %d registros." % count
print "Executando commit."
hsman.CommitToDB()
print "Fechando"
con.close()
hsman.DisconnectDB()
