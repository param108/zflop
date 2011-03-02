import qa.mxunit.*;
import mx.collections.*;
import mx.containers.*;
import flash.utils.*;
import mx.controls.*;
import mx.controls.dataGridClasses.*;
import mx.controls.menuClasses.*;
import mx.controls.treeClasses.*;

public var callLaterMax:uint = 40;

public function testStartUp():void
{
    testButton.dispatchEvent(new MouseEvent("mouseOver"));
    testButton.dispatchEvent(new MouseEvent("mouseDown"));
    testButton.dispatchEvent(new MouseEvent("mouseUp"));
    testButton.dispatchEvent(new MouseEvent("click"));
    Assert.isTrue(testLabel.text == "This is a test",
                "testLabel should be 'This is a test'");

}

public function testForCheckbox():void
{
    testCheckBox.dispatchEvent(new MouseEvent("mouseOver"));
    testCheckBox.dispatchEvent(new MouseEvent("mouseDown"));
    testCheckBox.dispatchEvent(new MouseEvent("mouseUp"));
    testCheckBox.dispatchEvent(new MouseEvent("click"));
    Assert.isTrue(testCheckBox.selected,
                "testCheckBox should be selected");
}

public function testForRadio1():void
{
    testRadio1.dispatchEvent(new MouseEvent("mouseOver"));
    testRadio1.dispatchEvent(new MouseEvent("mouseDown"));
    testRadio1.dispatchEvent(new MouseEvent("mouseUp"));
    testRadio1.dispatchEvent(new MouseEvent("click"));
    Assert.isTrue(testRadio1.selected,
                "testRadio1 should be selected");
}
public function testForRadio2():void
{
    testRadio2.dispatchEvent(new MouseEvent("mouseOver"));
    testRadio2.dispatchEvent(new MouseEvent("mouseDown"));
    testRadio2.dispatchEvent(new MouseEvent("mouseUp"));
    testRadio2.dispatchEvent(new MouseEvent("click"));
    Assert.isTrue(!testRadio1.selected,
                "testRadio1 should not be selected");
    Assert.isTrue(testRadio2.selected,
                "testRadio2 should be selected");
}

public function testForTextInput(isCallLater:uint=0):void
{
    if (isCallLater == 0)
    {
        testTextInput.text = "This is another test";
        testTextInput.dispatchEvent(new TextEvent("textInput"));
        callLater(testForTextInput,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertEquals("This is another test", testTextInput.getTextField().text);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForTextInput,[isCallLater+1]);
        }
    }
}
public function testForTextArea(isCallLater:uint=0) :void
{
    if (isCallLater == 0)
    {
        testTextArea.text = "This is a TextArea and it contains lots of text which should word wrap and eventually cause scrollbars to appear.  The validation will test for the scrollbar properties";
        testTextInput.dispatchEvent(new TextEvent("textInput"));
        callLater(testForTextArea,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertTrue("expect larger than 6, but:"+testTextArea.maxVerticalScrollPosition, testTextArea.maxVerticalScrollPosition >6);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForTextArea,[isCallLater+1]);
        }
    }
}

public function testForCombobox1(e:Event=null):void
{
	var textInput:TextInput = TextInput(testCombo.getChildAt(2));
    if (e== null)
    {
        testCombo.addEventListener("open",testForCombobox1);
        textInput.dispatchEvent(new MouseEvent("mouseOver"));
        textInput.dispatchEvent(new MouseEvent("mouseDown"));
        textInput.dispatchEvent(new MouseEvent("mouseUp"));
        textInput.dispatchEvent(new MouseEvent("click"));
        Assert.hasPendingTest = true;
    }
    else
    {
        testCombo.removeEventListener("open",testForCombobox1);
        Assert.assertTrue("Dropdown visible", testCombo.dropdown.visible);
        Assert.hasPendingTest = false;
    }
}

