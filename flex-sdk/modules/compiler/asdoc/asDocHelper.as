/*
 *
 *          ASDocGenGen.as
 *
 *   "ASDocGenGen" is an AS3 script which reads an
 *   XML file exported from asd containing asdoc 
 *   comments and postprocesses it to match the
 *   format IMD's help system expects.
 *
 *   Chris Nuuja
 *   Feb 3, 2004
 *
 */

import avmplus.*;

var verbose:Boolean = false;
var benchmark:Boolean = false;
var refErrors:Boolean = false;

const SCRIPT_NAME = "ASDocGenGen";
const newline = "\n";
const globalPackage = "$$Global$$";

try {
if (verbose) print("Starting asDocHelper");

var inputStr:String = "toplevel.xml";
var outputStr:String = "toplevel_classes.xml";
var configStr:String = "../templates/ASDoc_Config.xml";

if (System.argv.length > 0)
{
    inputStr = System.argv[0]
}
if (System.argv.length > 1)
{
    outputStr = System.argv[1]
}
if (System.argv.length > 2)
{
    configStr = System.argv[2]
}

var startTime:Date = new Date();
var config:XML = XML(File.read(configStr));
var OUTPUT_FILE:String = String(outputStr);
var includeInheritedExcludes:Boolean = false;
var includeInternal:Boolean = false;

var filename:String = inputStr;

if (! inputStr)
{
   print("A filename was not given.  This executable should only be used internally by ASDoc.")
}

if (verbose) print("reading sourcefile: " + filename);

var fileContents:String = File.read(filename);
var rawDocXML:XML = XML(fileContents);
var configRec = new Object();
configRec.includePrivates = config.options.@includePrivate == "true" ? true : false;
verbose =  config.options.@verbose == "true" ? true : verbose;
benchmark =  config.options.@benchmark == "true" ? true : benchmark;
refErrors =  config.options.@refErrors == "true" ? true : refErrors;
var buildNum = config.options.@buildNum;
if (buildNum == undefined)
   buildNum = 0;

var doc =	<asdoc build={buildNum}>
				<link rel="stylesheet" href="style.css" type="text/css" />
			</asdoc>;

XML.prettyPrinting = true;
XML.prettyIndent = 3;

if (verbose) print("configuring namespaces");

var hideNamespaces:Boolean = false;
if (config.hasOwnProperty("namespaces"))
	config.namespaces[0].@hideAll == "true" ? true : false;
var namespaces:String = ":";

if (config.descendants("namespace") != undefined)
{
	for (var i:Number = 0; i < config.namespaces.namespace.length(); i++)
	{
		if (config.namespaces.namespace[i].@hide != undefined)
			namespaces += (config.namespaces.namespace[i].toString() + ":" + config.namespaces.namespace[i].@hide + ":");
	}
}

if (verbose) print("configuring packages");
var hidePackages:Boolean = false;
if (config.hasOwnProperty("packages"))
	config.packages[0].@hideAll == "true" ? true : false;
var hiddenPackages:String = ":";

if (config.descendants("asPackage") != undefined)
{
	for (var i:Number = 0; i < config.packages.asPackage.length(); i++)
	{
		if (config.packages.asPackage[i].@hide != undefined)
			hiddenPackages += (config.packages.asPackage[i].toString() + ":" + config.packages.asPackage[i].@hide + ":");
	}
}
if (verbose) print("done with configuration");

//var classDoc = new XML();

var count = 0;

var classTable  = new Object(); // quick lookup of AClass record for a fullname (aka qualifiedName) of a class
//var simpleNameClassTable = new Object(); // quick lookup given a simple name and package name (not accurate if using namespaces)
var eventTable  = new Object();
var packageContentsTable = new Object();	// table of packages, indexable by package name.  each table element holds an array of all classes in that package
var packageTable   = new Object(); // table of package records, indexable by package name.  This holds all the asDoc comments encountered for a package.
									// todo: deal with multiple definitions of the same package, and packages declared solely through fully qualified class names
									// todo: create a package record which holds both this and the list of classes held in packageContentsTable
									//  and store the entire thing in packageContentsTable instead of using two tables

var bindableTable = new Object();  // table of bindable metadata which wasn't handled in processFields and should be handled in processMethods

// create fake "global" class to hold global methods/props
classTable[globalPackage] = new AClass(); 
classTable[globalPackage].name = globalPackage;
classTable[globalPackage].fullname = globalPackage;
classTable[globalPackage].base_name = "Object";
classTable[globalPackage].decompName = decomposeFullClassName('Object');
classTable[globalPackage].node = <aClass></aClass>;
var packageContents = new Object();
packageContentsTable[globalPackage] = packageContents;
packageContents[globalPackage] = classTable[globalPackage];
if (verbose) print("done with setup");

if (benchmark) { lastTime = startTime; timeSec(lastTime); lastTime = new Date(); }
if (verbose || benchmark) print("preprocessing classes");
var lastTime = startTime;
preprocessClasses(rawDocXML); // create AClass records in classTable for each class. Build basic xml skeleton
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("processing classes");
processClasses(rawDocXML);
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("processing excludes");
processExcludes(rawDocXML);
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("processing fields");
processFields(rawDocXML);
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("processing metadata");
processMetaData(rawDocXML);
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("processing methods");
processMethods(rawDocXML);
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("processing inheritance");
processClassInheritance();
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("assembling classes");
assembleClassXML(); // build xml subtrees for all classeses
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("assembling packages");
assembleClassPackageHierarchy(); // put inner class nodes inside their parent classes, put classes (and top level methods/vars) inside their packages.
if (benchmark) { timeSec(lastTime); lastTime = new Date(); }

if (verbose || benchmark) print("writing output file:  " + OUTPUT_FILE);
File.write(OUTPUT_FILE, doc.toXMLString());

var elapsedTime:Number = (new Date().time - startTime.time)/1000;
if (verbose || benchmark) { print("asDocHelper completed successfully in " + elapsedTime + " seconds"); }

} catch (ex:Error) {
	print("An unexpected error occurred.");
	print(ex.message);
    if (verbose)
    {
		print(ex.getStackTrace());
		for (var foo:String in ex)
		{
			print(foo);
		}
    }
}

/*
var count:int = 0;
for (var z:Object in simpleNameClassTable)
{
print("simpleNameClassTable["+z+"]="+simpleNameClassTable[z]);
count++;
}
print("total="+count);
count = 0;
for (var:Object x in classTable)
{
print("classTable["+x+"]="+classTable[x]);
count++;
}
print("total="+count);
*/

//----------------------------------------------------------------------------------------------------------------//


class AClass {
	var isInnerClass:Boolean;
	var isInterface:Boolean;
	var interfaceStr:String;
    
	var node:XML;
	var name:String;
	var fullname:String;
	var base_name:String;
	var directDecendants:Array;
	var allDecendants:Array;
	var excludedProperties:Array;
	var excludedMethods:Array;
	var excludedEvents:Array;
	var excludedStyles:Array;
	var excludedEffects:Array;
	
	var decompName:QualifiedFullName;
	
	var methodCount:Number;
	var methods:XML;
	
	var constructorCount:Number;
	var constructors:XML;
	
	var fieldCount:Number;
	var fields:XML;
	
	var eventCount:Number;
	var classEvents:XML;
	
	var innerClassCount:Number;
	var innerClasses:Array;
	
	var href:String;
	
	var eventCommentTable:Object;
	var fieldGetSet:Object;
	var privateGetSet:Object;
	var methodOverrideTable:Object;
	
	public function toString()
	{
		return "Class Record for: " + fullname;
	}
	
	public function AClass()
	{
		isInnerClass = false;
		isInterface = false;
		name = "";
		base_name = "";
		fullname = "";
		methodCount = 0;
		fieldCount = 0;
		constructorCount = 0;
		eventCount = 0;
		innerClassCount = 0;
		href = "";
		directDecendants = [];
		allDecendants = null;
		excludedProperties = [];
		excludedMethods = [];
		excludedEvents = [];
		excludedStyles = [];
		excludedEffects = [];
		decompName = null;
		node = null;
		interfaceStr = null;
		innerClasses = null;
		
		eventCommentTable = new Object();
		methodOverrideTable = new Object();
		fieldGetSet = new Object();
		privateGetSet = new Object();
	}
}

class QualifiedFullName
{
	var packageName:String;
	var classNames:Array;
	var classNameSpaces:Array;
	
	var methodName:String;
	var methodNameSpace:String;
	var fullClassName:String;
	var getterSetter:String;
	
	public function QualifiedFullName() 
	{
		packageName = "";
		classNames = [];
		classNameSpaces = [];
		methodName = "";
		methodNameSpace = "";
		fullClassName = "";
		getterSetter = "";
	}
	
	public function toString()
	{
		var result:String = "PackageName: " + packageName;
		result += newline;
		result += "className: " + classNames[0] + newline;
		result += "classNameSpace: " + classNameSpaces[0] + newline;
		
		for(var x=1; x<classNames.length; x++)
		{
			result += "  innerClass: " + classNames[x] + newline;
			result += "  innerClassNamespace: " + classNameSpaces[x] + newline;
		}
		result += "methodName: " + methodName + newline;
		result += "methodNameSpace: " + methodNameSpace + newline;
		result += "fullClassName: " + fullClassName + newline;
		result += "getterSetter: " + getterSetter + newline;
		return result;
	}
}

function timeSec(lastTime:Date)
{
	var elapsedTime:Number = (new Date().time - lastTime)/1000;
	print(elapsedTime + " seconds");
}


function decomposeFullName(a:String, rec:QualifiedFullName, defaultNameSpace:String="public")
{	
	if (a == "")
		return;
		
	var classIndex = rec.classNames.length;
	var r:RegExp = /(?:([^:\/]+:)|([^:\/]+\/))(.+)$/;  // i.e. match (namespace):  or (class)
	var matches:Array = r.exec(a);
	
	if (matches == null)
	{
		rec.classNames[classIndex] = a;
		rec.classNameSpaces[classIndex] = defaultNameSpace;
	}
	else
	{
		if (matches[1] != undefined) // identifier terminated by ':', its a namespace
		{
			rec.classNameSpaces[classIndex] = matches[1].substring(0,matches[1].length-1);
			if (defaultNameSpace != "public")
				print("ERROR: in DecomposeName2, namespace: " + defaultNameSpace + " was passed in, but namespace: " + rec.classNameSpaces[classIndex] + " was specified");

			var ci = matches[3].indexOf("/");
			if (ci == -1)
				ci = matches[3].length;
			rec.classNames[classIndex] = matches[3].substring(0,ci);
			decomposeFullName(matches[3].substring(ci+1,matches[3].length),rec);
		}
		else if (matches[2] != undefined) // identifier terminated by a '/', its a classname
		{
			rec.classNames[classIndex] =  matches[2].substring(0,matches[2].length-1);
			rec.classNameSpaces[classIndex] = defaultNameSpace;
			decomposeFullName(matches[3],rec);
		}
		else
		{
			print("ERROR: " + matches);
		}
	}
}

function decomposeFullClassName(a:String):QualifiedFullName
{
	var r:RegExp = /(?:([^:\/\$]+\$)|([^:\/\$]+:)|([^:\/\$]+\/))(.+)$/; // i.e. match (packageName$) or (packageName:) or (className/)
	var matches:Array = r.exec(a);
	var result:QualifiedFullName = new QualifiedFullName();
	
	if (matches == null)
	{
		result.classNames[0] = a;
		result.classNameSpaces[0] = "public";
		result.fullClassName = a;
		return result;
	}
	else
	{
		var restStr = matches[4];
		if (matches[1] != undefined) // found a $ first, its a package, non-public namespace.  ':' must follow '$'.
		{
			result.packageName = matches[1].substring(0,matches[1].length-1);
			var ci = restStr.indexOf(":");
			if (ci == -1)
				print("ERROR: compiler emitted invalid fullname: " + a);
			var nextNameSpace = restStr.substring(0,ci).replace(/\d+\$/, "");
			decomposeFullName(restStr.substring(ci+1,restStr.length),result,nextNameSpace);
		}
		else if (matches[2] != undefined) // found a : first, its a package, public namespace
		{
			result.packageName = matches[2].substring(0,matches[2].length-1);
			decomposeFullName(restStr,result,"public");
		}
		else if (matches[3] != undefined) // found a / first, no package, public class
		{
			result.classNames[0] = matches[3].substring(0,matches[3].length-1);
			result.classNameSpaces[0]="public";
			decomposeFullName(restStr,result);
		}
		else
		{
			print("Internal Error in decomposeFullName: " + matches);
		}
	}
	
	result.fullClassName = a;
	//print("@@ Decomp fullname: " + result.fullClassName);

	return result;
}

function decomposeFullMethodOrFieldName(a:String):QualifiedFullName
{
	var result:QualifiedFullName = decomposeFullClassName(a); // just like a className, except for the end.
	
	// last "class" is actually the function or variable name
	result.methodName = result.classNames.pop();
	result.methodNameSpace = result.classNameSpaces.pop();

	// unless it was a getter or setter.
	if (result.methodName == "get" && result.classNames.length > 1)
	{
		result.getterSetter = "Get";
		result.methodName = result.classNames.pop();
		result.methodNameSpace = result.classNameSpaces.pop();
	}
	else if (result.methodName == "set" && result.classNames.length > 1)
	{
		result.getterSetter = "Set";
		result.methodName = result.classNames.pop();
		result.methodNameSpace = result.classNameSpaces.pop();
	}
	
	if (result.methodNameSpace == result.packageName)
		result.methodNameSpace = "public";


	// special case for a method or var which is toplevel within a package:
 	if (result.classNames[0] == undefined && result.packageName != "") {
		result.classNames[0] = "$$" + result.packageName + "$$";
		if (verbose)	
			print("##inventing fake className: " + result.classNames[0]);
	}

	// now rebuild fullclassname from components 	
	result.fullClassName = "";
	if (result.packageName != "")
	{
		result.fullClassName = result.packageName.substring(0, result.packageName.length);
		if (result.classNameSpaces[0] != "" && result.classNameSpaces[0] != "public" && result.classNameSpaces[0] != undefined)
			result.fullClassName += "$" + result.classNameSpaces[0] + ":";
		else
			result.fullClassName += ":";
	}
	if (result.classNames[0] != "" && result.classNames[0] != undefined)
		result.fullClassName += result.classNames[0];
	var numClasses = result.classNames.length;
	if (numClasses>1)
		result.fullClassName += "/";
	for(var x =1; x < numClasses; x++)
	{
		if (result.classNameSpaces[x] != "public" && result.classNameSpaces[x] != undefined)
			result.fullClassName += result.classNameSpaces[x] + ":";
		if (result.classNames[x] != "") {
			result.fullClassName +=  result.classNames[x] + (x == numClasses-1 ? "" : "/");
		}
	}
	
 	if (result.fullClassName == "")
 	{
 		result.fullClassName = globalPackage; // use fake "global" class to hold global methods/props
 	}
 	else 
	if (result.classNames[0] == "$$" + result.packageName + "$$")
 	{
 		// if this is the first time we've had to create this fake name, create fake class record for it
 		var fakeClass  = classTable[result.fullClassName];
 		if (fakeClass == undefined) {
			if (verbose)
				print("## created fake class record for: " + result.fullClassName);
 			var newClass = new AClass();
 			newClass.name = result.classNames[0];
			newClass.fullname = result.fullClassName;
			newClass.decompName = result;
			newClass.base_name = ""; // don't use Object, else it shows up in Object's decendants
			newClass.isInterface = false;
 			classTable[result.fullClassName] = newClass;
 			newClass.node = <asClass></asClass>;
 			
 			var packageContents = packageContentsTable[result.packageName];
			if (verbose)
				print("  adding class " + result.classNames[0] + " to package: " + result.packageName);
			if (packageContents == undefined || packageContents == null) {
				packageContents = new Object();
				packageContentsTable[result.packageName] = packageContents;
			}
			packageContents[result.classNames[0]] = newClass;	
 		}
	}
	// print("$$$ Decomp fullname: " + result.fullClassName + " method: " + result.methodName);

	return result;
}

