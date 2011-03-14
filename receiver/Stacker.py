import sys
import re
from phpserialize import dumps as phpserialize
from collections import defaultdict
from StringIO import StringIO as StringBuffer

class XHProfData(object):
	def __init__(self):
		self.entries = defaultdict(lambda: {"wt":0.0, "ct":0})

	def call(self, parent, child, wt):
		if(not parent):
			parent = "main()"
			self.entries[parent]["wt"] += wt
			self.entries[parent]["ct"] = 1
		key = "%s==>%s" % (parent, child)
		self.entries[key]["wt"] += wt
		self.entries[key]["ct"] += 1
	
	def data(self):
		return phpserialize(self.entries)
		
	def __repr__(self):
		return "<XHProfData of %d functions>" % (len(self.entries),)
	
class FunctionInfo(object):
	__slots__ = ["klass", "start", "func", "recursion", "depth"]
	def __init__(self, start, klass, func):
		self.klass = klass
		self.func = func
		self.start = start
		self.depth = 0
	
	def __eq__(self, v):
		if not v:
			return False
		if isinstance(v, FunctionInfo):
			return (v.klass, v.func) == (self.klass, self.func)
		elif isinstance(v, tuple):
			return v == (self.klass, self.func)
		return False
	def __repr__(self):
		func = self.func
		if(self.recursion):
			func = "%s@%d" % (self.func, self.recursion)
		if(self.klass):	return "%s::%s" % (self.klass, func);
		if(self.func): return func
		
class StackTraceMaker(object):
	def __init__(self, xhprof):
		self.stack = []
		self.current = None
		self.xhprof = xhprof
	
	def push(self, fi):
		recursion = len(filter(lambda x: x == fi, self.stack))
		fi.recursion = recursion
		fi.depth = len(self.stack)
		self.stack.append(fi)
	
	def pop(self):
		return self.stack.pop()
	
	def peek(self):
		if(len(self.stack) == 0): return None;
		return self.stack[-1]
	
	def enterFunction(self, ts, klass, func, pkg):
		fi = FunctionInfo(ts, klass, func)
		self.push(fi)
	
	def exitFunction(self, ts, klass, func, pkg):
		while(True):
			shouldbe = self.pop()
			parent = self.peek()
			wt = 1000*(ts - shouldbe.start)
			self.xhprof.call(parent, shouldbe, wt)
			if(shouldbe == (klass, func)):
				break

class FlashTraceReader(object):
	formatre = re.compile("\[(?P<ts>\\d+)\] (?P<event>Enter|Exit) (?P<func>.*)@[^;]*;(?P<class>.*)")
	def __init__(self, stackmaker):
		self.stackmaker = stackmaker

	def push(self, line):
		m = self.formatre.match(line)
		if(m):
			event_type = m.group("event")
			if(event_type == "Enter"):
				self.stackmaker.enterFunction(int(m.group("ts")), m.group("class"), m.group("func"),"")
			elif(event_type == "Exit"):
				self.stackmaker.exitFunction(int(m.group("ts")), m.group("class"), m.group("func"),"")

xh = XHProfData()
fr = FlashTraceReader(StackTraceMaker(xh))

lines = open("flash4-unix.prof").xreadlines()

for l in lines:
	fr.push(l)

fp = open("x.xhprof", "w")
fp.write(xh.data())
