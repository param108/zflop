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
 * allows the setting of children.
 */
public abstract class NonTerminalExp implements ValueExp
{
	ValueExp  m_right;
	ValueExp  m_left;

	/* sets the left hand side child to the given node */
	public void setLeftChild(ValueExp node) { m_left = node; }

	/* sets the right hand side child to the given node */
	public void setRightChild(ValueExp node) { m_right = node; }

	/* traverses the children asking each one if they are an instanceof something */
	public boolean containsInstanceOf(Class c)
	{
		boolean yes = false;
		if (c.isInstance(this))
			yes = true;
		else if (m_left != null && m_left.containsInstanceOf(c))
			yes = true;
		else if (m_right != null && m_right.containsInstanceOf(c))
			yes = true;
		return yes;
	}

	public boolean hasSideEffectsOtherThanGetters()
	{
		return containsInstanceOf(AssignmentExp.class);
	}
}