//function getFirstClassRecFromNonFullName(className:String, packageName:String):AClass
//{
//	if (className == undefined || className == null)
//		return null;
	
//	var result = simpleNameClassTable[className];
//	if (result == undefined || typeof result != "object") // looking up "toString" will return the function value of toString
//		result = null;
	
//	return result;
	
	// todo, find class which matches packageName, else ???
	/*
	if (packageName == "")
	{
		result = simpleClassTable[className];
	}
	for( var x in classTable)
	{
		var r:AClass = classTable[x]		
 		if (r.name == className) 
 			return r;
 	}
 	return null;
 	*/
 //}
 
 

// create shortDescription string from long version by looking for the first
//   period followed by whitespace.  That first sentance is the shortDesc
function descToShortDesc(fullDesc:String)
{
  	var nextLoc = 0;
	var periodLoc = fullDesc.indexOf(".",nextLoc);
 	var len = fullDesc.length;
 	var shortDesc:String = fullDesc.substring(0);

 	while(periodLoc != -1 && nextLoc < len)
 	{
 		nextLoc = periodLoc+1;
 		var nextChar = fullDesc.charAt(nextLoc);
 		if (nextChar == " " || nextChar == "\r" || nextChar == "\n" || nextChar == "\t")
 		{
 			shortDesc = fullDesc.substring(0,nextLoc);
 			break;
 		}	
 		// else it was a non sentence ending '.', keep searching
 		periodLoc = fullDesc.indexOf(".",nextLoc);
 	}

	return new XML("<shortDescription><![CDATA[" + shortDesc + "]]></shortDescription>");
}

function createRelativePath(fromPackage:String, toPackage:String):String
{
	if (verbose)
		print("createRelativePath("+fromPackage + ","+toPackage+")");
	var fromParts:Array = fromPackage.split('.');
	var toParts:Array = toPackage.split('.');
	var result:String = "";

	for (var i:Number=0; i < fromParts.length; i++)
	{
		if (i > (toParts.length - 1) || fromParts[i] != toParts[i])
		{
			for (var j:int=i; j < fromParts.length; j++)
				result += "../";
				
			for (var k:int=i; k < toParts.length; k++)
			{
				if (toParts[k] != "")
					result += (toParts[k] + "/");
			}
			
			break;
		}
		
		if (i == fromParts.length - 1)
		{
			for (var j:int=i+1; j < toParts.length; j++)
			{
				if (toParts[j] != "")
					result += (toParts[j] + "/");
			}
		}
	}
	
	if (verbose)
		print("createRelativePath returning " + result);
	
	return result;
}

function createClassRef(classNm:QualifiedFullName, fromClassNm:QualifiedFullName)
{
	var result = <classRef name={classNm.classNames[classNm.classNames.length-1]} fullName={classNm.fullClassName} packageName={classNm.packageName}></classRef>;
	var href = simpleRefNameToRelativeLink(classNm.fullClassName, fromClassNm.packageName);
//	var href = simpleRefNameToRelativeLink(classNm.classNames[0], fromClassNm.packageName);
	if (href == null)
	{
		// simpleRefNameToRelativeLink already print error for the link string, now print where problem occurs
		if (classNm.methodName != "")
			print("in createClassRef() for : " + classNm.fullClassName + " in " + classNm.methodName);
		else
			print("in createClassRef() for : " + classNm.fullClassName + " from " + fromClassNm.toString());
	}
	else
	{
		href = href.replace(/[:]/, "/");
	}
	result.@relativePath = href;
	
	return result;
}

function createCanThrow(fullThrows:XML, fromQualifiedClassName:QualifiedFullName):XML
{
	var throwCommentStr = "";
	var fullThrowStr = String(fullThrows);
	var nextSpaceIndex = fullThrowStr.indexOf(" ");
	var errorClassStr = null;
	if (nextSpaceIndex == -1)
	{
		errorClassStr = "Error";
		throwCommentStr = fullThrowStr;
	}
	else
	{
		errorClassStr = fullThrowStr.substring(0,nextSpaceIndex);
		throwCommentStr = fullThrowStr.substring(nextSpaceIndex+1,fullThrowStr.length);
	}
	//throwCommentStr = translateHtmlTags(throwCommentStr);
	
	var canThrow:XML = <canThrow></canThrow>;
	canThrow.description = new XML("<description><![CDATA[" + throwCommentStr + "]]></description>");
//	canThrow.description = <description>{"<![CDATA[" + throwCommentStr + "]]>"}</description>;

	var errorClass = classTable[errorClassStr];
	if (errorClass != null) {
 		canThrow.classRef = createClassRef(errorClass.decompName, fromQualifiedClassName);
	}
	else {
		if (verbose)
			print("   Can not resolve error class name: " + errorClassStr + " looking in flash.errors");
		errorClass = classTable["flash.errors:" + errorClassStr];
		if (errorClass != null) {
			canThrow.classRef = createClassRef(errorClass.decompName, fromQualifiedClassName);
		} 
		else {
			if (errorClassStr.indexOf(".") != -1 && errorClassStr.indexOf(":") == -1) {
				var parts:Array = errorClassStr.split(".");
				errorClassStr = "";
				for (var i:int=0; i < parts.length; i++) {
					if (i == parts.length - 1)
						errorClassStr += ":";
					else if (i != 0)
						errorClassStr += ".";
						
					errorClassStr += parts[i];
				}
				
				errorClass = classTable[errorClassStr];
				if (errorClass != null) {
					canThrow.classRef = createClassRef(errorClass.decompName, fromQualifiedClassName);
					return canThrow;
				}
			}
            if (refErrors) {
			print("   Can not resolve error class name: " + errorClassStr + " Using Error instead");
			print("     in createCanThrow() for " + fromQualifiedClassName.fullClassName);
            }
			errorClass = classTable["Error"];
			if (errorClass != null) // should never happen, but just in case.
				canThrow.classRef = createClassRef(errorClass.decompName, fromQualifiedClassName);	
		}
	}
	
	return canThrow;
 }


// build xml subtrees for all classes, but don't create inner classes just yet.  XML property set is by value, so we
//  need to put all the methods/fields into the inner class's xml node before we add that class to the classes xmlList 
//  of its containing class.
function assembleClassXML()
{
	if (verbose) print("assembleClassXML");

	for each (var r:Object in classTable)
	{
		if (verbose) print("assembling" + r.fullname);
		
		if (r.node == null)
			continue;
			
		if (r.constructorCount > 0) {
 			r.node.constructors = r.constructors;
 		}
		
 		if (r.methodCount > 0) {
 			r.node.methods = r.methods;
 		} 
	 		
 		if (r.fieldCount > 0) {
 			// special post-process necessary to denote read-only or write-only properties
 			//  This has to happen after all methods have been processed.  Only then can you be
 			//  sure that you found a getter but not a setter or visa versa
 			for each(var f:Object in r.fields.field)
 			{
 				var getSetCode = r.fieldGetSet[String(f.@name)];
 				if (getSetCode == undefined) {
 					// probably a normal field. 
 					continue;
 				}
				if (getSetCode == 1) {
 					f.@only = "read";

 				} else if (getSetCode == 2) {
					f.@only = "write";
 				} else if (getSetCode == 3) {
					f.@only = "read-write";
 				}
 			}

 			r.node.fields = r.fields;			
 		}
  		if (r.eventCount > 0) {
 			r.node.classEvents = r.classEvents;
 		}
 	}
	if (verbose) print("done with assembleClassXML");
 }

// put inner classes within their containing class
//  create package xml nodes
//  put classes, methods, fields into their containing package
function assembleClassPackageHierarchy()
{
	// first put inner classes inside their containing classes
	for each (var r:AClass in classTable)
	{
		if (r.innerClasses != null)
		{
			r.node.classes = <classes></classes>;
			for each (var ir:AClass in r.innerClasses)
			{
				ir.node.@name = r.name + "." + ir.name;
				r.node.classes.asClass += ir.node;
			}
		}
	}

	// now build packages
	var globalClassList = packageContentsTable[globalPackage];
	var packages = <packages></packages>;
	var count = 0;
	
	for( var y in packageContentsTable)
	{
		var p = packageTable[y]; // use asDoc comment for package, if available.
		if (p == undefined)
			p = <asPackage name={y}></asPackage>;
			
		if (p.private != undefined || hidePackage(y)) // skip private or hidden packages.
			continue; 
			
		var packageContents = packageContentsTable[y];
		if (y != "" /*&& y != globalPackage*/)
		{
			p.classes = <classes></classes>;
			for each (var r:AClass in packageContents)
			{
				if (verbose)
					print("post-processing class " + r.name + " in package " + y);	
	 			
 				// if its the fake class created to hold top level methods and fields of a package
				if (r.name.charAt(0) == "$" && r.name.charAt(1) == "$") 
				{   // add the fake class's list of methods/fields to the package
					p.methods += r.node.methods;
					p.fields += r.node.fields;
				}
				else if (r.isInnerClass == false) // add to class to the packages list of classes
				{
					if (r.node.@accessLevel != "private" /*r.decompName.classNameSpaces[0] != "private"*/ || configRec.includePrivates == true)	
						p.classes.asClass += r.node;
				}
			}
			
			/* check for top level methods and variables of the package */
			// TODO:  IS THIS STILL NECESSARY?
/*			var packageTopClassRec:AClass = null;
			if (y == globalPackage)
				packageTopClassRec = classTable[y];
			else
				classTable["$$" + y + "$$"];
			if (packageTopClassRec != null)
			{
				print("found top level methods or functions for package: " + y);
				if (packageTopClassRec.fields != null) {
					print("Adding fields");
					p.fields = packageTopClassRec.fields;
				}
				if (packageTopClassRec.methods != null) {
					print("Adding methods");
					p.methods = packageTopClassRec.methods;
				}
			}
			else print("Did not find top level package stuff for " + y);
*/
			if (p.classes.hasOwnProperty("asClass") || p.hasOwnProperty("fields") || p.hasOwnProperty("methods"))
				packages.asPackage += p;
		}
	}

	doc.appendChild(packages);
}




function preprocessClass(classRec:XML, isInterface:Boolean)
{
	var fullname:String =	String(classRec.@fullname);
	if (verbose) print("preprocessing " + fullname);
	var thisClass:AClass =  new AClass();	
	thisClass.decompName = decomposeFullClassName(fullname);
	if (hidePackage(thisClass.decompName.packageName))
		return;
	
	var name:String = String(classRec.@name);	
	thisClass.name = name;
	thisClass.fullname = fullname;
	thisClass.base_name = String(classRec.@baseclass);
	
	thisClass.isInterface = isInterface;
	
	classTable[fullname] = thisClass; 
//	var simpleName = name; // for lookup of simple class name
	
	// adjust if this is an inner class
//	if (thisClass.decompName.classNames.length > 1)
//	{
//		simpleName = thisClass.decompName.classNames[0];
//		for(var xx=1; xx < thisClass.decompName.classNames.length; xx++)
//		{
//			simpleName += ("." + thisClass.decompName.classNames[xx]);
//		}
//	}
//	if (simpleNameClassTable[simpleName] != undefined)
//	{
//		print("Warning: data contains more than one class with simple class name: " + simpleName);
//		print("   " + fullname + " vs " + simpleNameClassTable[simpleName].fullname);
//	}
//	simpleNameClassTable[simpleName] = thisClass; // sometimes a comment only refers to a class by simple name
//	simpleNameClassTable[name] = thisClass; // also add just the class name itself in case of local class lookup

	var packageName = thisClass.decompName.packageName;
	if (packageName == "" || packageName == null)
		packageName = globalPackage;
		
	var packageContents = packageContentsTable[packageName];
	if (verbose)
		print("  adding class " + name + " to package: " + packageName);
	if (packageContents == undefined || packageContents == null) {
		packageContents = new Object();
		packageContentsTable[packageName] = packageContents;
	}
	packageContents[name] = thisClass;	
	count++;
	
	// now create xml node for this class.  Don't process custom tags like <see> yet because we can't handle
	//  cross class references until all classes have been pre-processed.   Fields and methods will be added
	//  to this xml node individually when we process those elements.  Inner classes will have their node's added
	//  to this node during processClass() as well.
	
	var packageTag =		thisClass.decompName.packageName;
//	var accessLevel = thisClass.decompName.classNameSpaces[0];
	var accessLevel:String = String(classRec.@access);
	var baseclass:String =	String(classRec.@baseclass); // "Object";
	var inheritanceTree:String = "";
	
	var first_time = true;
		
	// synthesize these?
	var path = "";
	if (classRec.hasOwnProperty("path"))
		path = String(classRec.@path);

	var relativePath = "../";
	if (classRec.hasOwnProperty("relativePath"))
		relativePath = String(classRec.@relativePath);
			
	var href = "";
	if (classRec.hasOwnProperty("href"))
		href = String(classRec.href);
	thisClass.href = href;
		
	var author = "";
	if (classRec.hasOwnProperty("author"))
		author = String(classRec.author);
			
	var taghref = "";
	if (classRec.hasOwnProperty("taghref"))
		taghref = String(classRec.taghref);
			
	var isFinal = String(classRec.@isFinal);
	if (isFinal == "")
		isFinal = "false";
		
	var isDynamic = String(classRec.@isDynamic);
	if (isDynamic == "")
		isDynamic = "false";
			
	// <!ELEMENT object (description,(short-description)?,(subclasses)*,(prototypes)?,(customs)*,(sees)*,
	//					 (methods)*,(fields)*,(events)*,(inherited)?,(constructors)?,(example)*,(private)*,(version)*)>
	var nuClass:XML;
	
	if (isInterface) {
//BDJ TODO @interfaces will move to @baseclasses
		var interfaceStr = String(classRec.@baseClasses);
		
		if (interfaceStr != "")
			thisClass.interfaceStr = interfaceStr;
			
		nuClass = <asClass
					name={name}
					type='interface'
					fullname={fullname}
					accessLevel={accessLevel}
					isFinal={isFinal}
					isDynamic={isDynamic}
					packageName={packageTag}
					path={path} 
					relativePath={relativePath} 
					href={href}
					taghref={taghref}>
				</asClass>;
	} else {
		var interfaceStr = String(classRec.@interfaces);
		
		if (interfaceStr != "")
			thisClass.interfaceStr = interfaceStr;
			
		nuClass = <asClass
					name={name}
					type='class'
					fullname={fullname}
					accessLevel={accessLevel}
					isFinal={isFinal}
					isDynamic={isDynamic}
					packageName={packageTag}
					path={path} 
					relativePath={relativePath} 
					href={href}
					taghref={taghref}>
				</asClass>;
	}
	
 	if (classRec.hasOwnProperty("description"))
 	{
 		var fullDesc:String = String(classRec.description);
 		nuClass.description = classRec.description;
		nuClass.shortDescription = descToShortDesc(fullDesc);
	}
	
	nuClass.@inheritDoc = classRec.hasOwnProperty("inheritDoc");
	if (classRec.hasOwnProperty("copy"))
		nuClass.@copy = String(classRec.copy);
	
	processVersions(classRec, nuClass, name);
	nuClass.author = author;
	
	if (classRec.hasOwnProperty("example"))
	{
		nuClass.example = classRec.example;
	}
	thisClass.node = nuClass;
	if (verbose) print("done preprocessing " + fullname);
}

