import sys
import re
from phpserialize import dumps as phpserialize
CLOSED="closed"
OPEN="open"
ENTER="enter"
EXIT="exit"

class FunctionData:
	def __init__(self):
		self.called = 0
		self.totalInclTime = 0
		self.totalExclTime = 0
		self.presentInclTime = []
		self.presentExclTime = []
		self.maxInclTime = 0
		self.maxExclTime = 0
		self.parents=[]
		self.children=[]
	# totalInclTime, maxInclTime
	# totalExclTime, maxExclTime
	# parents
	# children
	pass

class ProfileInfo:
	def __init__(self):
		self.pathDict={}
		self.functionDict = {}
		self.thisPath=[]
		self.pathState=CLOSED


	def mungeArray(self, x):
		s = ""
		if len(x) == 0:
			return "None"
		for i in x:
			s = s + "->" + i	
		return s

	def mprint(self):
		print "Name"+","+"count"+","+ "totalInclTime"+","+"totalExclTime"+","+"maxInclTime"+","+"maxExclTime"+","+"parents"+","+"children"
		for i,f in self.functionDict.iteritems():
			print i+","+str(f.called)+","+str(f.totalInclTime)+","+str(f.totalExclTime)+","+str(f.maxInclTime)+","+str(f.maxExclTime)+","+self.mungeArray(f.parents)+","+ self.mungeArray(f.children)
			
	def pprint(self):
		return phpserialize(self.pathDict)
	
	def updatePathDict(self, parent, me, inclTime):
		if not parent :
			funcname = "main()==>"+me
		else:
			funcname = parent+"==>"+me
		if not self.pathDict.has_key(funcname):
			self.pathDict[funcname] = {"ct":0,"wt":0.0}			
		self.pathDict[funcname]["ct"]+=1
		self.pathDict[funcname]["wt"]+=(inclTime/1000.0)

	def handleFunctionExit(self, time, funcname, isException):
		if not self.functionDict.has_key(funcname):
			self.functionDict[funcname] = FunctionData();
		f = self.functionDict[funcname]
		inclTime = int(time) -f.presentInclTime.pop() 
		exclTime = int(time) -f.presentExclTime.pop()
		f.totalInclTime = f.totalInclTime + inclTime
		f.totalExclTime = f.totalExclTime + exclTime
		if f.maxInclTime < inclTime:
			f.maxInclTime = inclTime
		if f.maxExclTime < exclTime:
			f.maxExclTime = exclTime
		# parent.exclTime = parent.inclTime - child.inclTime
		if len(self.thisPath)>0:
			fp=self.functionDict[self.thisPath[len(self.thisPath)-1]];
			fp.presentExclTime[len(fp.presentExclTime)-1]+=inclTime
			if not self.thisPath[len(self.thisPath)-1] in f.parents:
				f.parents.append(self.thisPath[len(self.thisPath)-1])	
			self.updatePathDict(self.thisPath[len(self.thisPath)-1],funcname,inclTime)
		else:
			self.pathState = CLOSED
			self.updatePathDict(None,funcname,inclTime)
	
	def addEvent(self, time, type, funcname, filename, linenum, linedesc):
		if not self.functionDict.has_key(funcname):
			self.functionDict[funcname] = FunctionData();
		f = self.functionDict[funcname]
		if type == ENTER:
			if self.pathState != CLOSED:
				parent = self.thisPath[len(self.thisPath)-1]
				if not parent in f.parents:
					f.parents.append(parent)
				fp=self.functionDict[parent];
				if not funcname in fp.children:
					fp.children.append(funcname)
				# for recursion
			else:
				self.pathState = OPEN
			f.called+=1
			f.presentInclTime.append(int(time))
			f.presentExclTime.append(int(time))
			self.thisPath.append(funcname)
		if type == EXIT:
			if self.pathState != CLOSED:
				shouldBeMe = self.thisPath.pop(); 
				while not shouldBeMe == funcname:
					self.handleFunctionExit(time, shouldBeMe, True)	
					if len(self.thisPath) == 0:
						print "Failing Ayo!:"+str(linedesc)
					shouldBeMe = self.thisPath.pop() 
				self.handleFunctionExit(time, funcname, False)	
			else:
				print "Fail: exit without entry for "+funcname+"\n"
				sys.exit(-1);	

pd = ProfileInfo()
filep = open("flash4-unix.prof","r")
lines=filep.readlines();
profre = re.compile("\[([0-9]*)\] ([Enterxi]*) (.*)")
typeTranslate={"Enter":ENTER, "Exit":EXIT}
num= 0;
for i in lines:
	m = profre.match(i)
	if m:
		g = m.groups()
		pd.addEvent(g[0],typeTranslate[g[1]],g[2],"junk","junk", num)
	else:
		print("Fail: failed to match regexp with:"+i)
		sys.exit(-1)
	num=num+ 1
print pd.pprint()
