from distutils.core import setup
try:
	import py2exe
except:
	pass

setup(console=['CollectionServer.py'])

