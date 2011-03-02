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

import java.util.HashMap;

import flash.util.Trace;

/**
 * Provides access to Operator related actions. 
 */
public class Operator
{
	public final int precedence;
	public final String name;
	public final String token; // never null, but sometimes the empty string
	private final Class expressionNodeClass; // a subclass of NonTerminalExp corresponding to this Operator

	private static HashMap mapTokenToOperator = new HashMap(); // map of String to Operator

	/**
	 * @param precedence the operator precedence relative to other operators
	 * @param name the name of the operator, used by toString() and thus by UnknownOperationException
	 * @param token the actual characters of the operator, used to find this operator in the expression text
	 * @param expressionNodeClass the subclass of NonTerminalExp which should be created to represent this operator
	 */
	private Operator(int precedence, String name, String token, Class expressionNodeClass)
	{
		assert name != null || token != null : "Either the name or the token must be non-null"; //$NON-NLS-1$
		assert expressionNodeClass == null || NonTerminalExp.class.isAssignableFrom(expressionNodeClass) : "expressionNodeClass must be a subclass of NonTerminalExp"; //$NON-NLS-1$

		// 'token' can be null, but 'this.token' never is
		if (token == null)
			token = ""; //$NON-NLS-1$

		// if the name wasn't specified, use the token as the name
		if (name == null)
			name = token;

		this.precedence = precedence;
		this.name = name;
		this.token = token;
		this.expressionNodeClass = expressionNodeClass;

		if (token.length() > 0)
			mapTokenToOperator.put(token, this);
	}

	public String toString()
	{
		return name;
	}

	/**
	 * These constants represent the various operators that are supported as 
	 * subclasses of this node.
	 *
	 * They are numerically in order of precedence, with numbers meaning higher
	 * precedence.  The only exceptions are open-paren and open-bracket, which
	 * we've placed at an artifically low precedence in order allow the infix
	 * converter logic to work more easily.
	 * 
	 * The numbering is derived from Harbison's "C: A Reference Manual," Table 7-3.
	 * Gaps are left for operators we don't support.
	 */
	public final static Operator CLOSE_BRACKET	= new Operator(-2, null, "]", null); //$NON-NLS-1$
	public final static Operator CLOSE_PAREN	= new Operator(-2, null, ")", null); //$NON-NLS-1$
	public final static Operator OPEN_BRACKET	= new Operator(-1, null, "[", null); //$NON-NLS-1$
	public final static Operator OPEN_PAREN		= new Operator(-1, null, "(", null); //$NON-NLS-1$
	public final static Operator UNKNOWN		= new Operator(0,  "Operator.UNKNOWN", null, null); //$NON-NLS-1$

	public final static Operator ASSIGNMENT		= new Operator(2,  null, "=", AssignmentExp.class); //$NON-NLS-1$
	public final static Operator LOGICAL_OR		= new Operator(4,  null, "||", LogicOrExp.class); //$NON-NLS-1$
	public final static Operator LOGICAL_AND	= new Operator(5,  null, "&&", LogicAndExp.class); //$NON-NLS-1$
	public final static Operator BITWISE_OR		= new Operator(6,  null, "|", OrExp.class); //$NON-NLS-1$
	public final static Operator BITWISE_XOR	= new Operator(7,  null, "^", XorExp.class); //$NON-NLS-1$
	public final static Operator BITWISE_AND	= new Operator(8,  null, "&", AndExp.class); //$NON-NLS-1$
	public final static Operator RELATION_EQ	= new Operator(9,  null, "==", EqExp.class); //$NON-NLS-1$
	public final static Operator RELATION_NEQ	= new Operator(9,  null, "!=", NeqExp.class); //$NON-NLS-1$
	public final static Operator RELATION_LT	= new Operator(10, null, "<", LTExp.class); //$NON-NLS-1$
	public final static Operator RELATION_GT	= new Operator(10, null, ">", GTExp.class); //$NON-NLS-1$
	public final static Operator RELATION_LTEQ	= new Operator(10, null, "<=", LTEqExp.class); //$NON-NLS-1$
	public final static Operator RELATION_GTEQ	= new Operator(10, null, ">=", GTEqExp.class); //$NON-NLS-1$
	public final static Operator BITWISE_LSHIFT	= new Operator(11, null, "<<", LShiftExp.class); //$NON-NLS-1$
	public final static Operator BITWISE_RSHIFT	= new Operator(11, null, ">>", RShiftExp.class); //$NON-NLS-1$
	public final static Operator ARITH_ADD		= new Operator(12, null, "+", AddExp.class); //$NON-NLS-1$
	public final static Operator ARITH_SUB		= new Operator(12, null, "-", SubExp.class); //$NON-NLS-1$
	public final static Operator ARITH_MULT		= new Operator(13, null, "*", MultExp.class); //$NON-NLS-1$
	public final static Operator ARITH_DIV		= new Operator(13, null, "/", DivExp.class); //$NON-NLS-1$
	public final static Operator ARITH_MOD		= new Operator(13, null, "%", ModExp.class); //$NON-NLS-1$

