package
{
import flash.events.*;

public class ResizePanelEvent extends Event {

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function ResizePanelEvent(size:String = "small")
	{
		super("resizeEvent", false, false);
		this.size = size;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	public var size:String;
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override public function clone():Event
	{
		return new ResizePanelEvent(size);
	}
}
}

