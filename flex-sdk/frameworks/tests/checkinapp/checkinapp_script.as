import mx.effects.*;

[Bindable]
public var selectedItem:Object;

[Bindable]
public var currentView:String = "thumb";

[Bindable]
[Embed(source="assets/images/thumb_on.png")]
public var thumbOn:Class; //AS3 must be public to be bindable

[Bindable]
[Embed(source="assets/images/thumb_off.png")]
public var thumbOff:Class; //AS3 must be public to be bindable

[Bindable]
[Embed(source="assets/images/list_on.png")]
public var listOn:Class; //AS3 must be public to be bindable

[Bindable]
[Embed(source="assets/images/list_off.png")]
public var listOff:Class; //AS3 must be public to be bindable

[Embed(source="assets/images/thumb_roll.png")]
private var thumbRoll:Class;

[Embed(source="assets/images/list_roll.png")]
private var listRoll:Class;

// The web service request for the product catalog is sent when the application is initialized
private function initApp():void {
	selectedItem = catalog.product[0];
}

private function changeView(view:String):void {
    currentView=view;
    if (view=="thumb") {
        bodyStack.selectedChild=thumbView;
        controlStack.selectedChild=thumbCtrl;
    } else if (view=="grid") {
        bodyStack.selectedChild=gridView;
        controlStack.selectedChild=gridCtrl;
    } else if (view=="checkout") {
        bodyStack.selectedChild=checkoutView;
        controlStack.selectedChild=checkoutCtrl;
    }
}

private function slide(event:ResizePanelEvent):void {
	var d:Dissolve;
    var e:Resize = new Resize(topCanvas);
    if (event.size=="medium") {
        e.heightTo=325;
        productDetail.ctrl.visible=true;
        cartView.ctrl.visible=true;
        statusTop.selected="medium";
        statusBottom.selected="medium";
    } else if (event.size=="small") {
        if (event.target==statusTop) {
            e.heightTo=30;
            productDetail.ctrl.visible=false;
            cartView.ctrl.visible=true;
            statusTop.selected="small";
            statusBottom.selected="large";
        } else {
            e.heightTo = hb.height - 36;
            productDetail.ctrl.visible=true;
            cartView.ctrl.visible=false;
            statusTop.selected="large";
            statusBottom.selected="small";
        }
    } else {
        if (event.target==statusTop) {
            e.heightTo = hb.height - 36;
            productDetail.ctrl.visible=true;
            cartView.ctrl.visible=false;
            statusTop.selected="large";
            statusBottom.selected="small";
        } else {
            e.heightTo=30;
            productDetail.ctrl.visible=false;
            cartView.ctrl.visible=true;
            statusTop.selected="small";
            statusBottom.selected="large";
        }
    }
    e.hideChildrenTargets = [productDetail, cartView];
    e.duration=300;
    e.play();
}
