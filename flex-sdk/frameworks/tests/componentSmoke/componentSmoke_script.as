import flash.display.*;
import flash.utils.*;
import mx.core.*;

use namespace mx_internal;

mx_internal var tests:XMLList;
mx_internal var numTests:int;
mx_internal var currentTest:int;
mx_internal var defaultTimeOut:int;
mx_internal var testTimeOut:int;
mx_internal var verifyTimeOut:int;
mx_internal var verifyLaterTimeOut:int;
mx_internal var foundErrors:Boolean = false;

public function objectFromString(pathName:String):Object
{
	if (pathName == "null") return null;
	
	var pathElements:Array = pathName.split(".");
	var o:Object = this;
	for (var i:int = 0; i < pathElements.length; i++)
	{
		if (pathElements[i] == "application")
		{
			o = this;
		}
		else if (o[pathElements[i]])
		{
			o = o[pathElements[i]];
		}
		else
			return undefined;
	}
	return o;
}


// triggerType:String: [event, frame, time, created, deleted]
// triggerEvent:Class;
// triggerEvent. String
// triggerEventTarget: Object;
// triggerFailureFrames: int
// target:Object;
// testEvent:Class;
// testProperties:Object;
// verify:Array of Verifications
// testName: String
// comment: String
// fatal: Boolean

public function startTests(testDoc:XML):void
{
	var timeout:XMLList = testDoc.attribute("timeout");
	if (timeout && timeout.length())
		defaultTimeOut = int(timeout[0]);
	else
		defaultTimeOut = 5;

	tests = testDoc.test;
	numTests = tests.length();
	currentTest = 0;
	runTests();
}

public function runTests():void
{
	while (currentTest < numTests)
	{
		if (!runTest())
			break;
		currentTest++;
	}
	if (currentTest >= numTests)
	{
		if (foundErrors)
		{
			trace("Test Completed. Found Errors");
			Alert.show("Test Completed. Found Errors");
		}
		else
		{
			trace("Test Completed.");
			Alert.show("Test Completed");
		}
	}
}

public function runTest():Boolean
{
	var test:XML = tests[currentTest];
	switch (test.attribute("type").toString())
	{
	case "onEvent":
		testOnEvent(test)
		return false;
		break;
	case "onNextFrame":
		var timeout:XMLList = test.verification.attribute("timeout");
		if (timeout && timeout.length())
		{
			verifyLaterTimeOut = int(timeout[0].toString());
		}
		else
			verifyLaterTimeOut = 0;
		callLater(testLater);
		return false;
		break;
	case "onCreate":
		if (testWhenCreated(test))
			return executeTest(test);
		break;
/*	case "onDelete":
		if (testWhenDeleted(test))
			return executeTest(test);
		break; */
	case "now":
		return executeTest(test);
	default:
		testError("unknown testTrigger");
	}
	return false;
}

public function testOnEvent(test:XML):void
{
	var o:Object = objectFromString(test.attribute("target").toString());
	o.addEventListener(test.attribute("eventType").toString(), triggerEvent, false, 10);
	var timeout:XMLList = test.attribute("timeout");
	if (timeout && timeout.length())
		testTimeOut = int(timeout[0]);
	else
		testTimeOut = defaultTimeOut;
	callLater(triggerFailureCheck);
}

public function testWhenCreated(test:XML):Boolean
{
	var o:Object = objectFromString(test.attribute("target").toString());
	if (o[test.property])
		return true;
	var timeout:XMLList = test.attribute("timeout");
	if (timeout && timeout.length())
		testTimeOut = int(timeout[0]);
	else
		testTimeOut = defaultTimeOut;
	callLater(reTestWhenCreated, [test]);
	return false;
}

public function reTestWhenCreated(test:XML):void
{
	var o:Object = objectFromString(test.attribute("target").toString());
	if (o[test.attribute("property")])
		executeTest(test);

	if (testTimeOut > 0)
	{
		callLater(reTestWhenCreated, [test]);
		testTimeOut--;
	}
	else if (testTimeOut == 0)
	{
		testError("timeout waiting for start of test");
		testStopped();
	}
}

public function triggerFailureCheck():void
{
	if (testTimeOut > 0)
	{
		callLater(triggerFailureCheck);
		testTimeOut--;
	}
	else if (testTimeOut == 0)
	{
		testError("timeout waiting for start of test");
		testStopped();
	}
}

public function triggerEvent(event:Event):void
{
	var test:XML = tests[currentTest]
	if (!(event is Class(getDefinitionByName(test.attribute("eventClass").toString()))))
	{
		testError("incorrect event class");
		testStopped();
		return;
	}

	if (event.type != test.attribute("eventType").toString())
	{
		testError("incorrect event type");
		testStopped();
		return;
	}
	event.target.removeEventListener(test.attribute("eventType").toString(), triggerEvent);

	if (executeTest(test))
	{
		currentTest++;
		runTests();
	}
}