// Create AClass entry in classTable for every class in the data.  Need to
//  find all classes before we can resolve cross references in @see tags
//  Also, create basics of xml node for class.
function preprocessClasses(rawDocXML:XML)
{
	if (verbose) {
		print("preprocessing classes");
		print("------------------");
	}
	var count = 0;
	for each (var classRec:Object in rawDocXML..classRec)
	{
		if (((classRec.hasOwnProperty("private") || classRec.@access=="private") && configRec.includePrivates == false) || classRec.metadata.hasOwnProperty("Deprecated"))
			continue;	
			
		if (classRec.@access=="internal" && !includeInternal)
			continue;

		preprocessClass(classRec,false);
	}
	for each (var interfaceRec:Object in rawDocXML..interfaceRec)
	{
		if (((interfaceRec.hasOwnProperty("private") || interfaceRec.@access=="private") && configRec.includePrivates == false) || interfaceRec.metadata.hasOwnProperty("Deprecated"))
			continue;		
			
		if (interfaceRec.@access=="internal" && !includeInternal)
			continue;
			
		preprocessClass(interfaceRec,true);
	}
	
	for each( var packageRec:Object in rawDocXML..packageRec)
	{
		var name = packageRec.@fullname;
		// TODO packageTable["constructor"] is never undefined
		if (packageTable[name] == undefined)
			packageTable[name] = <asPackage name={packageRec.@name}></asPackage>;
		var aPackage = packageTable[name];
		
		aPackage.description = packageRec.description;
		if (packageRec.private != undefined)
			aPackage.private = <private>true</private>;
		processCustoms(packageRec, aPackage, false, "", "", "");		
	}

}

function processVersions(from:XML, to:XML, id:String="")
{	
	if (verbose) print("Processing versions");
	
	var showWarnings:Boolean = false;
	var langversion:String = ""
	if (id == "")
		id = String(from.@name);
		
	if (to.versions == undefined)
		to.versions = <versions/>;
	if (from.hasOwnProperty("langversion"))
	{
		langversion = String(from.langversion[0]).replace("\n","").replace("\r","");
				
		if (showWarnings && langversion.length == 0)
			print("Warning: Empty langversion tag for " + id);
			
		if (showWarnings && from.langversion.length > 1)
			print("Warning: Found " + from.langversion.length + " langversion tags for " + id);
	}
	else if (showWarnings)
	{
		print("Warning: Missing langversion tag for " + id);
	}
	
	var playerversion:Array = [];
	if (from.hasOwnProperty("playerversion"))
	{
		for(var i:uint = 0; i < from.playerversion.length(); i++)
			playerversion.push(String(from.playerversion[i]).replace(/\A\s+/,"").replace(/\Z\s+/,"").replace(/\s+/," ").split(" "));
	
		if (showWarnings && from.playerversion.length() != 1)
			print("Warning: Found " + from.playerversion.length() + " playerversion tags for " + id);
	}
	else if (showWarnings)
	{
		print("Warning: Missing playerversion tag for " + id);
	}
	
	var productversion:Array = [];
	if (from.hasOwnProperty("productversion"))
	{
		for (var i:uint = 0; i < from.productversion.length(); i++)
			productversion.push(String(from.productversion[i]).replace(/\A\s+/,"").replace(/\Z\s+/,"").replace(/\s+/," ").split(" "));
			
		if (showWarnings && from.productversion.length() != 1)
			print("Warning: Found " + from.productversion.length() + " productversion tags for " + id);
	}
	else if (showWarnings)
	{
//		print("Warning: Missing productversion tag for " + id);
	}
	
	var toolversion:Array = [];
	if (from.hasOwnProperty("toolversion"))
	{
		for (var i:uint = 0; i < from.toolversion.length(); i++)
			toolversion.push(String(from.toolversion[i]).replace(/\A\s+/,"").replace(/\Z\s+/,"").replace(/\s+/," ").split(" "));
		
		if (showWarnings && from.toolversion.length() != 1)
			print("Warning: Found " + from.toolversion.length() + " toolversion tags for " + id);
	}
	else if (showWarnings)
	{
//		print("Warning: Missing toolversion tag for " + id);
	}

	if (langversion.length > 0)
		to.versions.langversion = <langversion version={langversion}/>;

	for (var i:uint = 0; i < playerversion.length; i++)
	{
		if (playerversion[i].length > 1)
			to.versions.playerversion += <playerversion name={playerversion[i][0]} version={playerversion[i][1]}/>;
//		if (showWarnings && playerversion[i].length != 2)
//		{
//			print("Warning: Invalid playerversion tag with " + playerversion[i].length + " elements for " + id);
//			for (var j:uint = 0; j < playerversion[i].length; j++)
//				print("playerversion["+i+"]["+j+"]=|"+playerversion[i][j]+"|");
//		}
	}

	for (var i:uint = 0; i < productversion.length; i++)
	{
		if (productversion[i].length > 1)
			to.versions.productversion += <productversion name={productversion[i][0]} version={productversion[i][1]}/>;
//		if (showWarnings && productversion[i].length != 2)
//			print("Warning: Invalid productversion tag for " + id);
	}

	for (var i:uint = 0; i < toolversion.length; i++)
	{
		if (toolversion[i].length > 1)
			to.versions.toolversion += <toolversion name={toolversion[i][0]} version={toolversion[i][1]}/>;
//		if (showWarnings && toolversion[i].length != 2)
//			print("Warning: Invalid toolversion tag for " + id);
	}
	
	if (verbose) print("Done processing versions");
}

// process all custom elements for the class xml node 
//  (resolve @see references now that we have AClass records for every class name
//  record inner class relationship.
function processClass(classRec:XML, isInterface:Boolean)
{
	var name:String =		String(classRec.@name);
	var fullname:String =	String(classRec.@fullname);
	if (verbose)
		print("  processing class: " + name);
	
	if (hidePackage(decomposeFullClassName(fullname).packageName))
		return;
		
	if (hideNamespace(String(classRec.@access)))
		return;
	
	var thisClass:AClass	= classTable[fullname];
	processCustoms(classRec,thisClass.node, false /*use params*/, "", "", "");
	processExcludeInherited(classRec, thisClass);

	// if this is an inner class, add it to it's parent class's node and mark as innerClass
	if (thisClass.decompName.classNames.length > 1)
	{
		thisClass.isInnerClass = true;
		var fullname = thisClass.decompName.fullClassName;
		var classScopeName = fullname.substring(0, fullname.indexOf("/"));
		var outerClass = classTable[classScopeName];

		if (outerClass != null)
		{
			if (outerClass.innerClassCount == 0)
				outerClass.innerClasses = new Array();
			outerClass.innerClasses[outerClass.innerClassCount++] = thisClass;		
		}
		else
		{
			print("Didn't find outer class for " + thisClass.decompName.fullClassName);
		}
	}
	
	if (verbose)
		print("  done processing class: " + name);
}

function processExcludeInherited(node:XML, thisClass:AClass)
{
	for each (var exclude:Object in node..excludeInherited)
	{
		var args:Array = exclude.toString().split(" ");
		
		if (verbose)
			print("excludeInherited: |" + args[0] + "|, |" + args[1].replace(/\s/,"") + "|");
			
		switch (args[1].replace(/\s/,""))
		{
			case "property":
				thisClass.excludedProperties.push(args[0]);
				break;
			case "method":
				thisClass.excludedMethods.push(args[0]);
				break;
			case "event":
				thisClass.excludedEvents.push(args[0]);
				break;
			case "style":
				thisClass.excludedStyles.push(args[0]);
				break;
			case "effect":
				thisClass.excludedEffects.push(args[0]);
				break;
		}
	}
}
	
function processClasses(rawDocXML:XML)
{
	for each (var classRec:Object in rawDocXML..classRec) 
	{
		if (((classRec.hasOwnProperty("private") || classRec.@access=="private") && configRec.includePrivates == false) || classRec.metadata.hasOwnProperty("Deprecated"))
			continue;		
		processClass(classRec, false);
	} 
	for each (var interfaceRec:Object in rawDocXML..interfaceRec)
	{
		if (((interfaceRec.hasOwnProperty("private") || interfaceRec.@access=="private") && configRec.includePrivates == false) || interfaceRec.metadata.hasOwnProperty("Deprecated"))
			continue;	
		processClass(interfaceRec, true);	
	}
}

function deriveAllDecendantsFor(thisClass:AClass):Array
{	
	// if we've already processed this class, return our cached result
	if (thisClass.allDecendants != null) 
		return thisClass.allDecendants;
	
	if (verbose)
		print("deriving decendants for: " + thisClass.name);

	// else if this has no decendants, return the empty array
	//print(" num chillun: " + thisClass.directDecendants + ":" + thisClass.directDecendants.length + " = " + thisClass.directDecendants[0]);
	if (thisClass.directDecendants.length == 0)
	{	
		return [];
	}
	
		
	// else walk each direct decendant, adding it and all its children's decendants
	//   (i.e. recursively walk the direct decendants)
	var myDecendants = [];
	for each (var d:Object in thisClass.directDecendants)
	{
		myDecendants = myDecendants.concat(deriveAllDecendantsFor(d));
		myDecendants.push(d);
	}	
	
	thisClass.allDecendants = myDecendants;
	
	return myDecendants;
}

function processClassInheritance()
{
	if (verbose) {
		print("processing class inheritance");
	}
	
	// first generate the list of direct decendants for each class (and set inner class relationship)
	for each (var thisClass:Object in classTable)
	{
		if (thisClass.node == null)
			continue;
			
		//if (verbose)
		//	print("processing class: " + thisClass.decompName.classNames[thisClass.decompName.classNames.length-1]);
		for each (var searchClass:AClass in classTable)
		{
			//print("      checking against class: " + searchClass.name);
			if (searchClass.fullname != thisClass.fullname)
			{
				if (!searchClass.isInterface)
				{
					if (searchClass.base_name == thisClass.fullname)
//					if (searchClass.base_name == thisClass.fullname && thisClass.name != 'Object')
					{
						if (searchClass.decompName.classNameSpaces[0] != "private" || configRec.includePrivates == true)
							thisClass.directDecendants.push(searchClass);
						//break;
					}
					if (thisClass.isInterface)
					{
						if (searchClass.interfaceStr != null && searchClass.interfaceStr.indexOf(thisClass.fullname) != -1)
							if (searchClass.decompName.classNameSpaces[0] != "private" || configRec.includePrivates == true)
							{
								if (thisClass.node.foo == null)
								{
//									var implementers:XML = <implementers></implementers>;
									thisClass.node.implementers = new XML("<implementers></implementers>");
								}
									
								thisClass.node.implementers.classRef += createClassRef(searchClass.decompName, thisClass.decompName);
							}
					}
				}
				else if (thisClass.isInterface)
				{
					if (searchClass.interfaceStr.indexOf(thisClass.fullname) != -1)
					{
						if (searchClass.decompName.classNameSpaces[0] != "private" || configRec.includePrivates == true)
							thisClass.directDecendants.push(searchClass);
						//break;
					}
				}
			}
		}

		// now recursively walk each classes directDecendants to generate its allDecendants	
		if (thisClass.name == globalPackage)
			continue;
		
		var allChillun:Array = thisClass.directDecendants; // deriveAllDecendantsFor(thisClass);	
		if (allChillun.length > 0)
		{
			thisClass.node.asDecendants  = <asDecendants></asDecendants>;
			for each(var chillun:Object in allChillun)
			{
				thisClass.node.asDecendants.classRef += createClassRef(chillun.decompName, thisClass.decompName);	
			}
		}
		
  		if (thisClass.name == 'Object' || (thisClass.isInterface && thisClass.interfaceStr == null))
  			continue;

		var inheritsFrom:XML = <asAncestors></asAncestors>;

		if (thisClass.isInterface)
		{
			inheritsFrom = processInterfaceInheritance(thisClass, inheritsFrom, thisClass);
		} 
		else 
		{
			var base = classTable[thisClass.base_name];
			var inheritedMethNames = " ";
			for (var methName:Object in thisClass.methodOverrideTable)
				inheritedMethNames += methName + " ";
			var inheritedFieldNames = " ";
			if (thisClass.fields != null)
			{
				for each (var field:Object in thisClass.fields.field)
					inheritedFieldNames += field.@name + " ";
			}
			var inheritedEventNames = " ";
			for each (var event:Object in thisClass.node.eventsGenerated.event)
				inheritedEventNames += event.@name + " ";
			var inheritedStyleNames = " ";
			for each (var style:Object in thisClass.node.styles.style)
				inheritedStyleNames += style.@name + " ";


            var lastName:String = thisClass.base_name;
			while(base != null) // thisClass.interfaceStr
			{
				mergeExcludes(base, thisClass);
				inheritsFrom.asAncestor += createAncestor(base,thisClass, inheritedFieldNames, inheritedMethNames, inheritedEventNames, inheritedStyleNames);
				
				for (var methName:Object in base.methodOverrideTable)
					inheritedMethNames += methName + " ";
			
				if (base.fields != null)
				{
					for each (var field:Object in base.fields.field)
						inheritedFieldNames += field.@name + " ";
				}
				if (base.node.hasOwnProperty("styles"))
				{
					for each(var style:Object in base.node.styles.style)
						inheritedStyleNames += style.@name + " ";
				}

				if (base.name == "Object")
					break;

                lastName = base.base_name;
				base = classTable[base.base_name];	
			}

			if ((base == null || base.name != "Object") && lastName != null && lastName != "Object")
			{
				inheritsFrom.asAncestor += createExternalAncestor(thisClass, lastName);
			}

		}
		thisClass.node.asAncestors = inheritsFrom;

		if (!thisClass.isInterface && thisClass.interfaceStr != undefined && String(thisClass.interfaceStr) != "")
		{
			var implementsList = <asImplements></asImplements>;
 			var interfaces:Array = thisClass.interfaceStr.split(";");
  			for (var i:int = 0; i < interfaces.length; i++)
  			{
  				var interfaceClass = classTable[interfaces[i]];
  				if (interfaceClass != null) {
 					implementsList.asAncestor += createAncestor(interfaceClass,thisClass, "", "", "", "");
  				}
  				else {
					implementsList.asAncestor += createExternalAncestor(thisClass, interfaces[i]);

                    if (refErrors) {    
  					print("   Can not resolve: ");
					print(interfaces[i]);
  					print("    in processAncestors() for: " + thisClass.decompName.fullClassName);
                    }
  				}
   			}
			thisClass.node.asImplements = implementsList;
		}
		
		processCopyDoc(thisClass);
	}
		
	for each (var thisClass:Object in classTable)
	{
		processInheritDoc(thisClass);
	}
	
	if (verbose) {
		print("done processing class inheritance");
	}
}

function processCopyDoc(currentClass:AClass)
{
	if (currentClass.node.customs != undefined && currentClass.node.customs.copy != undefined)
	{
		processCopyNode(currentClass.node, currentClass.node.customs[0], currentClass);
	}
	if (currentClass.constructors != undefined)
	{
		for each (var constructor:XML in currentClass.constructors.constructor)
		{
			if (constructor.customs.copy != undefined)
				processCopyNode(constructor, constructor.customs[0], currentClass);
		}
	}
	if (currentClass.methods != undefined)
	{
		for each (var method:XML in currentClass.methods.method)
		{
			if (method.customs.copy != undefined)
				processCopyNode(method, method.customs[0], currentClass);
		}
	}
	if (currentClass.fields != undefined)
	{
		for each (var field:XML in currentClass.fields.field)
		{
			if (field.customs.copy != undefined)
				processCopyNode(field, field.customs[0], currentClass);
		}
	}
	if (currentClass.node.eventsGenerated != undefined)
	{
		for each (var event:XML in currentClass.node.eventsGenerated.event)
		{	
			if (event.customs.copy != undefined)
				processCopyNode(event, event.customs[0], currentClass);
		}
	}
	if (currentClass.node.styles != undefined)
	{
		for each (var style:XML in currentClass.node.styles.style)
		{
			if (style.copy != undefined)
				processCopyNode(style, style, currentClass);
		}
	}
	if (currentClass.node.effects != undefined)
	{
		for each (var effect:XML in currentClass.node.effects.effect)
		{
			if (effect.copy != undefined)
				processCopyNode(effect, effect, currentClass);
		}
	}
}
		