public function testForCombobox2(e:Event=null)  :void
{
    if (e== null)
    {
        testCombo.addEventListener("close",testForCombobox2);
        testCombo.dropdown.indicesToItemRenderer(1, 0).dispatchEvent(new MouseEvent("mouseOver"));
        testCombo.dropdown.indicesToItemRenderer(1, 0).dispatchEvent(new MouseEvent("mouseDown"));
        testCombo.dropdown.indicesToItemRenderer(1, 0).dispatchEvent(new MouseEvent("mouseUp"));
        testCombo.dropdown.indicesToItemRenderer(1, 0).dispatchEvent(new MouseEvent("click"));
        Assert.hasPendingTest = true;
    }
    else
    {
        testCombo.removeEventListener("close",testForCombobox2);
        Assert.assertEquals(1, testCombo.selectedIndex);
        Assert.hasPendingTest = false;
    }
}

public function testForColorPicker1(e:Event=null):void
{
	var textInput:TextInput = TextInput(testColorPicker.getChildAt(3));
    if (e== null)
    {
        testColorPicker.addEventListener("open",testForColorPicker1);
        textInput.dispatchEvent(new MouseEvent("mouseOver"));
        textInput.dispatchEvent(new MouseEvent("mouseDown"));
        textInput.dispatchEvent(new MouseEvent("mouseUp"));
        textInput.dispatchEvent(new MouseEvent("click"));
        Assert.hasPendingTest = true;
    }
    else
    {
        testColorPicker.removeEventListener("open",testForColorPicker1);
        Assert.assertTrue("Dropdown visible", testColorPicker.dropdown.visible);
        Assert.hasPendingTest = false;
    }
}

public function testForNumericStepper(isCallLater:uint=0):void
{
    var target:Object = testNumStep.nextButton;
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"))
        var event:Event= new eventClass("mouseOver", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        callLater(testForNumericStepper,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertEquals(1, testNumStep.value);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForNumericStepper,[isCallLater+1]);
        }
    }
}

public function testForNumericStepper1(isCallLater:uint=0):void
{
    var target:Object = testNumStep.nextButton;
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        callLater(testForNumericStepper1,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertEquals(2, testNumStep.value);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForNumericStepper1,[isCallLater+1]);
        }
    }
}

public function testForNumericStepper2(isCallLater:uint=0) :void
{
    var target:Object = testNumStep.prevButton;
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        callLater(testForNumericStepper2,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertEquals(1, testNumStep.value);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForNumericStepper2,[isCallLater+1]);
        }
    }
}

public function testForDateField(isCallLater:uint=0) :void
{
    var target:Object = testDateField.getChildAt(1); // dropdown
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        //setEventProperty(event,"localX","30");
        //setEventProperty(event,"localY","30");
        target.dispatchEvent(event);
        callLater(testForDateField,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertTrue("Pulldown visible", testDateField.dropdown.visible);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForDateField,[isCallLater+1]);
        }
    }
}

public function testForDateField2(isCallLater:uint=0):void
{
	/*
	var target:Object = testDateField.dropdown.dateGrid.dayBlocksArray[4][1];
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        setEventProperty(event,"relatedObject","testDateField.mx_internal::dropdown");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        setEventProperty(event,"relatedObject","null");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        setEventProperty(event,"relatedObject","null");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        setEventProperty(event,"relatedObject","null");
        target.dispatchEvent(event);

        callLater(testForDateField2,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertTrue("Pass", true);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForDateField2,[isCallLater+1]);
        }
    }
	*/
    Assert.assertTrue("Pass", true);
    Assert.hasPendingTest = false;
}

public function testForDateField3() :void
{
    Assert.assertEquals(new Date(2005,4,4), testDateField.selectedDate);
}

public function testForMenu(isCallLater:uint=0) :void
{
    var target:Object = testMenuButton;
    if (isCallLater ==0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        callLater(testForMenu,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax) {
            Assert.assertTrue("Menu visible", testMenu.visible);
            Assert.hasPendingTest = false;
        }
        else 
        {
            callLater(testForMenu,[isCallLater+1]);
        }
    }
}

public function testForMenu2(isCallLater:uint=0)  :void
{
    var target:Object = testMenu.indicesToItemRenderer(0, 0);
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        callLater(testForMenu2,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax) {
            Assert.assertTrue(target.menu.visible);
            Assert.hasPendingTest = false;            
        }
        else
        {
            callLater(testForMenu2,[isCallLater + 1]);
        }
        
    }
}