public function testLater():void
{
	if (verifyLaterTimeOut == 0)
	{
		if (executeTest(tests[currentTest]))
		{
			currentTest++;
			runTests();
		}
	}
	else
	{
		verifyLaterTimeOut--;
		callLater(testLater);
	}
}

public function executeTest(test:XML):Boolean
{
	testTimeOut = -1;
	var eventList:XMLList = test.event;
	var len:int = eventList.length();
	var verifyNow:Boolean = prepareVerification();
	for (var i:int = 0; i < len; i++)
	{
		var testEvent:XML = eventList[i];
		var eventClassName:String = testEvent.attribute("eventClass").toString();
		var eventTypeName:String = testEvent.attribute("eventType").toString();
		var eventBubbles:String = testEvent.attribute("eventBubbles").toString();
		var eventClass:Class = Class(getDefinitionByName(eventClassName)); 
		var event:Event = new eventClass(eventTypeName, (eventBubbles == "true"));
		var o:Object = objectFromString(testEvent.attribute("target").toString());
		if (eventClassName == "flash.events.TextEvent" && eventTypeName == "textInput")
		{
			var textPropList:XMLList = testEvent.property;
			var textProperty:XML = textPropList[0]; 
			o.text = textProperty.attribute("value").toString();
		}
		// workaround until bug 123736 fixed
		else if (eventClassName == "flash.events.FocusEvent" && eventTypeName == "tabForward")
		{
			event = new FocusEvent("keyFocusChange", true, true, null, false, 9);
		}
		else if (eventClassName == "flash.events.FocusEvent" && eventTypeName == "tabBackward")
		{
			event = new FocusEvent("keyFocusChange", true, true, null, true, 9);
		}
		else
		{
			var propList:XMLList = testEvent.property;
			var propLen:Number = propList.length();
			for (var j:int = 0; j < propLen; j++)
			{
				var eventProperty:XML = propList[j]; 
				setEventProperty(event, eventProperty.attribute("propertyName").toString(), eventProperty.attribute("value").toString());
			}
		}
		o.dispatchEvent(event);
	}
	if (verifyNow)
	{
		if (test.verification.attribute("type").toString() == "afterUpdateNow")
		{
			o = objectFromString(test.verification.attribute("target").toString());
			o.updateNow();
		}
		verifyTest(test);
		return true;
	}
	return false;
}

public function prepareVerification():Boolean
{
	var test:XML = tests[currentTest];

	switch (test.verification.attribute("type").toString())
	{
	case "onEvent":
		verifyOnEvent(test)
		break;
	case "onNextFrame":
		var timeout:XMLList = test.verification.attribute("timeout");
		if (timeout && timeout.length())
		{
			verifyLaterTimeOut = int(timeout[0].toString());
		}
		else
			verifyLaterTimeOut = 0;
		callLater(verifyLater);
		break;
	case "afterUpdateNow":
	case "now":
		return true;
	default:
		testError("unknown verifyTrigger");
		testStopped();
	}
	return false;
}

public function verifyOnEvent(test:XML):void
{
	var o:Object = objectFromString(test.verification.attribute("target").toString());
	o.addEventListener(test.verification.attribute("eventType").toString(), verifyEvent, false, 10);
	var timeout:XMLList = test.verification.attribute("timeout");
	if (timeout && timeout.length())
		verifyTimeOut = int(timeout[0]);
	else
		verifyTimeOut = defaultTimeOut;
	callLater(verifyFailureCheck);
}

public function verifyFailureCheck():void
{
	if (verifyTimeOut > 0)
	{
		callLater(verifyFailureCheck);
		verifyTimeOut--;
	}
	else if (verifyTimeOut == 0)
	{
		testError("timeout waiting for verification event");
		testStopped();
	}
}

public function verifyEvent(event:Event):void
{
	var test:XML = tests[currentTest]
	if (!(event is Class(getDefinitionByName(test.verification.attribute("eventClass").toString()))))
	{
		testError("incorrect event class");
		return;
	}

	if (event.type != test.verification.attribute("eventType"))
	{
		testError("incorrect event type");
		return;
	}
	event.target.removeEventListener(test.verification.attribute("eventType").toString(), verifyEvent);
	verifyTimeOut = -1;

	verifyTest(test);
	currentTest++;
	runTests();
}

public function verifyLater():void
{
	if (verifyLaterTimeOut == 0)
	{
		if (verifyTest(tests[currentTest]))
		{
			currentTest++;
			runTests();
		}
	}
	else
	{
		verifyLaterTimeOut--;
		callLater(verifyLater);
	}
}

