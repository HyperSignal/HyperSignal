import base64
import StringIO
import zlib
from Crypto.Cipher import AES

BLOCK_SIZE = 16

class TheUpCrypter:

	def __init__(self, key, iv):
		self.key = key
		self.iv  = iv

	def ODataEncrypt(self,string):
		'''
			Encripta uma string para OData
		'''
		encdata		=	self.__Encrypt(string)
		compressed	=	self.__Compress(encdata)
		return	base64.b64encode(compressed)

	def ODataDecrypt(self,string):
		'''
			Desencripta uma String OData
		'''
		data		=	base64.b64decode(string)
		uncompressed=	self.__UnCompress(data)
		return self.__Decrypt(uncompressed)

	def __Compress(self, string):
		'''
			Comprime a string em gzip
		'''
		return string.encode('zlib')

	def __UnCompress(self, string):
		'''
			Descomprime a string em gzip
		'''
		return zlib.decompress(string)

	def __Encrypt(self, string):
		'''
			Criptografa string em AES-RIJNDAEL-128 CBC
		'''
		padded_text	=	string + (BLOCK_SIZE - len(string) % BLOCK_SIZE) * '\0'

		decryptor = AES.new(self.key, AES.MODE_CBC, self.iv)
		data = decryptor.encrypt(padded_text)

		return data

	def __Decrypt(self, code):
		'''
			Descriptografa string em AES-RIJNDAEL-128 CBC
		'''
		decryptor = AES.new(self.key, AES.MODE_CBC, self.iv)
		data = decryptor.decrypt(code).split('\x00', 1)[0]
		return data
