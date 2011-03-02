////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger;

/**
 * @author mmorearty
 */
public interface Value
{
	/**
	 * The value returned if somone calls getId() for a Variable
	 * which stores a variable of simple type such as String or
	 * integer, rather than an Object or MovieClip.
	 * @see getId()
	 */
	public static final int UNKNOWN_ID							= -1;

	/**
	 * The special ID for pseudo-variable "_global".  (Note, this only
	 * exists in AS2, not AS3.)
	 * @see getId()
	 */
	public static final int GLOBAL_ID							= -2;

	/**
	 * The special ID for pseudo-variable "this".
	 * @see getId()
	 */
	public static final int THIS_ID								= -3;

	/**
	 * The special ID for pseudo-variable "_root".  (Note, this only
	 * exists in AS2, not AS3.)
	 * @see getId()
	 */
	public static final int ROOT_ID								= -4;

	/**
	 * The special ID for the top frame of the stack.  Locals and
	 * arguments are "members" of this pseudo-variable.
	 * 
	 * All the stack frames have IDs counting down from here.  For example,
	 * the top stack frame has ID <code>BASE_ID</code>; the next
	 * stack frame has ID <code>BASE_ID - 1</code>; and so on.
	 * 
	 * @see getId()
	 */
	public static final int BASE_ID								= -100;

	/**
	 * _level0 == LEVEL_ID, _level1 == LEVEL_ID-1, ...
	 * 
	 * all IDs below this line are dynamic.
	 */
	public static final int LEVEL_ID							= -300;

	/**
	 * The return value of getTypeName() if this value represents the traits of a class.
	 */
	public static final String TRAITS_TYPE_NAME					= "traits"; //$NON-NLS-1$

	/**
	 * Variable type can be one of VariableType.OBJECT,
	 * VariableType.FUNCTION, VariableType.NUMBER, VariableType.STRING,
	 * VariableType.UNDEFINED, VariableType.NULL.
	 */
	public int			getType();

	public String		getTypeName();

	public String		getClassName();

	/**
	 * Variable attributes define further information 
	 * regarding the variable.  They are bitfields identified
	 * as VariableAttribute.xxx
	 * 
	 * @see VariableAttribute
	 */
	public int			getAttributes();

	/**
	 * @see VariableAttribute
	 */
	public boolean		isAttributeSet(int variableAttribute);

	/**
	 * Returns a unique ID for the object referred to by this variable.
	 * If two variables point to the same underlying object, their
	 * getId() functions will return the same value.
	 * 
	 * This is only meaningful for variables that store an Object or
	 * MovieClip.  For other types of variables (e.g. integers and
	 * strings), this returns <code>UNKNOWN_ID</code>.
	 */
	public int			getId();

	/**
	 * Returns the value of the variable, as an Object.  For example,
	 * if the variable is an integer, the returned object will be an
	 * <code>Integer</code>.
	 */
	public Object		getValueAsObject();

	/**
	 * Returns the value of the variable, converted to a string.
	 */
	public String		getValueAsString();

	/**
	 * Returns all child members of this variable.  Can only be called for
	 * variables of type Object or MovieClip.
	 * @throws NotConnectedException 
	 * @throws NoResponseException 
	 * @throws NotSuspendedException 
	 */
	public Variable[]	getMembers(Session s) throws NotSuspendedException, NoResponseException, NotConnectedException;

	/**
	 * Returns a specific child member of this variable.  Can only be called for
	 * variables of type <code>Object<code> or <code>MovieClip<code>.
	 * @param s the session
	 * @param name just a varname name, without its namespace (see <code>getName()</code>)
	 * @return the specified child member, or null if there is no such child.
	 * @throws NotConnectedException 
	 * @throws NoResponseException 
	 * @throws NotSuspendedException 
	 */
	public Variable     getMemberNamed(Session s, String name) throws NotSuspendedException, NoResponseException, NotConnectedException;

	/**
	 * Returns the number of child members of this variable.  If called for
	 * a variable which has a simple type such as integer or string,
	 * returns zero.
	 * @throws NotConnectedException 
	 * @throws NoResponseException 
	 * @throws NotSuspendedException 
	 */
	public int			getMemberCount(Session s) throws NotSuspendedException, NoResponseException, NotConnectedException;

	/**
	 * Returns the list of classes that contributed members to this object, from
	 * the class itself all the way down to <code>Object</code> (or, if
	 * allLevels == false, down to the lowest-level class that actually
	 * contributed members).
	 * 
	 * @param allLevels
	 *            if <code>true</code>, the caller wants the entire class
	 *            hierarchy. If <code>false</code>, the caller wants only
	 *            that portion of the class hierarchy that actually contributed
	 *            member variables to the object. For example,
	 *            <code>Object</code> has no members, so if the caller passes
	 *            <code>true</code> then the returned array of strings will
	 *            always end with <code>Object</code>, but if the caller
	 *            passes <code>false</code> then the returned array of strings
	 *            will <em>never</em> end with <code>Object</code>.
	 * @return an array of fully qualified class names.
	 */
	public String[]		getClassHierarchy(boolean allLevels);
}