public function verifyTest(test:XML):Boolean
{
	var passed:Boolean = true;
	var tests:XMLList = test.verification.verify;
	var value:String;
	var len:int = tests.length();
	for (var i:int = 0; i < len; i++)
	{
		var verify:XML = tests[i];
		var o:Object = objectFromString(verify.attribute("target").toString());
		if (!testProperty(o, verify.attribute("propertyName").toString(), verify.attribute("value").toString()))
		{
			value = getProperty(o, verify.attribute("propertyName").toString());
			passed = false;
			testError("property " + verify.attribute("propertyName").toString() + " is " + value + " not " + verify.attribute("value").toString());
			if (verify.attribute("fatal").length() > 0)
			{
				testStopped();
				return false;
			}
		}

	}
	tests = test.verification.pixel;
	len = tests.length();
	if (len)
	{
		getBitmapOfScreen();
		for (i = 0; i < len; i++)
		{
			var pixelTest:XML = tests[i];
			o = objectFromString(pixelTest.attribute("target").toString());
			if (!testPixel(o, uint(pixelTest.attribute("x").toString()), uint(pixelTest.attribute("y").toString()), uint(pixelTest.attribute("value").toString())))
			{
				value = (getPixel(o, uint(pixelTest.attribute("x").toString()), uint(pixelTest.attribute("y").toString()))).toString();
				passed = false;
				testError("pixel at " + pixelTest.attribute("x").toString() + ", " + pixelTest.attribute("y").toString() + " is " + value + " not " + pixelTest.attribute("value").toString());
			}

		}
	}
	if (passed)
		trace("Passed: " + test.attribute("title").toString());
	return true;

}

public function testError(s:String):void
{
	var t:String = tests[currentTest].attribute("title").toString();
	trace("FAILURE: " + t + " - " + s);
	foundErrors = true;
}

public function setEventProperty(event:Event, propName:String, propValue:String):void
{
	if (event[propName] is String)
	{
		event[propName] = propValue;
	}
	else if (event[propName] is Number)
	{
		event[propName] = Number(propValue);
	}
	else if (event[propName] is int)
	{
		event[propName] = int(propValue);
	}
	else if (event[propName] is uint)
	{
		event[propName] = uint(propValue);
	}
	else if (event[propName] is Boolean)
	{
		if (propValue == "true")
			event[propName] = true;
		else
			event[propName] = false;
	}
	else if (event[propName] is Object ||
			 event[propName] === null)
	{
		var o:Object = objectFromString(propValue);
		//trace("set object: " + o);
		event[propName] = o;
	}
	else
	{
		testError("unknown type for property assignment " + propName + " = " + propValue);
	}
}

public function getProperty(o:Object, propName:String):String
{
	if (o[propName] is String)
	{
		return o[propName];
	}
	else if (o[propName] is Number)
	{
		return o[propName].toString();
	}
	else if (o[propName] is int)
	{
		return o[propName].toString();
	}
	else if (o[propName] is uint)
	{
		return o[propName].toString();
	}
	else if (o[propName] is Boolean)
	{
		return o[propName].toString();
	}
	else if (o[propName] is Date)
	{
		var testDate:Date = o[propName];
		return testDate.getDate().toString();
	}
	else if (propName == "focus")
	{
		return stage.focus.toString();
	}
	else
	{
		testError("unknown type for property retrieval " + propName);
	}
	return null;
}

public function testProperty(o:Object, propName:String, propValue:String):Boolean
{
	var value:String = getProperty(o, propName);
	if (o[propName] is Date)
	{
		var indices:Array = propValue.split(",");
		var today:Date = new Date();
		var month:int = today.getMonth();
		month += int(indices[0]);
		month = month % 12;
		today.setMonth(month);
		today.setDate(1);
		var column:int = today.getDay();
		var date:int = 1 + int(indices[1]) - column;
		date += 7 * (int(indices[2]) - 1);
		propValue = date.toString();
	}
	else if (propName == "focus")
	{
		propValue = objectFromString(propValue).toString();
	}
	return value == propValue;
}

public function testStopped():void
{
	Alert.show("Test Stopped. Fatal Error");
}

public var screenBits:BitmapData;

public function getBitmapOfScreen():void
{
	screenBits = new BitmapData(stage.stageWidth, stage.stageHeight);
	screenBits.draw(IBitmapDrawable(systemManager), new Matrix());
}

public function getPixel(o:Object, x:uint, y:uint):uint
{
	var pt:Point = new Point(x,y);
	pt = o.localToGlobal(pt);
	var pixel:uint = screenBits.getPixel(pt.x, pt.y);
	trace("pixel is " + pixel);
	return pixel;
}

public function testPixel(o:Object, x:uint, y:uint, color:uint):Boolean
{
	var pixel:uint = getPixel(o, x, y);
	return pixel == color;
}
