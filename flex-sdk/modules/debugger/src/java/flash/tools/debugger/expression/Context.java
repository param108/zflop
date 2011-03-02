////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2003-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.expression;

/**
 * An object which returns a value given a name and
 * appropriate context information.  
 */
public interface Context
{
	/**
	 * Look for an object of the given name.
	 *
	 * The returned Object can be of any type at all.  For example, it could be:
	 *
	 * <ul>
	 * <li> a <code>flash.tools.debugger.Variable</code> </li>
	 * <li> your own wrapper around <code>Variable</code> </li>
	 * <li> a <code>flash.tools.debugger.Value</code> </li>
	 * <li> any built-in Java primitive such as <code>Long</code>, <code>Integer</code>,
	 *      <code>Double</code>, <code>Boolean</code>, or <code>String</code> </li>
	 * <li> any other type you want which has a good <code>toString()</code>; see below </li>
	 * </ul>
	 *
	 * No matter what type you return, make sure that it is a type whose <code>toString()</code>
	 * function returns a string representing the underlying value, in a form which
	 * the expression evaluator can use to either (1) return to the caller, or
	 * (2) attempt to convert to a number if the user typed an expression
	 * such as <code>"3" + "4"</code>.
	 */
	public Object lookup(Object o) throws NoSuchVariableException, PlayerFaultException;

	/**
	 * Look for the members of an object.
	 * @param o A variable whose members we want to look up
	 * @return Some object which represents the members; could even be just a string.
	 * See lookup() for more information about the returned type.
	 * @see lookup()
	 */
	public Object lookupMembers(Object o) throws NoSuchVariableException;

	/**
	 * Create a new context object by combining the current one and o.
	 * For example, if the user typed "myVariable.myMember", then this function
	 * will get called with o equal to the object which represents "myVariable".
	 * This function should return a new context which, when called with
	 * lookup("myMember"), will return an object for that member.
	 *
	 * @param o any object which may have been returned by this class's lookup() function
	 */
	public Context createContext(Object o);

	/**
	 * Assign the object o, the value v.
	 * @return Boolean true if worked, false if failed.
	 */
	public Object assign(Object o, Object v) throws NoSuchVariableException, PlayerFaultException;

	/**
	 * Enables/disables the creation of variables during lookup calls.
	 * This is ONLY used by AssignmentExp for creating a assigning a value 
	 * to a property which currently does not exist.
	 */
	public void createPseudoVariables(boolean oui);
}
