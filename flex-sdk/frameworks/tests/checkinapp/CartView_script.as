import mx.core.IUIComponent;
import mx.events.DragEvent;
import mx.managers.DragManager;

[Bindable]
public var selectedItem:Object;

[Bindable]
public var _cartItems:ShoppingCart;

public function set cartItems(items:ShoppingCart):void
{
    _cartItems = items;
    dg.dataProvider = _cartItems.items;
}

public function removeItem():void
{
    if (dg.selectedIndex >= 0)
        _cartItems.removeItemAt(dg.selectedIndex);
    if (_cartItems.items.length == 0)
	selectedItem = null;
    dg.dataProvider = _cartItems.items;
}

public function dataGridChange(selectedItem:Object):void
{
    if (selectedItem)
	{
        this.selectedItem = selectedItem;
        dispatchEvent(new Event('itemSelected'));
    }
}

private function doDragEnter(event:DragEvent):void
{
	if (event.dragSource.hasFormat("item"))
	{
		DragManager.acceptDragDrop(IUIComponent(event.target));
		event.preventDefault();
	}
}

private function doDragOver(event:DragEvent):void
{
	if (event.dragSource.hasFormat("item"))
	{
		DataGrid(event.target).showDropFeedback(event);
		event.preventDefault();
	}
}

private function doDragDrop(event:DragEvent):void
{
    DataGrid(event.target).hideDropFeedback(event);
    var item:Object = event.dragSource.dataForFormat("item");   
	if (!item)
	{
        // Item was dragged from the thumbnail view
        var items:Array = event.dragSource.dataForFormat("items") as Array;
        item = items[0];
    }
    var dropLoc:int = DataGrid(event.target).calculateDropIndex();
    _cartItems.addItem(item, 1, dropLoc);
	dg.dataProvider = _cartItems.items;
	event.preventDefault();
}