function processCopyNode(toNode:XML, fromNode:XML, toClass:AClass)
{
	var fromClassName:String = normalizeString(String(fromNode.copy));
	var fromClass:AClass = getClass(fromClassName);		
	if (fromClass == null)
	{
		if (fromClassName.indexOf("#") != -1)
			fromClass = getClass(fromClassName.slice(0, fromClassName.indexOf("#")));
			
		if (fromClass == null)
		{
			fromClass = getClass(toClass.decompName.packageName + "." + fromClassName.slice(0, fromClassName.indexOf("#")));
			
			if (fromClass == null)
			{
                if (refErrors)
                {
				print("Error: processCopyNode could not find node to copy from: " + fromClassName + " for " + toClass.name);
                }
				return;
			}
		}
	}
	
	if (fromClassName.indexOf("#") == -1)	
	{
		inheritDoc(toNode, fromClass.node, toClass);
	}
	else
	{
		var anchor:String = fromClassName.slice(fromClassName.indexOf("#") + 1, 9999999);
		if (anchor.indexOf("()") != -1) //method or constructor
		{
			anchor = anchor.slice(0, anchor.indexOf("("));
			if (fromClass.methodCount > 0)
			{
				for each (var method:Object in fromClass.methods.method)
				{
					if (method.@name == anchor)
					{
						inheritDoc(toNode, method, toClass, true, true);
						break;
					}
				}
			}
			if (fromClass.constructorCount > 0)
			{
				for each (var constructor:Object in fromClass.constructors.constructor)
				{
					if (constructor.@name == anchor)
					{
						inheritDoc(toNode, constructor, toClass, true, true);
						break;
					}
				}
			}
		}
		else if (anchor.indexOf("event:") != -1) //event
		{
			anchor = anchor.slice(anchor.indexOf(":") + 1, 9999999);
			if (fromClass.node.eventsGenerated != undefined)
			{
				for each (var event in fromClass.node.eventsGenerated.event)	
				{
					if (event.@name == anchor)
					{
						inheritDoc(toNode, event, toClass);
						break;
					}
				}
			}
		}
		else if (anchor.indexOf("style:") != -1) //style
		{
			anchor = anchor.slice(anchor.indexOf(":") + 1, 9999999);
			if (fromClass.node.styles != undefined)
			{
				for each (var style in fromClass.node.styles.style)
				{
					if (style.@name == anchor)
					{
						inheritDoc(toNode, style, toClass);
						break;
					}
				}
			}
		}
		else if (anchor.indexOf("effect:") != -1) //effect
		{
			anchor = anchor.slice(anchor.indexOf(":") + 1, 9999999);
			if (fromClass.node.effects != undefined)
			{
				for each (var effect in fromClass.node.effects.effect)
				{
					if (effect.@name == anchor)
					{
						inheritDoc(toNode, effect, toClass);
						break;
					}
				}
			}
		}
		else //field
		{
			if (fromClass.fieldCount > 0)
			{
				anchor = anchor.slice(anchor.indexOf(":") + 1, 9999999);
				for each (var field in fromClass.fields.field)
				{
					if (field.@name == anchor)
					{
						inheritDoc(toNode, field, toClass);
						break;
					}
				}
			}
		}
	}
}


function getClass(classString:String) : AClass
{
	var poundLoc = classString.indexOf("#");
	if (poundLoc == 0)
		return null;
	var lastDot = classString.lastIndexOf(".");
	
	if (lastDot != -1)
		classString = (classString.slice(0, lastDot) + ":" + classString.slice(lastDot + 1, 999999));
		
	classString = classString.replace("event:", "");
	classString = classString.replace("style:", "");
	classString = classString.replace("effect:", "");
	if (poundLoc != -1)
	{
		var classNameStr:String = classString.substring(0, poundLoc);
	
		var pathSpec:Array = classNameStr.split('.');
	
		// GlowFilter or flash.core.GlowFiler  must match the debug name: flash.core:GlowFilter
		var lastDotPos = classNameStr.lastIndexOf('.');
		var fullClassNameStr:String = classNameStr;
		if (lastDotPos != -1)
		{
			fullClassNameStr = classNameStr.substring(0, lastDotPos) + ":" + classNameStr.substring(lastDotPos+1, classNameStr.length);
		}
		if (classTable.hasOwnProperty(classNameStr) == false)
		{		
			if (classTable.hasOwnProperty(fullClassNameStr) == false)
			{
				return null;
			}
			else
			{
				return classTable[fullClassNameStr];
			}
		}
		else
		{
			return classTable[classNameStr];
		}
	}
	else
	{
		return classTable[classString];
	}
}

function findInterfaceField(currentClass:AClass, fieldName:String):XML
{
	var implemented:AClass = null;
	for each (var asAncestor:XML in currentClass.node.asImplements.asAncestor)
	{
		implemented = classTable[asAncestor.classRef.@fullName];
		if (implemented != null)
		{
			if (implemented.fieldCount > 0)
			{
	//print("implemented " + implemented.name);
				for each (var implementedField in implemented.fields.field)
				{
	//print("comparing " + implementedMethod.@name + " and " + method.@name);
					if (implementedField.@name == fieldName)
						return implementedField;
				}
			}
			
			for each (var asInterface:XML in implemented.node.asAncestors.asAncestor)
			{
				implemented = classTable[asInterface.classRef.@fullName];
				if (implemented != null && implemented.fieldCount > 0)
				{
					for each (var impField in implemented.fields.field)
					{
						if (impField.@name == fieldName)
							return impField;
					}
				}
			}
		}
	}
	return null;
}

function findInterfaceMethod(currentClass:AClass, methodName:String):XML
{
	var implemented:AClass = null;
	for each (var asAncestor:XML in currentClass.node.asImplements.asAncestor)
	{
		implemented = classTable[asAncestor.classRef.@fullName];
		if (implemented != null)
		{
			if (implemented.methodCount > 0)
			{
	//print("implemented " + implemented.name);
				for each (var implementedMethod in implemented.methods.method)
				{
	//print("comparing " + implementedMethod.@name + " and " + method.@name);
					if (implementedMethod.@name == methodName)
						return implementedMethod;
				}
			}
			
			for each (var asInterface:XML in implemented.node.asAncestors.asAncestor)
			{
				implemented = classTable[asInterface.classRef.@fullName];
				if (implemented != null && implemented.methodCount > 0)
				{
					for each (var impMethod in implemented.methods.method)
					{
						if (impMethod.@name == methodName)
							return impMethod;
					}
				}
			}
		}
	}
	return null;
}

function processInheritDoc(currentClass:AClass)
{
//print("inheritDoc checking class " + currentClass.name + "=" + currentClass.node.@inheritDoc);
	if (currentClass.node.@inheritDoc == true)
	{
		var found:Boolean = false;
		var base:AClass = classTable[currentClass.base_name];
		while (base != null && !found)
		{
			found = inheritDoc(currentClass.node, base.node, currentClass);
			
			if (base.name == "Object")
				break;
			else
				base = classTable[base.base_name];
		}
	}
	
	if (currentClass.methodCount > 0)
	{
//print("**"+currentClass.name);
		for each (var method in currentClass.methods.method)
		{
			if (method.@inheritDoc == true)
			{
				var found:Boolean = false;
				var implementedMethod:XML = findInterfaceMethod(currentClass, method.@name);
				
				if (implementedMethod != null)
					found = inheritDoc(method, implementedMethod, currentClass);
				
				if (!found)
				{						
					var base:AClass = classTable[currentClass.base_name];
					while (base != null && !found)
					{
						if (base.methodCount > 0)
						{
							for each (var baseMethod in base.methods.method)
							{	
								if (baseMethod.@name == method.@name)
								{
									found = inheritDoc(method, baseMethod, currentClass);
									break;
								}
							}
						}
						
						if (!found)
						{
							implementedMethod = findInterfaceMethod(base, method.@name);
							if (implementedMethod != null)
								found = inheritDoc(method, implementedMethod, currentClass);
						}
						
						if (base.name == "Object")
							break;
						else
							base = classTable[base.base_name];
					}
				}
			}
		}
	}
	
	if (currentClass.fieldCount > 0)
	{
		for each (var field in currentClass.fields.field)
		{
			if (field.@inheritDoc == true)
			{
				var found:Boolean = false;
				var implementedField:XML = findInterfaceField(currentClass, field.@name);
				
				if (implementedField != null)
					found = inheritDoc(field, implementedField, currentClass);
					
				if (!found)
				{
					var base:AClass = classTable[currentClass.base_name];
					while (base != null && !found)
					{
						if (base.fieldCount > 0)
						{
							for each (var baseField in base.fields.field)
							{	
								if (baseField.@name == field.@name)
								{
									found = inheritDoc(field, baseField, currentClass);
									break;
								}
							}
						}
						
						if (!found)
						{
							implementedField = findInterfaceField(base, field.@name);
							if (implementedField != null)
								found = inheritDoc(field, implementedField, currentClass);
						}
						
						if (base.name == "Object")
							break;
						else
							base = classTable[base.base_name];
					}
				}
			}
		}
	}
	
	if (currentClass.node.styles != undefined)
	{
		for each (var style in currentClass.node.styles.style)
		{
			if (style.hasOwnProperty("inheritDoc"))
			{
				var found:Boolean = false;
				var base:AClass = classTable[currentClass.base_name];
				while (base != null && !found)
				{
					if (base.node.styles != null)
					{
						for each (var baseStyle in base.node.styles.style)
						{
							if (baseStyle.@name == style.@name)
							{
								found = inheritDoc(style, baseStyle, currentClass);
								break;
							}
						}
					}
					
					if (base.name == "Object")
						break;
					else
						base = classTable[base.base_name];
				}
			}
		}
	}
}

function inheritDoc(to:XML, from:XML, toClass:AClass, copyParams:Boolean=true, copyResult:Boolean=true, includeAncestors:Boolean=true) : Boolean
{
	if (String(from.description.normalize()).length != 0)
	{
		if (String(to.description.normalize()).length == 0)
		{
			to.description = new XML("<description><![CDATA[" + from.description + "]]></description>");
			to.shortDescription = new XML("<shortDescription><![CDATA[" + from .shortDescription + "]]></shortDescription>");
		
			if (includeAncestors)
			{
				for (var className in classTable)
				{
					for each (var asAncestor:XML in classTable[className].node.asAncestors.asAncestor.(classRef.@fullName == toClass.fullname))
					{
						switch (from.localName())
						{
							case "field":
								for each (var myField in asAncestor.fields.field)
								{
									if (myField.@name == from.@name)
									{
										if (myField.shortDescription.normalize().toString().length == 0)
											myField.shortDescription = to.shortDescription;
										else
											print("Warning: " + className + "." + from.@name + " already has a shortDescription");
									}
								}
								break;
								
							case "method":
								for each (var myMethod in asAncestor.methods.method)
								{
									if (myMethod.@name == from.@name)
									{
										if (myMethod.shortDescription.normalize().toString().length == 0)
											myMethod.shortDescription = to.shortDescription;
										else
											print("Warning: " + className + "." + from.@name + " already has a shortDescription"); // + myMethod.shortDescription.normalize().toString() + ") with length " + myMethod.shortDescription.normalize().toXMLString().length);
									}
								}
								break;
								
							case "event":
								for each (var myEvent in asAncestor.eventsGenerated.event)
								{
									if (myEvent.@name == from.@name)
									{
										if (myEvent.shortDescription.normalize().toString().length == 0)
											myEvent.shortDescription = to.shortDescription;
										else
											print("Warning: " + className + "." + from.@name + " already has a shortDescription");
									}
								}
								break;
								
							case "style":
								for each (var myStyle in asAncestor.styles.style)
								{
									if (myStyle.@name == from.@name)
									{
										if (myStyle.shortDescription.normalize().toString().length == 0)
											myStyle.shortDescription = to.shortDescription;
										else
											print("Warning: " + className + "." + from.@name + " already has a shortDescription");
									}
								}
								break;
								
							case "effect":
								for each (var myEffect in asAncestor.effects.effect)
								{
									if (myEffect.@name == from.@name)
									{
										if (myEffect.shortDescription.normalize().toString().length == 0)
											myEffect.shortDescription = to.shortDescription;
										else
											print("Warning: " + className + "." + from.@name + " already has a shortDescription");
									}
								}
								break;
							
							default:
								print("Error in inheritDoc, found unknown node " + from.localName());
						}
					}
				}
			}
		}
		else
		{
			to.description = new XML("<description><![CDATA[" + to.description + "<p>" + from.description + "</p>]]></description>");
		}
/*	
		if (from.sees != undefined)
		{
			if (to.sees == undefined)
				to.sees = <sees></sees>;
				
			for each (var seeTag in from.sees.see)
				to.sees.see += seeTag;
		}
*/		
		if (copyParams && from.params != undefined)
		{
			to.params = from.params.copy();
			for each (param in to.params.param)
			{
				var paramClass:AClass = classTable[param.@type];
				if (paramClass != null)
					param.classRef = createClassRef2(toClass, paramClass);
				else
					param.classRef = undefined;
			}
		}
		
		if (copyResult && from.result != undefined)
		{
			to.result = from.result.copy();
			var resultClass:AClass = classTable[from.result.@type];
			if (resultClass != null)
				to.result.classRef = createClassRef2(toClass, resultClass);
		}
			
		return true;
	}
	
	return false;
}

function mergeExcludes(superclass:AClass, subclass:AClass)
{
	if (!includeInheritedExcludes)
		return;
		
	for (var i:uint = 0; i < superclass.excludedProperties.length; i++)
		subclass.excludedProperties.push(superclass.excludedProperties[i]);
		
	for (var i:uint = 0; i < superclass.excludedMethods.length; i++)
		subclass.excludedMethods.push(superclass.excludedMethods[i]);
		
	for (var i:uint = 0; i < superclass.excludedEvents.length; i++)
		subclass.excludedEvents.push(superclass.excludedEvents[i]);
		
	for (var i:uint = 0; i < superclass.excludedStyles.length; i++)
		subclass.excludedStyles.push(superclass.excludedStyles[i]);
		
	for (var i:uint = 0; i < superclass.excludedEffects.length; i++)
		subclass.excludedEffects.push(superclass.excludedEffects[i]);
}

function processInterfaceInheritance(thisInterface, inheritsFrom:XML, fromInterface):XML
{
	if (thisInterface == undefined || thisInterface.interfaceStr == 'Object')
		return inheritsFrom;

	var interfaceNames:Array = thisInterface.interfaceStr.split(";");
	for (var i:uint = 0; i < interfaceNames.length; i++)
	{
		var base = classTable[interfaceNames[i]];
        var sameName:Boolean = false
		if (base != undefined && inheritsFrom.asAncestor != undefined)
		{
			for each (var ancestor in inheritsFrom..asAncestor)
			{
				if (String(ancestor.classRef.@fullName) == base.fullname)
				{
					sameName = true;
					base = undefined;
					break;
				}
			}
		}
		if (base != undefined)
		{
			inheritsFrom.asAncestor += createAncestor(base, fromInterface, "", "", "", "");
			inheritsFrom = processInterfaceInheritance(base, inheritsFrom, fromInterface);
		}
        else if (! sameName)
        {
            inheritsFrom.asAncestor += createExternalAncestor(base, interfaceNames[i]);
        }
	}
	
	return inheritsFrom;
}


function createExternalAncestor(base:Object, name:String):XML
{
	var an:XML = <asAncestor></asAncestor>;
    var relPath:String = "none";
    name = name.replace(":", ".");
	an.classRef = <classRef name={name} fullName={name} relativePath={relPath}></classRef>;
    return an;
}