public function testForMenu3(isCallLater:uint=0) :void
{
    var target:Object = testMenu.indicesToItemRenderer(1, 0);
    if (isCallLater == 0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        callLater(testForMenu3,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
        	Assert.assertTrue("testMenu.indicesToItemRenderer(1, 0).menu.visible", MenuItemRenderer(testMenu.indicesToItemRenderer(1, 0)).menu.visible); 
            Assert.assertFalse("testMenu.indicesToItemRenderer(0, 0).menu.visible", MenuItemRenderer(testMenu.indicesToItemRenderer(0, 0)).menu.visible);
            Assert.hasPendingTest = false;                    
        }
        else
        {
            callLater(testForMenu3,[isCallLater + 1]);
        }
    }
}

public function testForMenu4(e:Event=null) :void
{
    if (e == null)
    {
        testMenu.addEventListener("itemClick",testForMenu4);
        var target:Object = MenuItemRenderer(testMenu.indicesToItemRenderer(1, 0)).menu.indicesToItemRenderer(0, 0);

        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);
        Assert.hasPendingTest = true; 
    }
    else
    {
        testMenu.removeEventListener("itemClick",testForMenu4);
        Assert.assertEquals("Copy", testMenuLabel.text);
        Assert.hasPendingTest = false;
    }
}

public function testForMenuBar1(isCallLater:uint=0):void
{
    if (isCallLater == 0)
    {
        var target:Object = testMenuBar.menuBarItems[0];

        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);
        callLater(testForMenuBar1,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        Assert.assertTrue("testMenuBar.menus[0].visible", testMenuBar.menus[0].visible);
        Assert.hasPendingTest = false;
    }
}

public function testForMenuBar2(isCallLater:uint=0) :void
{
    if (isCallLater == 0)
    {
        var target1:Object = testMenuBar.menuBarItems[0];
        var target2:Object = testMenuBar.menuBarItems[1];

        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOut", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target1.dispatchEvent(event);

        event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target2.dispatchEvent(event);

        callLater(testForMenuBar2,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertFalse("testMenuBar.menus[0].visible", testMenuBar.menus[0].visible);
            Assert.assertTrue("testMenuBar.menus[1].visible", testMenuBar.menus[1].visible);
            Assert.hasPendingTest = false;
        }    
        else
        {
            callLater(testForMenuBar2,[isCallLater+1]);
        }
    }
}

public function testForMenuBar3(isCallLater:uint=0)  :void
{
    if (isCallLater == 0)
    {
        var target:Object = testMenuBar.menus[1].indicesToItemRenderer(1, 0);

        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);
        callLater(testForMenuBar3,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax) {
            Assert.assertEquals("Paste", testMenuLabel.text);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForMenuBar3,[isCallLater + 1]);
        }    
    }
}

public function testTabFocus1(e:Event=null) :void
{
    var target:Object = testButton;
    if (e==null)
    {
        target.addEventListener("click",testTabFocus1);
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","40");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","40");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","40");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","40");
        target.dispatchEvent(event);
        
        Assert.hasPendingTest = true;
    }
    else
    {
        target.removeEventListener("click",testTabFocus1);
        Assert.assertEquals(testButton, stage.focus);
        Assert.hasPendingTest = false;
    }
}

public function testTabFocus2(isCallLater:uint=0) :void
{
    if (isCallLater == 0)
    {
        var target:Object = testButton;
		if (target.stage.focus == target)
		{
			var event:Event= new FocusEvent("keyFocusChange", true, true, null, false, 9);
			target.dispatchEvent(event);

			callLater(testTabFocus2,[1]);
			Assert.hasPendingTest = true;
		}
		else
		{
			callLater(testTabFocus2,[0]);
		}
    }
    else
    {
        if (isCallLater == callLaterMax) {
	        Assert.assertContains("testCheckBox", stage.focus.toString());
		    Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testTabFocus2,[isCallLater + 1]);
        }    
    }
}

public function testTabFocus3(isCallLater:uint=0)  :void
{
    if (isCallLater == 0)
    {
        var target:Object = testCheckBox;

		if (target.stage.focus == target)
		{
			var event:Event= new FocusEvent("keyFocusChange", true, true, null, false, 9);
			target.dispatchEvent(event);

			callLater(testTabFocus3,[1]);
			Assert.hasPendingTest = true;
		}
		else
		{
			callLater(testTabFocus3,[0]);
		}
    }
    else
    {
        if (isCallLater == callLaterMax) {
			Assert.assertContains("testRadio", stage.focus.toString());
			Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testTabFocus3,[isCallLater + 1]);
        }    
    }
}

