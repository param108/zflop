////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.expression;

/**
 * Abstract implementation of the ValueExp interface which 
 * does not allow the setting of children.
 */
public abstract class TerminalExp implements ValueExp
{
	/* sets the left hand side child to the given node */
	public void setLeftChild(ValueExp node) throws NoChildException
	{
		throw new NoChildException("left");
	}

	/* sets the right hand side child to the given node */
	public void setRightChild(ValueExp node) throws NoChildException
	{
		throw new NoChildException("right");
	}

	/* are we an instanceof something */
	public boolean containsInstanceOf(Class c)
	{
		return (c.isInstance(this));
	}

	public boolean hasSideEffectsOtherThanGetters()
	{
		return false;
	}
}