function createAncestor(base:AClass, thisClass:AClass, inheritedFields:String, inheritedMethods:String, inheritedEvents:String, inheritedStyles:String):XML
{	
	if (verbose)
		print("createAncestor: " + base.decompName.classNames[base.decompName.classNames.length-1] + " for " + thisClass.decompName.classNames[thisClass.decompName.classNames.length-1]);
		
	var found:Boolean = false;
	var methods:XML = null;
	
	if (base.methods != null)
	{
		for each(var meth in base.methods.method)
		{
			found = false;
			for (var i:uint = 0; i < thisClass.excludedMethods.length; i++)
			{
				if (thisClass.excludedMethods[i] == meth.@name)
				{
					found = true;
					break;
				}
			}
			
			if (found)
				continue;
				
			if (inheritedMethods.indexOf(" " + String(meth.@name) + " ") == -1)
			{
				if (methods == null)
					methods = <methods></methods>;
				
				var method:XML = meth.copy();
				var params:XML = method.params[0];
				var resultType:String = String(method.result.@type);
				method.setChildren(method.child("shortDescription"));
				
				var resultClass:AClass = classTable[resultType];
				if (resultClass != null)
				{
					method.result = <result type={resultType}></result>;
					method.result.classRef = createClassRef(resultClass.decompName, thisClass.decompName);
				}
				
				if (params != null)
				{
					method.params = <params></params>;
					for each(var param in params.param)
					{
						var paramClass:AClass = classTable[param.@type];
						if (paramClass != null)
							param.setChildren(createClassRef(paramClass.decompName, thisClass.decompName));
						else
							param.setChildren(new XMLList());
										
						method.params.param += param;
					}
				}
				
				methods.method += method;
			}
		}
	}
	
	var fields:XML = null;

	if (base.fields != null)
	{
		for each(var f in base.fields.field)
		{
			found = false;
			for (var i:uint = 0; i < thisClass.excludedProperties.length; i++)
			{
				if (thisClass.excludedProperties[i] == f.@name)
				{
					found = true;
					break;
				}
			}
			
			if (found || inheritedFields.indexOf(" " + String(f.@name) + " ") != -1)
				continue;

			if (f.@isConst != "true")
			{
				// have to do it this way to deal with fieldGetSet["constructor"]
				if (int(thisClass.fieldGetSet[f.@name]) != 0 && 
					int(thisClass.fieldGetSet[f.@name]) != int(thisClass.privateGetSet[f.@name]))
				//if (thisClass.fieldGetSet[f.@name] != undefined)
				{
					// have to do it this way to deal with privateGetSet["constructor"]
					if (int(thisClass.privateGetSet[f.@name]) == 0)
//					if (thisClass.privateGetSet[f.@name] == undefined)
//					{
						if (thisClass.fieldGetSet[f.@name] == 1) // get
						{
							if (base.fieldGetSet[f.@name] > 1)
								thisClass.fieldGetSet[f.@name] += 2;
						}
						else if (thisClass.fieldGetSet[f.@name] == 2) // set
						{
							if (base.fieldGetSet[f.@name] != 2)
								thisClass.fieldGetSet[f.@name] += 1;
						}
//					}
					continue;
				}
			}
			
			if (fields == null)
				fields = <fields></fields>;
				
			var field:XML = f.copy();
			field.setChildren(field.child("shortDescription"));
			var type:AClass = classTable[field.@type];
			if (type != null)
				field.classRef = createClassRef(type.decompName, thisClass.decompName);
				
			fields.fields += field;
		}
	}
	
	for (var baseGetSet in base.privateGetSet)
	{
		found = false;
		for (var i:uint = 0; i < thisClass.excludedProperties.length; i++)
		{
			if (thisClass.excludedProperties[i] == baseGetSet)
			{
				found = true;
				break;
			}
		}
		
		if (found)
			continue;
			
		for (var privateGetSet in thisClass.privateGetSet)
		{
			if (privateGetSet == baseGetSet)
			{
				if (thisClass.privateGetSet[privateGetSet] == 3)
				{
					found = true;
					break;
				}
				else if (thisClass.privateGetSet[privateGetSet] == 2)
				{
					if (base.privateGetSet[privateGetSet] > 1)
					{
						found = true;
						break;
					}
				}
				else if (thisClass.privateGetSet[privateGetSet] == 1)
				{
					if (base.privateGetSet[privateGetSet] != 2)
					{
						found = true;
						break;
					}
				}
			}
		}
		
		if (found)
			continue;
			
		for (var getSet in thisClass.fieldGetSet)
			if (getSet == baseGetSet)
			{
				if (thisClass.fieldGetSet[getSet] == 1) //get
				{
					if (base.privateGetSet[getSet] > 1)
						thisClass.fieldGetSet[getSet] += 2;
				}
				else if (thisClass.fieldGetSet[getSet] == 2) //set
				{
					if (base.privateGetSet[getSet] != 2)
						thisClass.fieldGetSet[getSet] += 1;
				}
				break;
			}
	}
	
	var eventsGenerated:XML = null;
	
	if (base.node.hasOwnProperty("eventsGenerated"))
	{
		for each(var f in base.node.eventsGenerated.event)
		{
			found = false;
			for (var i:uint = 0; i < thisClass.excludedEvents.length; i++)
			{
				if (thisClass.excludedEvents[i] == f.@name)
				{
					found = true;
					break;
				}
			}
			
			if (found)
				continue;
				
			if (eventsGenerated == null)
				eventsGenerated = <eventsGenerated></eventsGenerated>;
				
			var event:XML = f.copy();
			var classRef:XML = event.classRef[0];
			event.setChildren(event.child("shortDescription"));
			if (classRef != null)
				event.classRef = classRef;
				
			eventsGenerated.event += event;
		}
	}
	
	var styles:XML = null;
	
	if (base.node.hasOwnProperty("styles"))
	{
		for each(var f in base.node.styles.style)
		{
			found = false;
			for (var i:uint = 0; i < thisClass.excludedStyles.length; i++)
			{
				if (thisClass.excludedStyles[i] == f.@name)
				{
					found = true;
					break;
				}
			}
			
			if (found || inheritedStyles.indexOf(" " + String(f.@name) + " ") != -1)
				continue;
				
			if (styles == null)
				styles = <styles></styles>;
				
			var style:XML = f.copy();			
			var styleType = String(style.@type);
			style.setChildren(style.child("shortDescription"));
			if (styleType != null && styleType != "")
			{
				var see = processSeeTag(thisClass.fullname, styleType);
				if (see != null)
					style.@typeHref = see.@href;
			}
				
			styles.style += style;
		}
	}
	
	var effects:XML = null;
	
	if (base.node.hasOwnProperty("effects"))
	{
		for each(var f in base.node.effects.effect)
		{
			found = false;
			for (var i:uint = 0; i < thisClass.excludedEffects.length; i++)
			{
				if (thisClass.excludedEffects[i] == f.@name)
				{
					found = true;
					break;
				}
			}
			
			if (found)
				continue;
				
			if (effects == null)
				effects = <effects></effects>;
				
			var effect:XML = f.copy();
			effect.setChildren(effect.child("shortDescription"));
				
			effects.effect += effect;
		}
	}

	var an = <asAncestor></asAncestor>;	
	an.classRef = createClassRef2(thisClass, base);
//	var href:String = createRelativePath(thisClass.decompName.packageName, base.decompName.packageName) + base.name + ".html";
//	an.classRef = <classRef name={base.name} fullName={base.fullname} packageName={base.decompName.packageName} relativePath={href}></classRef>;

//	an.classRef = createClassRef(base.decompName, thisClass.decompName);
	if (fields != null)
		an.fields = fields;
		
	if (methods != null)
		an.methods = methods;
		
	if (eventsGenerated != null)
		an.eventsGenerated = eventsGenerated;
		
	if (styles != null)
		an.styles = styles;
		
	if (effects != null)
		an.effects = effects;
		
	if (verbose)
		print("done createAncestor for: " + base.decompName.classNames[base.decompName.classNames.length-1]);
		
	return an;
}
	
function createClassRef2(fromClass:AClass, toClass:AClass):XML
{
	var href:String = createRelativePath(fromClass.decompName.packageName, toClass.decompName.packageName) + toClass.name + ".html";
	return <classRef name={toClass.name} fullName={toClass.fullname} packageName={toClass.decompName.packageName} relativePath={href}></classRef>;
}

function processFields(rawDocXML:XML)
{
	if (verbose) {
		print("processing fields");	
		print("-----------------");
	}
	
	for each (var field in rawDocXML..field)
	{
 		var name		= String(field.@name);
 		var fullname	= String(field.@fullname);
 		if (verbose)
 			print("   processing field: " + fullname);
	 	
	 	// skip fields tagged with @private, even if they are public
		if ((field.hasOwnProperty("private") && configRec.includePrivates == false) || field.metadata.hasOwnProperty("Deprecated"))
			continue;	

 		var qualifiedName:QualifiedFullName = decomposeFullMethodOrFieldName(fullname);
 		
 		// skip fields actually in the private namespace
 		if (hideNamespace(qualifiedName.methodNameSpace) || hidePackage(qualifiedName.packageName))
			continue;
 			 		
 		var myClass  = classTable[qualifiedName.fullClassName];
 		if (typeof myClass == undefined || myClass == null)
 		{
 			// not an error, likely a method or field for a private class
 			//  print("*** internal error: could not find class for field fullname: " + fullname + " class: " + qualifiedName.fullClassName);
 			//print("$$ failed to find: " + qualifiedName.fullClassName + " " + fullname);

			continue;
 		}
		
		if (myClass.node.excludes != undefined)
		{
			var fieldName = name;
			if (myClass.node..exclude.(@kind == "property" && @name == fieldName).length() > 0)
			{
				if (verbose)
					print("Excluding property " + name + " from " + myClass.name);
				continue;
			}
		}
	 	
 		//<field name="blurX" fullname="flash.core/BlurFilter:public/blurX:public/get" accessLevel="public">		// <field name="INSERT" type="Number" accessLevel="public" static="true">

		var type:String = String(field.@type);
//		if (type == "" || type == "null")
//			type = "Object";
			
		var isConst = String(field.@isConst);
		var isStatic = String(field.@isStatic);
		if (isConst == "")
			isConst = "false";
		if (isStatic == "")
			isStatic = "false";
			
 		var nuField:XML = <field name={name} fullname={fullname} accessLevel={qualifiedName.methodNameSpace} type={type} isConst={isConst} isStatic={isStatic}>
 						</field>;
						
		if (field.attribute("defaultValue").length() > 0)
			nuField.@defaultValue = String(field.@defaultValue);
	
		var fieldTypeClass = classTable[type];
// 		var fieldTypeClass:AClass = getFirstClassRecFromNonFullName(type, "");
	 	if (fieldTypeClass != null)
 			nuField.classRef = createClassRef(fieldTypeClass.decompName, myClass.decompName);
	 					  
	 	if (field.hasOwnProperty("example"))
	 	{
	 		nuField.example = field.example;
	 	}
	 	
	 	var fullDesc:String = null;
	 	
 		if (field.hasOwnProperty("description"))
 		{
 			fullDesc = String(field.description);
 			nuField.description = field.description;
 			nuField.shortDescription = descToShortDesc(fullDesc);
 		}
 
	 	if (field.hasOwnProperty("throws"))
	 	{
 			for each(var t in field["throws"])
 			{
 				nuField.canThrow += createCanThrow(t, myClass.decompName);
 			}
	 	}

	 	processVersions(field, nuField, myClass.name + "." + field.@name);	
		processCustoms(field,nuField, false /*use params*/, "", "", "");
		
		if (field.hasOwnProperty("eventType"))
	 	{
	 		var eventNameStr:String = String(field.eventType);
	 		eventNameStr = eventNameStr.replace("\n","");
			eventNameStr = eventNameStr.replace("\r","");
			eventNameStr = normalizeString(eventNameStr);
			var firstSpace = eventNameStr.indexOf(" ");
			if (firstSpace != -1)
				eventNameStr = eventNameStr.substring(0,firstSpace);
			
 			var nuEvent:XML = <event name={eventNameStr}></event>;
 			nuEvent.classRef = createClassRef(myClass.decompName, myClass.decompName);
 			if (fullDesc != null)
 				nuEvent.description = field.description;
				
 			processCustoms(field,nuEvent, false /*use params*/, "", "", "");
 			
 			if (myClass.node.hasOwnProperty("eventsDefined") == false)
 				myClass.node.eventsDefined = <eventsDefined></eventsDefined>;
 			myClass.node.eventsDefined.event += nuEvent;	
 				
 			var qualString = qualifiedName.packageName;
 			for (var x=0; x < qualifiedName.classNames.length; x++)
 			{
 				qualString += "." + qualifiedName.classNames[x];
 			}
 			qualString += "." + qualifiedName.methodName;
 			eventTable[qualString] = myClass;

 			myClass.eventCommentTable[eventNameStr] = nuEvent.description; 
 		}
	 	
 		if (myClass != undefined && field != null)
 		{
 			if (myClass.fieldCount == 0) {
 				myClass.fields = <fields></fields>;
 				myClass.fields.field = nuField;
 			} else
 				myClass.fields.field += nuField;
	 			
			myClass.fieldCount++;	
 		}
 		else
 		{
 			print("*** Internal error: can't find class for field: " + qualifiedName.fullClassName);
 			debug_decomposeFullName(fullname);
 		}
		
 		if (verbose)
 			print("Done processing field: " + fullname);
	}
	
	if (verbose) {
		print("Done processing fields");	
		print("-----------------");
	}
}

function processExcludes(rawDocXML:XML)
{
	if (verbose)
		print("processing excludes");	
		
	for each (var exclude in rawDocXML..Exclude)
	{
		var fullname = String(exclude.@owner);
		if (verbose)
			print("   processing exclude: " + String(exclude.@name));
			
		var myClass = classTable[fullname];
		if (myClass != undefined && myClass != null)
		{
			if (myClass.node.excludes == undefined)
				myClass.node.excludes = <excludes></excludes>;
				
			myClass.node.excludes.exclude += <exclude name={exclude.@name} kind={exclude.@kind}></exclude>;	
				
			switch (String(exclude.@kind))
			{
				case "property":
					myClass.excludedProperties.push(exclude.@name);
					break;
				case "method":
					myClass.excludedMethods.push(exclude.@name);
					break;
				case "event":
					myClass.excludedEvents.push(exclude.@name);
					break;
				case "style":
					myClass.excludedStyles.push(exclude.@name);
					break;
				case "effect":
					myClass.excludedEffects.push(exclude.@name);
					break;
			}
		}
	}
	
	// TODO replace all of the <exclude> xml with the arrays,
	// use mergeExcludes() to do the below, need depth-first routine
	
	for each (var currentClass:AClass in classTable)
	{
		var base:AClass = classTable[currentClass.base_name];
		while (base != null)
		{
			if (base.node.excludes != undefined)
			{
				if (currentClass.node.excludes == undefined)
					currentClass.node.excludes = <excludes></excludes>;
					
				currentClass.node.excludes.exclude += base.node.excludes.exclude;
				
				currentClass.excludedProperties = currentClass.excludedProperties.concat(base.excludedProperties);
				currentClass.excludedMethods = currentClass.excludedMethods.concat(base.excludedMethods);
				currentClass.excludedEvents = currentClass.excludedEvents.concat(base.excludedEvents);
				currentClass.excludedStyles = currentClass.excludedStyles.concat(base.excludedStyles);
				currentClass.excludedEffects = currentClass.excludedEffects.concat(base.excludedEffects);
			}
			
			if (base.name == "Object")
				break;
			else
				base = classTable[base.base_name];
		}
	}
}