	public final static Operator INDIRECTION	= new Operator(15, "Operator.INDIRECTION", null, IndirectionExp.class); // *a or a. //$NON-NLS-1$

	public final static Operator LOGICAL_NOT	= new Operator(15, null, "!", LogicNotExp.class); //$NON-NLS-1$
	public final static Operator BITWISE_NOT	= new Operator(15, null, "~", NotExp.class); //$NON-NLS-1$

	public final static Operator DIRECT_SELECT	= new Operator(17, null, ".", DotExp.class); // a.b //$NON-NLS-1$
	public final static Operator SUBSCRIPT		= new Operator(17, "Operator.SUBSCRIPT", null, SubscriptExp.class); // a[k]; see ASTBuilder.addOp() //$NON-NLS-1$

	/**
	 * We create an empty non-terminal node of the given type based on the
	 * operator.
	 */
	public NonTerminalExp createExpNode() throws UnknownOperationException
	{
		NonTerminalExp node = null;

		if (expressionNodeClass == null)
		{
			throw new UnknownOperationException(this);
		}
		else
		{
			try
			{
				node = (NonTerminalExp) expressionNodeClass.newInstance();
			}
			catch (InstantiationException e)
			{
				// should never happen
				if (Trace.error)
					Trace.trace(e.getLocalizedMessage());
			}
			catch (IllegalAccessException e)
			{
				// should never happen
				if (Trace.error)
					Trace.trace(e.getLocalizedMessage());
			}
		}

		return node;
	}

	/**
	 * Given two characters, find out which operator they refer to. We may only
	 * use the first character. The caller can figure out how many characters we
	 * used by looking at the length of Operator.token for the returned
	 * Operator.
	 * 
	 * @param firstCh
	 *            the first character
	 * @param secondCh
	 *            the second character; may or may not be used
	 * @param isIndirectionOperatorAllowed
	 *            whether the fdb indirection operators are allowed: asterisk
	 *            (*x) or trailing dot (x.)
	 */
	public static Operator opFor(char firstCh, char secondCh, boolean isIndirectionOperatorAllowed)
	{
		Operator op;

		// See if there is a two-character operator which matches these two characters
		String s = "" + firstCh + secondCh; //$NON-NLS-1$
		op = (Operator) mapTokenToOperator.get(s);
		if (op == null)
		{
			// No two-character operator, so see if there is a one-character operator
			// which matches the first character passed in
			s = "" + firstCh; //$NON-NLS-1$
			op = (Operator) mapTokenToOperator.get(s);
			if (op == null)
				op = UNKNOWN;
		}

		if (isIndirectionOperatorAllowed)
		{
			if ( op == ARITH_MULT && (secondCh == '#' || Character.isJavaIdentifierStart(secondCh)) )
				op = INDIRECTION;
		}

		return op;
	}

}
