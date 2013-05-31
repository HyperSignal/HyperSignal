import sys
import tweepy
import config

auth = tweepy.OAuthHandler(TW_CONSUMER_KEY, TW_CONSUMER_SECRET)
auth.set_access_token(TW_ACCESS_KEY, TW_ACCESS_SECRET)
api = tweepy.API(auth)
api.update_status("Server Test 1") #Envia o Tweet
