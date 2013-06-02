#!/usr/bin/env python
#-*- coding: UTF-8 -*-

import config, manager, tweepy

hsman = manager.HyperSignalManager()
hsman.ConnectDB()

statistics	=	hsman.FetchDayStatistics()
numops		=	hsman.FetchNumOperators()
numtiles	=	hsman.FetchNumTiles()

hsman.DisconnectDB()

auth = tweepy.OAuthHandler(config.TW_CONSUMER_KEY, config.TW_CONSUMER_SECRET)
auth.set_access_token(config.TW_ACCESS_KEY, config.TW_ACCESS_SECRET)
api = tweepy.API(auth)
api.update_status("Day report - Sent Towers: %d , Sent Signals: %d , Sent TTS Signal: %d , Total Operators: %d Total Tiles: %d" %(statistics["tower"], statistics["signal"], statistics["tts"], numops, numtiles))