function processMetaData(rawDocXML:XML)
{
	if (verbose) {
		print("processing metadata");	
		print("-----------------");
	}
	var mc=0;
	for each (var metadata in rawDocXML..metadata)
	{
	 	if (metadata.hasOwnProperty("Style"))
 		{
			if ((metadata.Style.hasOwnProperty("private") && configRec.includePrivates == false) || metadata.Style.metadata.hasOwnProperty("Deprecated"))
				continue;
			var style:XML = metadata.Style[0];
			style.setName("style");
			var name = String(style.@name);
 			var fullname	= String(style.@owner);
 			var myClass  = classTable[fullname];
 			if (typeof myClass == undefined || myClass == null)
 			{
                if (refErrors) {
 				print("   Can not resolve style class name: " + fullname );
                }
 				continue;
 			}
			
			if (myClass.node.excludes != undefined)
			{
				var styleName = name;
				if (myClass.node..exclude.(@kind == "style" && @name == styleName).length() > 0)
				{
					if (verbose)
						print("Excluding style " + name + " from " + myClass.name);
					continue;
				}
			}
			
			var styleType = String(style.@type);
			if (styleType != null && styleType != "")
			{
				var see = processSeeTag(fullname, styleType);
				if (see != null)
					style.@typeHref = see.@href;
			}
 			
 			var fullDesc:String = String(style.description);
 			if (fullDesc != "")
 				style.shortDescription = descToShortDesc(fullDesc);

			myClass.node.styles.style += style;	
 		}
 		if (metadata.hasOwnProperty("Effect"))
 		{
			if ((metadata.Effect[0].hasOwnProperty("private") && configRec.includePrivates == false) || metadata.Effect[0].metadata.hasOwnProperty("Deprecated"))
				continue;
				
			var effect:XML = metadata.Effect[0];
			effect.setName("effect");
			var name = String(effect.@name);
 			var fullname	= String(effect.@owner);
 			var myClass  = classTable[fullname];
 			if (typeof myClass == undefined || myClass == null)
 			{
                if (refErrors) {
 				print("   Can not resolve effect class name: " + fullname );
                }
 				continue;
 			}
 			
			if (myClass.node.excludes != undefined)
			{
				var effectName = name;
				if (myClass.node..exclude.(@kind == "effect" && @name == effectName).length() > 0)
				{
					if (verbose)
						print("Excluding effect " + name + " from " + myClass.name);
					continue;
				}
			}
		
 			var fullDesc:String = String(effect.description);
 			if (fullDesc != "")
 				effect.shortDescription = descToShortDesc(fullDesc);
				
 			myClass.node.effects.effect += effect;	
		}

 		if (metadata.hasOwnProperty("Event"))
 		{
			if ((metadata.Event[0].hasOwnProperty("private") && configRec.includePrivates == false) || metadata.Event[0].metadata.hasOwnProperty("Deprecated"))
				continue;
				
			var event:XML = metadata.Event[0];

 			var name     = String(event.@name);
 			var fullname = String(event.@owner);
 			if (verbose)
 				print("   processing event: '" + name + "' for class:" + fullname);
		 	
 			var qualifiedName:QualifiedFullName = decomposeFullClassName(fullname);
 			var myClass  = classTable[fullname];
 			if (typeof myClass == undefined || myClass == null)
 			{
                if (refErrors) {
 				print("   Can not resolve generated event class name: " + fullname );
                }
 				continue;
 			}
		
			if (myClass.node.excludes != undefined)
			{
				var eventName = name;
				if (myClass.node..exclude.(@kind == "event" && @name == eventName).length() > 0)
				{
					if (verbose)
						print("Excluding event " + name + " from " + myClass.name);
					continue;
				}
			}
				
 			var nuEvent:XML = <event name={name} owner={fullname}></event>;
	 		var fullDesc:String = String(event.description.normalize());
 			if (fullDesc != "") {
				nuEvent.description = new XML("<description><![CDATA[" + event.description.normalize() + "]]></description>");
 				nuEvent.shortDescription = descToShortDesc(fullDesc);
			}
		//eventType fix	
 			if (metadata.Event[0].hasOwnProperty("eventType"))
  			{
				var eventType = String(metadata.Event[0].eventType).replace(/\s+/,"");
  				var eventRec = eventTable[eventType];
  				if (eventRec != undefined && eventRec != null) {
					var see = processSeeTag(fullname, eventType.substring(0, eventType.lastIndexOf(".")) + "#" + eventType.substring(eventType.lastIndexOf(".") + 1));
//  					nuEvent.classRef = createClassRef(eventRec.decompName, myClass.decompName);
					nuEvent.eventType = <eventType href={see.@href} label={see.@label}></eventType>;
  					var eventDesc:String = String(eventRec.eventCommentTable[name]);
  					if (eventDesc != "undefined" && eventDesc != "") {
						nuEvent.eventDescription = new XML("<eventDescription><![CDATA[" + eventDesc + "]]></eventDescription>");
//S  						nuEvent.eventDescription = <eventDescription>{"<![CDATA[" + eventDesc + "]]>"}</eventDescription>;
  						if (fullDesc == "") {						
  							nuEvent.shortDescription = descToShortDesc(eventDesc);
  						}
   					}
   				}
 				else {
                    if (refErrors)
   					print("   Failed to resolve generated event name: " + name + " for class:" + myClass.decompName.fullClassName);
  				}
				if (eventType.lastIndexOf(".") != -1)
				{
					nuEvent.@typeName = eventType.substring(eventType.lastIndexOf(".") + 1);
				}
   			}
		 	
		 	event.@fullname = fullname; // to use as a basis for generating href's for see references.
		 	processVersions(event, nuEvent, myClass.name + "." + event.@name);
			processCustoms(event,nuEvent, false /*use params*/, "", "", "");
			
			if (event.hasOwnProperty("example"))
				nuEvent.example = event.example;
				
			// add event type as a sees tag
			var eventObjectType = String(event.@type);
			if ((eventObjectType != null) && (eventObjectType != ""))
			{
				var see = processSeeTag(fullname, eventObjectType);
				var eventObject:XML = <eventObject href={see.@href} label={see.@label}></eventObject>;
				nuEvent.eventObject = eventObject;
			}		

 			if (myClass != undefined && event != null)
 			{
 				if (myClass.node.hasOwnProperty("eventsGenerated") == false )  {
 					myClass.node.eventsGenerated  = <eventsGenerated></eventsGenerated>;
 				} 
 				myClass.node.eventsGenerated.event += nuEvent;	
 			}
 			else
 			{
 				print("*** Internal error: can't find class for event: " + qualifiedName.fullClassName);
 				print("myClass: " + myClass + " event: " + event);
 				debug_decomposeFullName(fullname);
 			}
 		}
 		
		if (metadata.hasOwnProperty("Bindable"))
		{
			if ((metadata.Bindable[0].hasOwnProperty("private") && configRec.includePrivates == false) || metadata.Bindable[0].metadata.hasOwnProperty("Deprecated"))
				continue;
				
			var bindable:XML = metadata.Bindable[0];
			var fullname = String(bindable.@owner);
			if (verbose)
				print("   processing bindable: " + fullname);

			var qualifiedName:QualifiedFullName = decomposeFullClassName(fullname);
			var myClass  = classTable[fullname];

			if (myClass == undefined || myClass == null)
			{
				qualifiedName = decomposeFullMethodOrFieldName(fullname);
				myClass = classTable[qualifiedName.fullClassName];	
				var found:Boolean = false;

				if (myClass != undefined && myClass.fields != null)
				{
					for each(var f in myClass.fields.field)
					{
						if (f.@fullname == fullname)
						{
							var bindableField:XML = XML(f);
							if (!bindableField.hasOwnProperty("isBindable"))
							{
								bindableField.@isBindable = true;
								found = true;
								break;
							}
						}
					}
				}			
				if (!found)
				{
					// process later
					bindableTable[fullname] = "isBindable";
				}
			}
			else
			{
				// process later
				bindableTable[fullname] = "isBindable";
			}
		}
		if (metadata.hasOwnProperty("DefaultProperty"))
		{
			var defaultProperty:XML = metadata.DefaultProperty[0];
			var fullname = String(defaultProperty.@owner);
			if (verbose)
				print("   processing defaultProperty: " + fullname);
				
			var myClass = classTable[fullname];
			if (myClass != undefined && myClass != null)
				myClass.node.defaultProperty = <defaultProperty name={defaultProperty.@name}></defaultProperty>;
		}
	}
	if (verbose) {
		print("Done processing metadata");	
		print("-----------------");
	}
}


function processMethods(rawDocXML:XML)
{	
	if (verbose) {
		print("processing methods");
		print("------------------");
	}
	for each (var method in rawDocXML..method)
	{
 		var name		= String(method.@name);
 		var fullname	= String(method.@fullname);
 		if (verbose)
 			print("   processing method: " + fullname);
	 	
 		var qualifiedName:QualifiedFullName = decomposeFullMethodOrFieldName(fullname);
		if (hidePackage(qualifiedName.packageName))
			continue;
		
		var bindable:Boolean = false;
		if (bindableTable[fullname] != null)
		{
			bindable = true;
		}

		if (bindableTable[qualifiedName.fullClassName] != null)
		{
			bindable = true;
		}

 		var myClass  = classTable[qualifiedName.fullClassName];

		// skip class methods in the private namespace (always)
 		if (myClass == null || !myClass.isInterface)
		{
//constructors are always considered public, even if they're not declared that way
			if (qualifiedName.classNames[qualifiedName.classNames.length-1] == undefined ||
				name != qualifiedName.classNames[qualifiedName.classNames.length-1].toString())
			{	
				if (hideNamespace(qualifiedName.methodNameSpace))
					continue;
			}
		}
		if (myClass != undefined && myClass.node.excludes != undefined)
		{
			var methodName = name;
			var kind:String = qualifiedName.getterSetter.length != 0 ? "property" : "method";
			if (myClass.node..exclude.(@kind == kind && @name == methodName).length() > 0)
			{
				if (verbose)
					print("Excluding " + kind + " " + methodName + " from " + myClass.name);
				continue;
			}
		}
		
 		if (typeof myClass == undefined || myClass == null)
 		{
 			// not an error, probably a method from a class marked @private
 			//print("*** Internal error:  could not find class for method: " + fullname + " in class: " + qualifiedName.fullClassName);
 			// debug_decomposeFullName(fullname);
 		}
 		else // check if this is a getter/setter method and convert to a field
 		if (myClass != undefined && qualifiedName.getterSetter.length != 0)
 		{
 			if (verbose)
 				print("   changing method: " + fullname + " into a field (its a getter or setter)");
 			// todo: need to know if this is a static method or not
 			var field:XML = XML(<field name={name} fullname={fullname} accessLevel={qualifiedName.methodNameSpace} isConst='false' isStatic={method.@isStatic}></field>); 

 //			field.accessLevel = qualifiedName.methodNameSpace;
	 		if (method.hasOwnProperty("description"))
 			{
 				var fullDesc:String = String(method.description);
 				field.description = method.description;
 				field.shortDescription = descToShortDesc(fullDesc);
 			}

			if (bindable)
			{
				field.@isBindable = "true";
			}

 			processVersions(method, field, myClass.name + "." + method.@name);

 			if (myClass.fieldGetSet[name] == undefined)
 				myClass.fieldGetSet[name] = 0;
 				
 			// if this is the setter, but the getter was marked @private, skip it as well.  
// 			else if (myClass.fieldGetSet[name] == -99)
// 				continue;
			
	 		// skip method tagged with @private, even if they are public
			if ((method.hasOwnProperty("private") && configRec.includePrivates == false) || method.metadata.hasOwnProperty("Deprecated"))
			{
//				if (method.@isOverride=="true")
//					myClass.methodOverrideTable[String(method.@name)] = true;
					
//				myClass.fieldGetSet[name] = -99;

				if (myClass.privateGetSet[name] == undefined)
					myClass.privateGetSet[name] = 0;	
					
				if (qualifiedName.getterSetter == "Get")
				{
					myClass.privateGetSet[name] += 1;
					myClass.fieldGetSet[name] += 1;
				}
				else
				{
					myClass.privateGetSet[name] +=2;
					myClass.fieldGetSet[name] += 2;
				}

				continue;
			}

  			if (qualifiedName.getterSetter == "Get")
  			{
 				field.@type = String(method.@result_type);
				var fieldTypeClass = classTable[String(field.@type)];
	 			if (fieldTypeClass != null)
 					field.classRef = createClassRef(fieldTypeClass.decompName, myClass.decompName);

 				myClass.fieldGetSet[name] += 1;	
 			}
 			else
 			{
				field.@type = String(method.@param_types);
				var fieldTypeClass = classTable[String(field.@type)];
				if (fieldTypeClass != null)
					field.classRef = createClassRef(fieldTypeClass.decompName, myClass.decompName);
					
			 	myClass.fieldGetSet[name] += 2;
 			}
			//print( " @@ writing get/set info for: " + name + " in " + myClass.decompName.fullClassName + " = " + myClass.fieldGetSet[name]);
 			
 			if (method.hasOwnProperty("example"))
 			{	
 				field.example = method.example;
 			}
 			if (method.hasOwnProperty("throws"))
	 		{
 				for each(var t in method["throws"])
 				{
	 				field.canThrow += createCanThrow(t, myClass.decompName);	
 				}
	 		}
 			if (method.hasOwnProperty("event"))
 			{
 				field.event = method.event;
 			}
			field.@inheritDoc = method.hasOwnProperty("inheritDoc");
	
			processCustoms(method, field, false /*use params*/, "", "", "");
			
 			if (myClass.fieldCount == 0) {
				myClass.fields = <fields></fields>;
 				myClass.fields.field = field;
 				myClass.fieldCount++;
 			} else {
 				var foundField:XML = null;
 				var numChildren = myClass.fields.field.length();
 				
 				// todo:  this is brute force / hardly optional way of looking for this
				for (var xx=0; xx < numChildren; xx++ )
 				{
 					if (myClass.fields.field[xx].@name.toString() == field.@name.toString()) {
 						foundField = myClass.fields.field[xx];
 						break;
 					} 
 				}
 				
 				if (foundField == null) {
 					myClass.fields.field += field;
 					myClass.fieldCount++;
 				} else if (qualifiedName.getterSetter == "Get") { // could have seen setter first.  Need to set @type and classRef
 					foundField.@type = field.@type;
 					foundField.classRef = field.classRef;
 				}
 					
 			}
/*
 			   var isOverride = String(method.@isOverride);
 			   if (isOverride == "")
 			      isOverride = "false";
 			      
 			   if (isOverride)
 					myClass.methodOverrideTable[String(method.@name)] = true;
*/
		}	
 		else if (myClass != undefined)
 		{
 		/*
 		    <method name="hide" accessLevel="public" static="true" >
				<description>Method; hides the pointer in a SWF file. The pointer is visible by default.        </description>
				<short-description>Method; hides the pointer in a SWF file.</short-description>
				<version>Flash Player 5.      </version>
				<author></author>
				<return type="Number" >
					<description>An integer; either &lt;code&gt;0&lt;/code&gt; or &lt;code&gt;1&lt;/code&gt;. If the mouse pointer was hidden before the call to &lt;code&gt;Mouse.hide(),&lt;/code&gt; then the return value is &lt;code&gt;0&lt;/code&gt;. If the mouse pointer was visible before the call to &lt;code&gt;Mouse.hide()&lt;/code&gt;, then the return value is &lt;code&gt;1&lt;/code&gt;.        </description>
				</return>
		*/
	 		// skip method tagged with @private, even if they are public
			if ((method.hasOwnProperty("private") && configRec.includePrivates == false) || method.metadata.hasOwnProperty("Deprecated"))
			{
//				if (method.@isOverride=="true")
//					myClass.methodOverrideTable[String(method.@name)] = true;
				continue;
			}

 			var nuMethod:XML;
 			var isConstructor = false;

 			if (qualifiedName.classNames[qualifiedName.classNames.length-1] != undefined &&
				method.@name.toString() == qualifiedName.classNames[qualifiedName.classNames.length-1].toString())
 			{
 				nuMethod = <constructor name={method.@name} fullname={method.@fullname} accessLevel={qualifiedName.methodNameSpace} result_type={method.@result_type}></constructor>;
 				isConstructor = true;
 			}
 			else
 			{
 			   var isFinal = String(method.@isFinal);
 			   var isOverride = String(method.@isOverride);
 			   var isStatic = String(method.@isStatic);

 			   if (isFinal == "")
 			      isFinal = "false";
 			   if (isOverride == "")
 			      isOverride = "false";
 			      
 			   if (isOverride)
 					myClass.methodOverrideTable[String(method.@name)] = true;

 			   nuMethod = <method name={method.@name} fullname={method.@fullname} accessLevel={qualifiedName.methodNameSpace} result_type={method.@result_type} isFinal={isFinal} isOverride={isOverride} isStatic={isStatic}></method>;
 			}
 			if (method.hasOwnProperty("example"))
	 		{
	 			nuMethod.example = method.example;
	 		}
	 		if (method.hasOwnProperty("throws"))
	 		{
 				for each(var t in method["throws"])
 				{
 					nuMethod.canThrow += createCanThrow(t, myClass.decompName); 		
 				}
	 		}
	 		if (method.hasOwnProperty("event"))
	 		{
	 			var numEvents = method.event.children().length();
	 			
	 			for (var x = 0; x < numEvents; x++)
	 			{
	 				var oldEvent = method.event[x];
	 				var fullEventStr = String(oldEvent); 
	 				var eventCommentStr = "";
	 				var nextSpaceIndex = fullEventStr.indexOf(" ");
	 				var eventClassStr = null;
	 				if (nextSpaceIndex == -1)
	 				{
	 					eventClassStr = "Event";
	 					eventCommentStr = fullEventStr;
	 					nextSpaceIndex = fullEventStr.length-1
	 				}
	 					
	 				var eventName = fullEventStr.substring(0,nextSpaceIndex);
	 				var nuEvent = <event name={eventName}></event>;
	 				
	 				if (eventClassStr == null)
	 				{
	 					var lastSpaceIndex = nextSpaceIndex+1;
	 					nextSpaceIndex = fullEventStr.indexOf(" ", lastSpaceIndex);
	 					if (nextSpaceIndex == -1) {
	 						eventClassStr = "Event";
	 						eventCommentStr = fullEventStr.substring(lastSpaceIndex+1, fullEventStr.length);
	 					}
	 					else {
	 						eventClassStr = fullEventStr.substring(lastSpaceIndex, nextSpaceIndex);
	 						eventCommentStr = fullEventStr.substring(nextSpaceIndex+1, fullEventStr.length);
	 					}
	 				}
	 				
					nuEvent.description = <description>{eventCommentStr}</description>;
//	 				nuEvent.description = <description>{"<![CDATA[" + eventCommentStr + "]]>"}</description>;

					var eventClass = classTable[eventClassStr];
	 				if (eventClass != null) {
	 					nuEvent.classRef = createClassRef(eventClass.decompName, myClass.decompName);
	 				} else {
						if (verbose)
							print("   Can not resolve event name: " + eventClassStr + " looking in flash.events");
						eventClass = classTable["flash.events:" + eventClassStr];
						if (eventClass != null) {
							nuEvent.classRef = createClassRef(eventClass.decompName, myClass.decompName);
						} else {
                            if (refErrors) {
							print("   Can not resolve event name: " + eventClassStr);
							print("   in processMethods() for method: " + fullname);
                            }
						}
					}

	 				nuMethod.event += nuEvent;
	 			}
	 		}
	 		
	 		if (method.hasOwnProperty("description"))
 			{
 				var fullDesc:String = String(method.description);
 				nuMethod.description = method.description;
 				nuMethod.shortDescription = descToShortDesc(fullDesc);
 			}

 				
 			if (method.hasOwnProperty("author"))
 			{
 				nuMethod.author = method.author;
 			}
 			
 			processVersions(method, nuMethod, myClass.name + "." + method.@name);
			
 			if (method.hasOwnProperty("return"))
 			{
				nuMethod.result = new XML("<result type='" + method.@result_type + "'><![CDATA[" + String(method["return"]) + "]]></result>");
// 				nuMethod.result = <result type={method.@result_type}>{"<![CDATA[" + String(method["return"]) + "]]>"}</result>;
 			}
 			else
 			{
 				nuMethod.result = <result type={method.@result_type}></result>;
 			}
			
			nuMethod.@inheritDoc = method.hasOwnProperty("inheritDoc");
		
			var returnClass = classTable[String(method.@result_type)];
	 		if (returnClass != null)
 				nuMethod.result.classRef = createClassRef(returnClass.decompName, myClass.decompName);
 				
 			var paramNames = String(method.@param_names);
 			var paramTypes = String(method.@param_types);
 			var paramDefaults = String(method.@param_defaults);
			
 			processCustoms(method, nuMethod, true /*use params*/, paramNames, paramTypes, paramDefaults, myClass);

			if (isConstructor)
			{
				if (myClass.constructorCount == 0)
					myClass.constructors = <constructors></constructors>;
				myClass.constructors.constructor += nuMethod;
				myClass.constructorCount++;	
			}
			else
			{
				if (myClass.methodCount == 0)
					myClass.methods = <methods></methods>;
				myClass.methods.method += nuMethod;
				myClass.methodCount++;	
			}
				
			/*
 			if (myClass.methodCount != 0)
 				myClass.methods[myClass.methodCount] = nuMethod;
 			else
 				myClass.methods = new XMLList(nuMethod);
 			*/
	 			
			
 		}
 		else
 		{
 			print("can't find method for class: " + qualifiedName.fullClassName);
 			debug_decomposeFullName(fullname);
 		}
	 	
	}
}





