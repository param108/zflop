import sys,re,math,os
from select import select
from SocketServer import TCPServer, StreamRequestHandler

from Stacker import XHProfData, FlashTraceReader, StackTraceMaker, FlashRecordReader
from MultipartPostHandler import MultipartPostHandler
import urllib2 
import webbrowser

XHPROF_UPLOAD_URL = "http://zperfmon-01.ec2.zynga.com/zprof/uploader.php"

class PolicyHandler(StreamRequestHandler):
	def handle(self):
		l = self.rfile.readline()
		self.wfile.write(open("flashpolicy.xml").read())
		self.wfile.flush()

class FlashHandler(StreamRequestHandler):
	def handle(self):
		xh = XHProfData()
		#fr = FlashTraceReader(StackTraceMaker(xh))
		fr = FlashRecordReader(xh)
		line = True
		sz = 0
		fname = "%s-%d.xhprof" % (self.client_address)
		print "Writing data to %s at the end of session" % fname
		log = open("out.txt", "w")
		try:
			while line:
				line = self.rfile.readline().strip()
				if(line):
					log.write(line+"\n")
					sz += len(line)
					try:
						fr.push(line)
					except Exception as e:
						print "\n\n - error with '%s'\n" % (line)
						print e
						# keep going
					print "Processing .... %d kb\r" % (sz/1024),
					sys.stdout.flush()
		except:
			print "Error, continuing to dump profile anyway"
		print "\n"
		log.close()
		fp = open(fname, "w")
		xhprof = xh.data()
		print "Writing %d kb to %s\n" % ((len(xhprof)/1024), fname)
		fp.write(xhprof)
		fp.close()
		print "Uploading data to %s ... " % (XHPROF_UPLOAD_URL)
		sys.stdout.flush()
		self.upload(fname)
	
	def upload(self, fname):
		try:
			opener = urllib2.build_opener(MultipartPostHandler)
			params = { "filename": fname, "uploaded_file" : open(fname, "rb")}
			o = opener.open(XHPROF_UPLOAD_URL, params)
			url = o.read()
			if(url[:4] == "http"):
				print "Visit %s " % url
				webbrowser.open(url)
		except:
			print "Upload failed. Please upload manually to %s" % (XHPROF_UPLOAD_URL)

def serve_forever(*servers):
	print "\n".join(["Listening on %s:%d" % s.server_address for s in servers])

	while True:
		r,w,e = select(servers,[],[],0.5)
		for s in r:
			s.handle_request()

if __name__ == "__main__":

	policy_server = TCPServer(("", 843), PolicyHandler)
	trace_server = TCPServer(("", 42426), FlashHandler)

	serve_forever(policy_server, trace_server)