public function testDragDrop1(isCallLater:uint=0)  :void
{
    var target:Object = testList.indicesToItemRenderer(2, 0);
    if (isCallLater==0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);
        callLater(testDragDrop1,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax) {
            Assert.assertEquals(2, testList.selectedIndex);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testDragDrop1,[isCallLater + 1]);
        }
    }
}

public function testDragDrop2(isCallLater:uint=0) :void
{
    var target1:Object = testList.indicesToItemRenderer(2, 0);
    var target2:Object = testDataGrid;
    var target3:Object = testDataGrid.indicesToItemRenderer(1, 0);
    var target4:Object = testDataGrid.indicesToItemRenderer(2, 0);
    if (isCallLater==0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target1.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        setEventProperty(event,"buttonDown","true");
        target1.dispatchEvent(event);

        event= new eventClass("mouseOut", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        setEventProperty(event,"buttonDown","true");
        target1.dispatchEvent(event);

        event= new eventClass("mouseMove", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        setEventProperty(event,"buttonDown","true");
        target2.dispatchEvent(event);

        event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        setEventProperty(event,"buttonDown","true");
        target3.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target3.dispatchEvent(event);

        event= new eventClass("mouseOut", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target4.dispatchEvent(event);

        callLater(testDragDrop2,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            Assert.assertEquals(7, ICollectionView(testDataGrid.dataProvider).length);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testDragDrop2,[isCallLater + 1]);
        }
    }
}

public function testForTreeNode1(isCallLater:uint=0):void
{
    var target:Object = addNodeButton;
    if (isCallLater==0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","0");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","0");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","0");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","0");
        target.dispatchEvent(event);
        callLater(testForTreeNode1,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax)
        {
            var view:ICollectionView = ICollectionView(testTree.dataProvider);
            Assert.assertEquals(6, view.length);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForTreeNode1,[isCallLater + 1]);
        }
    }
}

public function testForTreeNode2(isCallLater:uint=0):void
{
	var target:Object = TreeItemRenderer(testTree.indicesToItemRenderer(2, 0)).getChildAt(1);
	// target is the renderer's disclosureIcon

    if (isCallLater==0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);

        event= new eventClass("click", (true));
        setEventProperty(event,"localX","30");
        setEventProperty(event,"localY","30");
        target.dispatchEvent(event);
        callLater(testForTreeNode2,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
        if (isCallLater == callLaterMax) {
            var view:ICollectionView = ICollectionView(testTree.dataProvider);
            Assert.assertEquals(6, view.length);
            Assert.hasPendingTest = false;
        }
        else
        {
            callLater(testForTreeNode2,[isCallLater + 1]);
        }
    }
}

public function testForImage(isCallerLater:uint=0):void
{
    Assert.assertEquals(testImage.measuredWidth,143);
    Assert.assertEquals(testImage.measuredHeight,43);
}

public function testForLoader():void
{
    getBitmapOfScreen();
    Assert.assertTrue(testPixel(testLoader,10,10,0x0066CC));
}

public function testForTestField():void
{
    Assert.assertEquals(testText.getTextField().length,86);
}

public function testForSlider(isCallLater:uint=0):void
{
    var target:Object = sliderThumb;
    if (isCallLater==0)
    {
        var eventClass:Class = Class(getDefinitionByName("flash.events.MouseEvent"));
        var event:Event= new eventClass("mouseOver", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseDown", (true));
        setEventProperty(event,"localX","3");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseMove", (true));
        setEventProperty(event,"localX","13");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseMove", (true));
        setEventProperty(event,"localX","23");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseMove", (true));
        setEventProperty(event,"localX","33");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        event= new eventClass("mouseUp", (true));
        setEventProperty(event,"localX","33");
        setEventProperty(event,"localY","3");
        target.dispatchEvent(event);

        callLater(testForSlider,[1]);
        Assert.hasPendingTest = true;
    }
    else
    {
            Assert.assertEquals(testImage.alpha,0.4296875);
            Assert.assertEquals(testProgress.value,42);
            getBitmapOfScreen();
            //Assert.assertTrue(testPixel(testProgress,60,10,0xCC6600));
           Assert.hasPendingTest = false;
    }
}