function simpleRefNameToRelativeLink(classNameStr:String, packageName:String):String
{
	var pathSpec:Array = classNameStr.split('.');
	var currentPathSpec:Array = packageName.split('.');
	var relativeLink = "";
	
	// if its empty or starts with a #, don't bother: its not to another class
	if (classNameStr.length == 0 || classNameStr.charAt(0) == "#")
	{
		return null;
	}
	
	// GlowFilter or flash.core.GlowFiler  must match the debug name: flash.core:GlowFilter
	var lastDotPos = classNameStr.lastIndexOf('.');
	if (lastDotPos != -1)
	{
		var fullClassNameStr = classNameStr.substring(0, lastDotPos) + ":" + classNameStr.substring(lastDotPos+1, classNameStr.length);
		if (classTable.hasOwnProperty(classNameStr) == false)
		{
			// perhaps a nested class reference?
/*			var nextLastDotPos = classNameStr.substring(0, lastDotPos).lastIndexOf('.');
			if (nextLastDotPos != -1)
			{
				fullClassNameStr = classNameStr.substring(0, nextLastDotPos) + ":" + 
								   classNameStr.substring(nextLastDotPos+1, lastDotPos) + "/" + 
								   classNameStr.substring(lastDotPos+1, classNameStr.length);
			}
*/			
			if (classTable.hasOwnProperty(fullClassNameStr) == false)
			{
				if (rawDocXML.method.(@fullname.indexOf(classNameStr.replace(":", ".") + ":") != -1) == undefined &&
					rawDocXML.field.(@fullname.indexOf(classNameStr.replace(":", ".") + ":") != -1) == undefined)
				{
                    if (refErrors) {
					print("   Failed to resolve reference to : " + classNameStr + " fullClassNameStr: " + fullClassNameStr);
                    }
					return null;
				}
				else if (packageContentsTable[classNameStr.replace(":", ".")] != undefined)
				{
					pathSpec = classNameStr.split(/[:.]/);
					pathSpec[pathSpec.length] = "package";
				}
				else
				{
                    if (refErrors) {
					print("   Failed to resolve reference to simple name: " + classNameStr);
					}
					return null;
				}
			}
		}
	}
	else
	{
		var classRec = classTable[classNameStr];
		var packageFound:Boolean = false;

		if (classRec == null)
		{
			classRec = classTable[packageName + ":" + classNameStr];
			if (classRec == null)
			{
				if (rawDocXML.method.(@fullname.indexOf(classNameStr.replace(":", ".") + ":") != -1) == undefined &&
					rawDocXML.field.(@fullname.indexOf(classNameStr.replace(":", ".") + ":") != -1) == undefined)
				{						
					if (classNameStr == "global")
					{
						classNameStr = "";
						packageFound = true;
					}
					else
					{
	                    if (refErrors) {
						print("   Failed to resolve reference to simple name: " + classNameStr);
						}
						return null;
					}
				}
				else if (packageContentsTable[classNameStr.replace(":", ".")] != undefined)
				{
					packageFound = true;
				}
				else
				{
                    if (refErrors) {
					print("   Failed to resolve reference to simple name: " + classNameStr);
					}
					return null;
				}
			}
		}
		
		//var qualifiedName:QualifiedFullName = decomposeFullClassName(classRec.fullname);
		if (packageFound)
		{
			pathSpec = classNameStr.split(/[:.]/);
			pathSpec[pathSpec.length] = "package";
		}
		else if (classRec.decompName.packageName != "")
		{
			pathSpec = classRec.decompName.packageName.split('.');
			pathSpec[pathSpec.length] = classNameStr;
		}
		else
		{
			pathSpec[0] = classNameStr;
		}
	}
	
	var cp:int;
	for(cp = 0; cp < currentPathSpec.length; cp++)
	{
		if (pathSpec[cp] != currentPathSpec[cp])
			break;
	}
	if (cp == currentPathSpec.length)
	{
		if (cp < pathSpec.length)
		{
			relativeLink = pathSpec[cp];
			for(var rp = cp+1; rp < pathSpec.length; rp++)
				relativeLink = relativeLink.concat("/" + pathSpec[rp]);
		}
	}
	else
	{
		if (cp < pathSpec.length)
			relativeLink = relativeLink.concat(pathSpec[cp]);
		for(var rp = cp+1; rp < pathSpec.length; rp++)
			relativeLink = relativeLink.concat("/" + pathSpec[rp]);

//		while (cp++ < currentPathSpec.length)
		if (packageName != "")
		{
			for (var fp = cp; fp < currentPathSpec.length; fp++)
				relativeLink = "../" + relativeLink;
		}
	}
	relativeLink = relativeLink + ".html";
	return relativeLink.replace("$", "").replace(":", "/");
}

/*
 * removes \n, \r, and \t and optionally ' '
 */
function removeWhitespace(str:String, removeSpace:Boolean):String
{
	while (str.indexOf("\n") != -1)
		str = str.replace("\n","");

	while (str.indexOf("\r") != -1)
		str = str.replace("\r","");

	while (str.indexOf("\t") != -1)
		str = str.replace("\t","");

	if (removeSpace)
	{
		while (str.indexOf(" ") != -1)
			str = str.replace(" ","");
	}

	return str;
}

function normalizeString(str:String):String
{
	return str.replace(/^[\s]+|[\s]+$/,"").replace(/\s+/," ");
}

function processSeeTag(fullname:String, seeStr:String):XML
{
	if (verbose) print("Processing @see tag[" + seeStr + "] from " + fullname);
	
	var labelStr:String = "";
	var hrefStr:String = "";
	
	seeStr = normalizeString(seeStr);
	if (seeStr.length == 0)
	{
		print("ERROR: Empty @see string in " + fullname);
		return <see></see>;
	}
	
	var spaceIndex = seeStr.indexOf(" ");	
	if (seeStr.indexOf("\"") == 0)
	{
		hrefStr = "";
		labelStr = seeStr.replace(/^["]|["]$/,"");
	}
	else 
	{
		if (spaceIndex != -1)
		{
			hrefStr = seeStr.substring(0, spaceIndex);
			labelStr = seeStr.substring(spaceIndex + 1, seeStr.length);
		}
		else
		{
			hrefStr = seeStr;
			labelStr = seeStr;
		}
		
		if (hrefStr.indexOf("http://") == -1 && hrefStr.indexOf(".html") == -1)
		{			
			var poundLoc = hrefStr.indexOf("#");
			var lastDot = hrefStr.lastIndexOf(".");
			
			if (lastDot != -1)
				hrefStr = (hrefStr.slice(0, lastDot) + ":" + hrefStr.slice(lastDot + 1, 999999));
				
			hrefStr = hrefStr.replace("event:", "event!");
			hrefStr = hrefStr.replace("style:", "style!");
			hrefStr = hrefStr.replace("effect:", "effect!");
			
			if (poundLoc != -1)
			{
				var qualifiedName:QualifiedFullName = (fullname.indexOf("/") == -1) ? decomposeFullClassName(fullname) :
							decomposeFullMethodOrFieldName(fullname);

				classNameStr = hrefStr.substring(0, poundLoc);
				memberNameStr = hrefStr.substring(poundLoc + 1, hrefStr.length);
			
				if (classNameStr == "")
					classNameStr = qualifiedName.fullClassName;
				relativeLink = simpleRefNameToRelativeLink(classNameStr, qualifiedName.packageName);						
				if (relativeLink != null)
				{
					hrefStr = relativeLink + "#" + memberNameStr;
				}
				else 
				{
					// simpleRefNameToRelativeLink already prints error for the link string
					// now print where problem occurs
//					print("      in processSeeTag() for : " + hrefStr + " from : " + fullname);
					hrefStr = "";
				}
			}
			else
			{
				var qualifiedName:QualifiedFullName = decomposeFullClassName(fullname);
				relativeLink = simpleRefNameToRelativeLink(hrefStr, qualifiedName.packageName);	
				if (relativeLink != null)
				{			
					if (relativeLink.indexOf("/package.html") != -1)
						relativeLink = relativeLink.replace("/package.html", "/package-detail.html");

					hrefStr = relativeLink;
				} 
				else if (hrefStr == "~~" || hrefStr == "Null" || hrefStr == "void")
				{
					var href = "";
					var packageNames:Array = qualifiedName.packageName.split(".");
					for (var i:int = 0; i < packageNames.length; i++)
						href += "../";
						
					href += "specialTypes.html#";
					if (hrefStr == "~~")
					{
						href += "*";
						if (labelStr == "~~")
							labelStr = "*";
					}
					else
					{
						href += hrefStr;
					}
					
					hrefStr = href;
				}
				else
				{
					// simpleRefNameToRelativeLink already prints error for the link string
					// now print where problem occurs
//					print("      in processSeeTag() for : " + seeStr + " from : " + fullname);
					hrefStr = "";
				}		
			}
			
			hrefStr = hrefStr.replace(":", "/");
			hrefStr = hrefStr.replace("event!", "event:");
			hrefStr = hrefStr.replace("style!", "style:");
			hrefStr = hrefStr.replace("effect!", "effect:");
			
			if (labelStr.indexOf("#") == 0) 
				labelStr = labelStr.replace("#", "");
			else 
				labelStr = labelStr.replace("#", ".");

			labelStr = labelStr.replace("event:", "");
			labelStr = labelStr.replace("style:", "");
			labelStr = labelStr.replace("effect:", "");
			labelStr = labelStr.replace("global.", "");
		}
	}
	
	var result:XML = new XML("<see href='" + hrefStr + "' label='" + labelStr + "'></see>");
//	result = <see href={hrefStr} label={labelStr}></see>;
	if (verbose) print("Done processing @see tag. Returning " + result.toXMLString());
	
	return result;
}

