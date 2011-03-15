import sys,re,math,os
from select import select
from SocketServer import TCPServer, StreamRequestHandler

from Stacker import XHProfData, FlashTraceReader, StackTraceMaker

class PolicyHandler(StreamRequestHandler):
	def handle(self):
		l = self.rfile.readline()
		self.wfile.write(open("flashpolicy.xml").read())
		self.wfile.flush()

class FlashTraceHandler(StreamRequestHandler):
	def handle(self):
		xh = XHProfData()
		fr = FlashTraceReader(StackTraceMaker(xh))
		line = True
		sz = 0
		fname = "%s-%d.xhprof" % (self.client_address)
		print "Writing data to %s at the end of session" % fname
		while line:
			line = self.rfile.readline().strip()
			if(line):
				sz += len(line)
				try:
					fr.push(line)
				except Exception as e:
					print "\n\n - error with '%s'\n" % (line)
					print e
					# keep going
				print "Processing .... %d kb\r" % (sz/1024),
		print "\n"
		fp = open(fname, "w")
		xhprof = xh.data()
		print "Writing %d kb to %s\n" % ((len(xhprof)/1024), fname)
		fp.write(xhprof)
		fp.close()

def serve_forever(*servers):
	while True:
		r,w,e = select(servers,[],[],0.5)
		for s in r:
			s.handle_request()

if __name__ == "__main__":

	policy_server = TCPServer(("", 8430), PolicyHandler)
	trace_server = TCPServer(("", 42426), FlashTraceHandler)

	serve_forever(policy_server, trace_server)
