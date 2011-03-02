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
 * All objects in the abstract syntax tree must provide 
 * this interface.  It allows the tree to resolve down
 * to a single value.
 * 
 * The tree nodes are terminal and non-terminal.  Terminals
 * are constants or variables, non-terminals are everything 
 * else.  Each non-terminal is an operation which takes 
 * its left hand child and right hand child as input
 * and produces a result.  Performing evaluate() at the root of 
 * the tree results in a single Object being returned.
 */
public interface ValueExp
{
	/**
	 * perform your evaluation
	 */
	public Object evaluate(Context context) throws NumberFormatException, NoSuchVariableException, PlayerFaultException;

	/**
	 * sets the left hand side child to the given node
	 */
	public void setLeftChild(ValueExp node) throws NoChildException;

	/**
	 * sets the right hand side child to the given node
	 */
	public void setRightChild(ValueExp node) throws NoChildException;

	/**
	 * sees if any nodes within this expression are equal to the type of o
	 */ 
	public boolean containsInstanceOf(Class c);

	/**
	 * Returns whether the expression would have any side effects other than
	 * executing getters -- e.g. assignment, ++, or function calls.
	 */
	public boolean hasSideEffectsOtherThanGetters();
}
