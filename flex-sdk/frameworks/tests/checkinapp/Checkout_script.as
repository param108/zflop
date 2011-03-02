import mx.events.*;
import mx.controls.Alert;

[Bindable]
public var cartData:ShoppingCart;
[Bindable]
public var months:Array = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
[Bindable]
public var years:Array = [2004, 2005, 2006, 2007, 2008, 2009, 2010];

public function confirmOrder(): void {

	var validators:Array = [nameValidator, addressValidator, cityValidator,
									stateValidator, cardHolderValidator, cardExpMonthValidator,
									cardExpYearValidator, creditCardValidator, zipCodeValidator,
									emailValidator];
	
    if (cartData.getItemCount() == 0) {
        //Shopping Cart is empty
        mx.controls.Alert.show("You have no items in your shopping cart. Please select some items and proceed with your checkout.", "Problem");
    } else {
		var isValid:Boolean = true;
	
		for (var i:uint = 0; i < validators.length; i++)
		{
			if (ValidationResultEvent(validators[i].validate()).type == ValidationResultEvent.INVALID)
			{
				isValid = false;
			}
		}
	
        if (isValid) {
            // You could invoke a web service here to process the order
            mx.controls.Alert.show("Thank you for your order.", "Confirmation");
        }
        else {
            mx.controls.Alert.show("Please enter valid data in the fields with errors and try again.", "Problem");
        }
    }

}