function processSeeTag2(fullname:String, seeStr:String):XML
{
	var c=0;
	var labelStr:String = "";
	var hrefStr:String = "";
	
	seeStr = removeWhitespace(seeStr, false);
		
	var sepLoc = seeStr.indexOf(" ");

	if (seeStr.indexOf('\"') != -1)
	{
		hrefStr = "";
		labelStr = seeStr.replace('\"',"");
	}	
	else if (sepLoc != -1)
	{
		labelStr = seeStr.substring(sepLoc+1, seeStr.length); // take the string after the space and make it the label;
		hrefStr  = seeStr.substring(0, sepLoc); // take the string before the space and make it the href; 
	} 
	else 
	{
		labelStr = seeStr.substring(0,seeStr.length);
		hrefStr = seeStr.substring(0,seeStr.length);
	}
	if (hrefStr.replace(/\s/, "") == 0 && labelStr.indexOf("<") == -1)
		hrefStr = labelStr;
	else if (labelStr.replace(/\s/, "") == 0)
		labelStr = hrefStr;
	
	var fallbackLabel:String = new String(hrefStr);

	if (hrefStr != "" && hrefStr.indexOf("http://") == -1 && hrefStr.indexOf(".html") == -1) // the href does not contain "http://" 
	{
		var memberNameStr:String = "";
		var classNameStr:String = "";
		var qualifiedName:QualifiedFullName = decomposeFullClassName(fullname);
		var relativeLink:String = "";
		
		var poundLoc = hrefStr.indexOf("#");
		hrefStr = hrefStr.replace("event:","event!");
		hrefStr = hrefStr.replace("style:","style!");
		hrefStr = hrefStr.replace("effect:","effect!");
		hrefStr = hrefStr.replace(/:/,".");
		hrefStr = hrefStr.replace(/\//,".");
		var lastDot = hrefStr.lastIndexOf('.');
		
		if (lastDot != -1)
			hrefStr = (hrefStr.slice(0, lastDot) + ":" + hrefStr.slice(lastDot + 1, 999999));
			
		if (poundLoc != -1)   // the href contains "#" 
		{
			memberNameStr = hrefStr.substring(poundLoc+1, hrefStr.length); // take the string after the # and make it the memberName;
			classNameStr = hrefStr.substring(0,poundLoc); // take the string before the # and make it the className (if there is nothing before the #, assume it's a link within the same file);
			qualifiedName = decomposeFullMethodOrFieldName(fullname);

			if (classNameStr == "")
				classNameStr = qualifiedName.fullClassName;

			relativeLink = simpleRefNameToRelativeLink(classNameStr, qualifiedName.packageName);			
			if (relativeLink != null)
				hrefStr = relativeLink + "#" + memberNameStr; // reassemble the className and memberName to make a complete relative url, like MovieClip.html#stop, and make this the new href;
			else {
				// simpleRefNameToRelativeLink already print error for the link string, now print where problem occurs
//				print("      in processSeeTag() for : " + hrefStr + " from : " + fullname);
				hrefStr = "";
			}
		}
		else // no "#"
		{ 
/*
			hrefStr = hrefStr.replace(/:/,".");
			var index = hrefStr.lastIndexOf('/');
			if (index == -1)
				index = hrefStr.lastIndexOf('.');
				
			if (index != -1)
			{
				var firstPart:String = hrefStr.slice(0, index);
//print("firstPart="+firstPart);
				hrefStr = firstPart + ":" + hrefStr.slice(index + 1);
			}
*/
			relativeLink = simpleRefNameToRelativeLink(hrefStr, qualifiedName.packageName);	
			if (relativeLink.indexOf("/package.html") != -1)
				relativeLink = relativeLink.replace("/package.html", "/package-detail.html");
			if (relativeLink != null)
			{
				//if it does, then get its location relative to the current class and build a relative url like /flash/display/MovieClip.html;
				hrefStr = relativeLink;
			} 
			else
			{
				// simpleRefNameToRelativeLink already print error for the link string, now print where problem occurs
//				print("      in processSeeTag() for : " + seeStr + " from : " + fullname);
				hrefStr = ""; // if it is not a class, then leave the href blank (so only the label will be filled in);
			}
		}
	}
	else // it contains "http://" or its ""
	{
	// no action needed; the href and label are already set 
	}
	
	var result:XML;
//	if (labelStr == "class")
//		labelStr = "Class";
		
	if (hrefStr != "") 
	{
		if (labelStr.replace(/\s/, "").length == 0)
			labelStr = fallbackLabel;
			
		if (labelStr.indexOf("#") == 0) {
			labelStr = labelStr.replace("#", "");
		} else {
			labelStr = labelStr.replace("#", ".");
		}
		labelStr = labelStr.replace("event:","");
		labelStr = labelStr.replace("style:","");
		labelStr = labelStr.replace("effect:","");
		
		hrefStr = hrefStr.replace(/:/,"/");
		hrefStr = hrefStr.replace("event!","event:");
		hrefStr = hrefStr.replace("style!","style:");
		hrefStr = hrefStr.replace("effect!","effect:");
		
		labelStr = labelStr.replace(".html", "");

		result = <see href={hrefStr}></see>;
		result.@label = labelStr;
	}
	else
	{
		result = <see></see>;
		result.@label = labelStr;
	}
	
	return result;
}

function processIncludeExampleTag(fullname:String, exampleStr:String):XML
{
	var noswf:Boolean = false;

	// remove -noswf from the @includeExample string
	if (exampleStr.indexOf("-noswf") != -1)
	{
		noswf = true;
		exampleStr = exampleStr.slice(0, exampleStr.indexOf("-noswf"));
	}

	// remove whitespace from @includeExample string
	exampleStr = removeWhitespace(exampleStr, true);

	// generate the examplefilename string
	var examplefilenameStr:String = exampleStr;
	var index:int = examplefilenameStr.lastIndexOf('/');
	if (index != -1)
	{
		examplefilenameStr = examplefilenameStr.slice(index + 1, 999999);
	}

	// generate the swfpart string
	var swfpartfileStr:String = exampleStr;

	index = swfpartfileStr.lastIndexOf('.');
	if (index != -1)
	{
		swfpartfileStr = swfpartfileStr.slice(0, index);
		swfpartfileStr += ".swf";
	}

	// construct the location of the mxml code and read in the mxml code
	var codepart:String = null;
	var codefilenameStr:String;

	try
	{
		codefilenameStr = String(config.includeExamplesDirectory);
		codefilenameStr += "/";
		var qualifiedName:QualifiedFullName = decomposeFullClassName(fullname);
		codefilenameStr += qualifiedName.packageName.replace(/\.+/g, "/");
		codefilenameStr += "/";
		codefilenameStr += exampleStr;
		codepart = File.read(codefilenameStr);
		codepart = codepart.replace(/\t/, "    ");
	}
	catch (error:Error)
	{
		print("The file specified in @includeExample, " + exampleStr + ", cannot be found at " + codefilenameStr);
	}

	var result:XML = new XML("<includeExample examplefilename='" + examplefilenameStr + "'></includeExample>");
//	result.@examplefilename = examplefilenameStr;

	if (!noswf)
	{
//		var loader:flash.display.Loader = new flash.display.Loader();
//		var request:flash.net.URLRequest = new flash.net.URLRequest(swfpartfileStr);
//		loader.load(request);
//print("width="+loader.contentLoaderInfo.width);
//print("height="+loader.contentLoaderInfo.height);
		result.swfpart = new XML("<swfpart file='" + swfpartfileStr + "'></swfpart>");
//		result.swfpart.@file = swfpartfileStr;
	}

	if (codepart != null)
	{
		// don't wrap in CDATA because mxml samples have CDATA sections that would have
		// to be escaped (i.e. ']]'), don't use XML constructor because it doesn't
		// escape the content
		result.codepart = codepart;
	}

	return result;
}

function processCustoms(node:XML, record:XML, useParams:Boolean, paramNames:String, paramTypes:String, paramDefaults:String, fromClass:AClass=null)
{
	if (verbose) print("Processing customs");
	
	var children = node.children();
		
	if (node.length() != 0)
	{
		var customCount = 0;
		var seesCount = 0;	
		var paramCount = 0;
		var includeExamplesCount = 0;
 		var sees:XML	= <sees></sees>;
		var customs:XMLList	= new XMLList();
		var customs2:XML = <customs></customs>;
		var includeExamples:XML = <includeExamples></includeExamples>;
		var params:XML	= <params></params>;
		var lastParamName = 0;
		var lastParamType = 0;
		var lastParamDefault = 0;
		
		for each (var tag in children)
		{
			if (verbose)
				print("      tag: " + tag.localName());
			var tagName:String = String(tag.localName());
			var handledTags:String = "path relativePath href author langversion playerversion productversion toolversion taghref description result return example throws canThrow event eventType metadata";

			if (handledTags.indexOf(tagName) != -1)
			{
				//if (verbose)
				//	print(" skipping custom tag " + tagName);
			}
			else if (tagName == "see")
			{
				if (verbose)
					print("     sees[" + seesCount +"] = " + tag.text());
				
				sees.see += processSeeTag(String(node.@fullname), String(tag.text()));
				seesCount++;
			}
			else if (useParams && tagName == "param")
			{
				if (verbose)
					print("     param[" + paramCount + "] = " + String(tag));
				if (String(tag) != "none")
				{			
					var nextParam = paramNames.indexOf(";", lastParamName);
					if (nextParam == -1)
						nextParam = paramNames.length;
					var nextName = paramNames.substring(lastParamName, nextParam);
					lastParamName = nextParam+1;
					
					nextParam = paramTypes.indexOf(";", lastParamType);
					if (nextParam == -1)
						nextParam = paramTypes.length;
					var nextType = paramTypes.substring(lastParamType, nextParam);
//					if (nextType == "")
//						nextType = "Object";
					lastParamType = nextParam+1;
					
					nextParam = paramDefaults.indexOf(";", lastParamDefault);
					if (nextParam == -1)
						nextParam = paramDefaults.length;
					var nextDefault = paramDefaults.substring(lastParamDefault, nextParam);
					lastParamDefault = nextParam+1;

					if (nextName == "")
						continue;
						
					var nuParam = <param name={nextName} type={nextType}>							
								</param>;
								
					var paramClass = classTable[nextType];
					if (paramClass != null) {
 						nuParam.classRef = createClassRef2(fromClass, paramClass);
					}
					else if (nextType != "restParam") {
                        if (refErrors) {
						print("   Can not resolve param type name: " + nextType);
                        }
					}
								
					if (nextDefault != "undefined")
						nuParam.@default = nextDefault;
												
//					var xmlDesc = tag.toXMLString(); // preserves CDATA
//					var nuXmlDesc = xmlDesc.replace("<param>"," ");
//					nuXmlDesc = nuXmlDesc.replace("</param>", " ");
					var desc:String = tag.toString();
					var spaceIndex:int = desc.indexOf(" ");
					if (spaceIndex > -1)
						desc = desc.substr(spaceIndex + 1);
						
//					nuParam.description = <description>{desc}</description>;
					nuParam.description = new XML("<description><![CDATA[" + desc + "]]></description>"); // tag.toString();  // nuXmlDesc; // tag.toXMLString(); // try to avoid problem where CDATA is translated, leaving '"' chars in string
					// which makes: <description>{String(tag)}</description>   wig out.
				
					params.param += nuParam;
					paramCount++;
				}
			}
			else if (tagName == "includeExample")
			{
				if (verbose)
					print("     includeExamples[" + includeExamplesCount +"] = " + tag.text());

				includeExamples.includeExample += processIncludeExampleTag(String(node.@fullname), String(tag.text()));
				includeExamplesCount++;
			}
			else 
			{
				if (verbose)
					print("     custom tag " + tag.localName() +" = " + tag);
				customs[customCount] = tag;
				customs2[tag.localName()] += tag;
				customCount++;
			}
			if (verbose)
				print("      done with tag: " + tag.localName());
		}
	}
	
	if (useParams && lastParamName < paramNames.length)
	{
		if (verbose)
		{
			print("     more params declared than found @param tags for, inventing param elements");
			print("        params to synth docs for: " + paramNames.substring(lastParamName, paramNames.length) );
		}
		while (lastParamName < paramNames.length)
		{
			var nextParam = paramNames.indexOf(";", lastParamName);
			if (nextParam == -1)
				nextParam = paramNames.length;
			var nextName = paramNames.substring(lastParamName, nextParam);
			lastParamName = nextParam+1;
			
			nextParam = paramTypes.indexOf(";", lastParamType);
			if (nextParam == -1)
				nextParam = paramTypes.length;
			var nextType = paramTypes.substring(lastParamType, nextParam);
//			if (nextType == "")
//				nextType = "Object";
			lastParamType = nextParam+1;
			
			nextParam = paramDefaults.indexOf(";", lastParamDefault);
			if (nextParam == -1)
				nextParam = paramDefaults.length;
			var nextDefault = paramDefaults.substring(lastParamDefault, nextParam);
			lastParamDefault = nextParam+1;
			
			//print("param: " + nextName + " type: " + nextType );
			var nuParam = <param name={nextName} type={nextType}>							
						</param>;
			var paramClass = classTable[nextType];
			if (paramClass != null) {
 				nuParam.classRef = createClassRef2(fromClass, paramClass);
			}
			else if (nextType != "restParam") {
                if (refErrors) {
				print("   Can not resolve param type name: " + nextType);
                }
			}
			if (nextDefault != "undefined")
				nuParam.@default = nextDefault;
					
			params.param += nuParam;
			paramCount++;
		}
	}
	if (seesCount != 0)
		record.sees = sees;
	if (paramCount != 0)
		record.params = params;
	if (includeExamplesCount != 0)
		record.includeExamples = includeExamples;

	if (customCount != 0)
		record.customs = customs2;
		
	if (verbose) print("Done processing customs");
}

function hideNamespace(namespace:String):Boolean
{
	if (namespace == undefined || namespace == null || namespace == "")
		return false;
	else if (namespaces.indexOf(":" + namespace + ":") != -1)
		return (namespaces.indexOf(":" + namespace + ":true:") != -1);
	else if (namespace == 'public')
		return false;
	else if (namespace == 'private')
		return true;
	else if (namespace.indexOf("$internal") != -1)
		return !includeInternal;
	else if (namespace == 'internal')
		return !includeInternal;
	else
		return hideNamespaces;
}

function hidePackage(asPackage:String):Boolean
{
	if (asPackage == undefined || asPackage == null || asPackage == "")
		return false;
	else if (hiddenPackages.indexOf(":" + asPackage + ":") != -1)
		return (hiddenPackages.indexOf(":" + asPackage + ":true:") != -1);
	else
		return hidePackages;
}

function translateHtmlTags(str:String):String
{
	if (str.indexOf("<") != -1)
		str = str.replace("<","&lt");
	if (str.indexOf(">") != -1)
		str = str.replace(">","&gt");
	return str;
}


function debug_decomposeFullName(fname:String):QualifiedFullName
{
	// flash.core/EventDispatcher:public/EventDispatcher:public
	// flash.text/TextRenderer:public/maxLevel:public/get
	
	var endPackage:Number = fname.indexOf('/');
	var endClass = fname.indexOf(':');

	var nextIndex = 0;
	if (endPackage != -1 && endPackage < endClass)
	{
		print("   for Package: " + fname.substring(nextIndex,endPackage));
		nextIndex = endPackage+1;
	}

	var endClass = fname.indexOf(':',nextIndex);
	if (endClass != -1)
	{
		print("       Class: "  + fname.substring(nextIndex,endClass));
		nextIndex = endClass+1;
	}
	
	var endClassNameSpace = fname.indexOf('/',nextIndex);
	if (endClassNameSpace != -1)
	{
		print("       ClassNameSpace: " + fname.substring(nextIndex,endClassNameSpace));
		nextIndex = endClassNameSpace+1;
	}
	
	var endMethodName = fname.indexOf(':',nextIndex);
	if (endMethodName != -1)
	{
		print("       Method: " + fname.substring(nextIndex,endMethodName));
		nextIndex = endMethodName+1;
		print("       Method NameSpace: " + fname.substring(nextIndex,fname.length));
	}
}



function pad(s:String, len:Number)
{
	s = (s == undefined ? "" : s);
	while (s.length < len) {
		s += ' ';
	}
	return s;
}

function quote(s:String)
{
	var r = "";
	for (var i=0; i<s.length; i++) {
		var c = s.charAt(i);
		if (c == '"') {
			r += "\\";
		}
		r += c;
	}
	return r;
}

//  Author: info@sephiroth.it
// ----------------------- 
// Replace single or 
// multiple chars in a 
// String. 
// The original string is 
// not affected. 
// 
// Based on a idea of 
// Davide Beltrame (Broly) 
// mail: davb86@libero.it 
// ----------------------- 
String.prototype.replace = function() 
{
	var endText:String;
    var preText:String
    var newText:String;
print("1");
	var arg_search:String = String(arguments[0]);
	var arg_replace:String = String(arguments[1]);
print("2");
	if(arg_search.length==1) 
		return this.split(arg_search).join(arg_replace);
	var position:Number = this.indexOf(arg_search);
print("3");	

	if(position == -1) 
		return this; 
	endText = this; 
	newText = ""; // cn: added this TODO: warning tool should warn that this doesn't work the same anymore!!!
	do 
	{ 
		position = endText.indexOf(arg_search); 
		preText = endText.substring(0, position) 
		endText = endText.substring(position + arg_search.length) 
		newText += preText + arg_replace; 
	} while(endText.indexOf(arg_search) != -1) 
	newText += endText; 
	return newText; 
}