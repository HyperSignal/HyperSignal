#-*- coding: UTF-8 -*-

import tool, manager, theupcrypter, config,json,urllib


tup = theupcrypter.TheUpCrypter(config.STKEY, config.STIV)

data = {"metodo":"addsinal","lat":-23.547917057611542, "lon": -46.639344692230225, "op" : "AEIO", "sig" : 15, "uid" : 0}
odata = json.dumps(data)
odata = tup.ODataEncrypt(odata);
print "Encrypted: %s" %odata
print tup.ODataDecrypt(odata)
#print urllib.quote_plus(tup.ODataEncrypt(odata))
