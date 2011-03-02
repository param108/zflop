package
{

[Bindable]
public class ShoppingCart
{
    public var items:Array = [];
    
	public var total:Number = 0;
    
	public var shippingCost:Number = 0;

    public function ShoppingCart()
	{
		super();
    }

    public function addItem(item:Object, qty:int = 1,
							index:int = 0):void
	{        
        items.splice(index, 0, { id: item.id,
                                 name: item.name,
                                 description: item.description,
                                 image: item.image,
                                 price: item.price,
                                 qty: qty });
        
		total += parseFloat(item.price) * qty;
    }

    public function removeItemAt(index:Number):void
	{
       	total -= parseFloat(items[index].price) * items[index].qty;
       	
		items.splice(index, 1);
	   	
		if (getItemCount() == 0)
			shippingCost = 0;
    }

    public function getItemCount():int
	{
        return items.length;
    }

    public function getTotal():Number
	{
        return total;
    }
}

}
