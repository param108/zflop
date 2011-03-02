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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import flash.localization.LocalizationManager;
import flash.tools.debugger.DebuggerLocalizer;
import flash.util.Trace;

/**
 * ASTBuilder.java
 * 
 *    This class creates an abstract syntax tree representation
 *    of an expression given a sequence of tokens.
 * 
 *    The tree is built in a single pass in a fairly traditional
 *    manner using an expression stack and an operator stack to 
 *    convert to infix notation on the fly.
 *
 *    No compression is performed on the tree, thus expressions
 *    such as (3*4) will result in 3 nodes.
 * 
 */
public class ASTBuilder implements IASTBuilder
{
	private static LocalizationManager m_localizationManager;

	/* stick this here until I'm ready to make a better implementation */
	private static class OperatorStack
	{
		private Stack m_stack = new Stack();

		public void push(Operator op)	{ m_stack.push(op); }
		public Operator pop()			{ return (Operator)m_stack.pop(); } 
		public boolean empty()			{ return m_stack.empty(); }
		public Operator peek()			{ return (Operator)m_stack.peek(); } 
		public int size()				{ return m_stack.size(); } 
		public void clear()				{ m_stack.clear(); }
	}

	private Stack			m_expStack;
	private OperatorStack	m_opStack;

	/* set when the reader has reached EOF (in parsing) */
	private boolean m_readerEOF = false;
	private int     m_parsePos = 0;

	/**
	 * whether the fdb indirection operators are allowed, e.g. asterisk (*x) or
	 * trailing dot (x.)
	 */
	private boolean m_isIndirectionOperatorAllowed = true;

	static
	{
        // set up for localizing messages
        m_localizationManager = new LocalizationManager();
        m_localizationManager.addLocalizer( new DebuggerLocalizer("flash.tools.debugger.expression.expression.") ); //$NON-NLS-1$
	}

	/**
	 * @param isIndirectionOperatorAllowed
	 *            whether the fdb indirection operators are allowed, e.g.
	 *            asterisk (*x) or trailing dot (x.)
	 */
	public ASTBuilder(boolean isIndirectionOperatorAllowed)
	{
		m_expStack = new Stack();
		m_opStack = new OperatorStack();
		m_isIndirectionOperatorAllowed = isIndirectionOperatorAllowed;
	}

	/**
	 * @return whether the fdb indirection operators are allowed, e.g. asterisk
	 *         (*x) or trailing dot (x.)
	 */
	public boolean isIndirectionOperatorAllowed()
	{
		return m_isIndirectionOperatorAllowed;
	}

	/**
	 * The front-end has finished so we pop the 
	 * stack until it empty 
	 */
	private ValueExp done() throws EmptyStackException, UnknownOperationException, IncompleteExpressionException
	{
		while(!m_opStack.empty())
			popOperation();

		/* should only have one entry on the expression stack */
		ValueExp tree = (ValueExp)m_expStack.pop();

		if ( !m_expStack.empty() || !m_opStack.empty() )
		{
			/* clear the stacks */
			while(!m_expStack.empty())
				m_expStack.pop();

			while(!m_opStack.empty())
				m_opStack.pop();

			throw new IncompleteExpressionException();
		}

		return tree;
	}

	/**
	 * Here's where the good stuff happens...
	 * 
	 * 1. If we get an open parenthesis or open bracket, then we push it.
	 * 2. If we get a close parenthesis or close bracket, the we pop the stack
	 *    growing our tree, until we find an open parenthesis or open bracket.
	 *    The type must match, e.g. if we are processing a close parenthesis
	 *    but encounter an open bracket, that indicates a syntax error in the
	 *    expression, and we throw an exception.
	 * 3. If our op has lower precedence then the current TOS op
	 *    we grow the tree and repeat (3).
	 * 
	 * Note we are using a trick, whereby the opType constants
	 * are number in order of precedence (that is lower precedence 
	 * = lower numeric value).  The exception to this rule is 
	 * the open parenthesis which is highest precedence but is
	 * lowest numerically.  This is to allow (3) to work correctly,
	 * since we want to make sure that a open parenthesis only gets
	 * popped by a close parenthesis.
	 */
	private void addOp(Operator opType) throws EmptyStackException, UnknownOperationException, ParseException
	{
		if (opType == Operator.OPEN_PAREN)
		{
			m_opStack.push(opType);
		}
		else if (opType == Operator.OPEN_BRACKET)
		{
			// This is tricky: for an array index, e.g. "a[k]", we treat it
			// like "a SUBSCRIPT k".  So first we push SUBSCRIPT, and
			// then we push the open-bracket op.
			addOp(Operator.SUBSCRIPT); // recursive call
			m_opStack.push(opType);
		}
		else if (opType == Operator.CLOSE_PAREN || opType == Operator.CLOSE_BRACKET)
		{
			Operator openingOpType;

			if (opType == Operator.CLOSE_PAREN)
				openingOpType = Operator.OPEN_PAREN;
			else
				openingOpType = Operator.OPEN_BRACKET;

			while (m_opStack.peek() != Operator.OPEN_PAREN &&
				   m_opStack.peek() != Operator.OPEN_BRACKET)
			{
				popOperation();
			}

			// If we saw "(x]" or "[x)", then indicate a syntax error
			if (m_opStack.peek() != openingOpType)
				throw new ParseException(getLocalizationManager().getLocalizedTextString("key1"), m_parsePos); //$NON-NLS-1$

			popOperation(); // pop the "(" or "["
		}
		else
		{
			while ( !m_opStack.empty() && (m_opStack.peek().precedence >= opType.precedence) )
				popOperation();

			m_opStack.push(opType);
		}
	}

	private void addVariable(String name)
	{
		/* create a variable node push */
		VariableExp node = VariableExp.create(name);
		m_expStack.push(node);
	}

	private void addInternalVariable(String name)
	{
		/* create a variable node push */
		VariableExp node = InternalVariableExp.create(name);
		m_expStack.push(node);
	}

	private void addLong(long value)
	{
		/* create a constant node and push */
		ConstantExp node = ConstantExp.create(value);
		m_expStack.push(node);
	}

	private void addBoolean(boolean value)
	{
		/* create a constant node and push */
		ConstantBooleanExp node = ConstantBooleanExp.create(value);
		m_expStack.push(node);
	}

	private void addString(String text)
	{
		StringExp node = StringExp.create(text);
		m_expStack.push(node);
	}

	/**
	 * Pop the next operation off the stack and build a non-terminal node
	 * around it, poping the next two items from the expression stack 
	 * as its children 
	 */
	private void popOperation() throws UnknownOperationException
	{
		Operator op = m_opStack.pop();

		/* dispose of stack place holder ops (e.g.. OPEN_PAREN and OPEN_BRACKET) */
		if (op.precedence < 0)
			return;

		if (isIndirectionOperatorAllowed())
		{
			/**
			 * Special case to support trailing dot syntax (e.g. a.b. )
			 * If DotOp and nothing on stack then we convert to
			 * an indirection op
			 */
			if (op == Operator.DIRECT_SELECT && m_expStack.size() < 2)
				op = Operator.INDIRECTION;
		}

		// create operation and set its nodes
		NonTerminalExp node = op.createExpNode();

		node.setRightChild( (ValueExp)m_expStack.pop() );

		if ( !(node instanceof SingleArgumentExp) )
			node.setLeftChild( (ValueExp)m_expStack.pop() );

		m_expStack.push(node);
	}

	/*
	 * @see flash.tools.debugger.expression.IASTBuilder#parse(java.io.Reader)
	 */
	public ValueExp parse(Reader in) throws IOException, EmptyStackException, UnknownOperationException, IncompleteExpressionException, ParseException { return parse(in, true); }

	/*
	 * @see flash.tools.debugger.expression.IASTBuilder#parse(java.io.Reader, boolean)
	 */
	public ValueExp parse(Reader in, boolean ignoreUnknownCharacters) throws IOException, EmptyStackException, UnknownOperationException, IncompleteExpressionException, ParseException
	{
		try
		{
			StringBuffer sb = new StringBuffer();
			boolean skipRead = false;
			boolean inDot = false;
			char ch = ' ';
	
			m_readerEOF = false;
			m_parsePos = 0;

			while(!m_readerEOF)
			{
				if (!skipRead)
					ch = readChar(in);

				/* whitespace? => ignore */
				if (m_readerEOF || Character.isWhitespace(ch))
				{
					skipRead = false;
					inDot = false;
				}

				/* A number? => parse constant */
				else if (!inDot && Character.isDigit(ch))
				{
					/* build up our value */
					int base = 10;
					long n = Character.digit(ch, base);

					ch = readChar(in);

					/* support for hex values */
					if ( ch == 'x' || ch == 'X' )
					{
						base = 16;
						ch = readChar(in);
					}

					while( Character.isLetterOrDigit(ch) )
					{
						n = (n*base) + Character.digit(ch, base);
						ch = readChar(in);
					}

					/* now add the constant */
					addLong(n);
					skipRead = true;
				}

				/* special demarcation for internal variables */
				else if (ch == '$')
				{
					sb.setLength(0);
					do
					{
						sb.append(ch);
						ch = readChar(in);
					} 
					while(Character.isJavaIdentifierPart(ch));

					/* now add it */
					addInternalVariable(sb.toString());
					sb.setLength(0);
					skipRead = true;
				}

				/* letter? => parse variable, accept #N, where N is entity id */
				else if ( Character.isJavaIdentifierStart(ch) || ch == '#' || (Character.isDigit(ch) && inDot) )
				{
					sb.setLength(0);
					do
					{
						sb.append(ch);
						ch = readChar(in);
					} 
					while(Character.isJavaIdentifierPart(ch));

					/* now add it ; true/false look like variables but are not */
					String s = sb.toString();
					if (s.equals("true")) //$NON-NLS-1$
						addBoolean(true);
					else if (s.equals("false")) //$NON-NLS-1$
						addBoolean(false);
					else
						addVariable(s);

					sb.setLength(0);
					skipRead = true;
				}

				/* quote? => parse string */
				else if ( ch == '\'' || ch == '\"' )
				{
					/* go until we find matching */
					char matching = ch;

					do 
					{
						ch = readChar(in);
						sb.append(ch);
					}
					while(!m_readerEOF && ch != matching);

					/* add it */
					int to = sb.length()-1;
					addString(sb.toString().substring(0, to));
					sb.setLength(0);
					skipRead = false;
				}

				else if (inDot && ch == '*')
				{
					// This is for the XML syntax, myXmlObject.*
					// to refer to all children of an XML object
					addVariable("*"); //$NON-NLS-1$
					inDot = false;
					skipRead = false;
				}

				/* could be an operator? */
				else 
				{
					/* do a lookup */
					char lookaheadCh = readChar(in);
					Operator op = Operator.opFor(ch, lookaheadCh, isIndirectionOperatorAllowed());

					if (op == Operator.UNKNOWN && !ignoreUnknownCharacters)
					{
						Map args = new HashMap();
						args.put("arg1", ""+ch); //$NON-NLS-1$ //$NON-NLS-2$
						throw new ParseException(getLocalizationManager().getLocalizedTextString("key2", args), m_parsePos); //$NON-NLS-1$
					}
					else
					{
						addOp(op);
						skipRead = (op.token.length() == 1 || op == Operator.INDIRECTION) ? true : false;	/* did we consume the character? */
						if (skipRead)
							ch = lookaheadCh;
						inDot = (op == Operator.DIRECT_SELECT) ? true : false;
					}
				}
			}

			/* now return the root node of the tree */
			return done();
		}
		finally
		{
			// We need to do this in case any exceptions were thrown, so that the
			// next time we're called to evaluate an expression, the stacks are
			// empty.
			m_expStack.clear();
			m_opStack.clear();
		}
	}

	/* read a character from a reader, throw end of stream exception if done */
	private char readChar(Reader in) throws IOException
	{
		int c = (char)' ';

		if (!m_readerEOF)
		{
			c = in.read();
			m_parsePos++;
			if (c < 0)
				m_readerEOF = true;
		}
		return (char)c;
	}

	public final static void main(String[] args)
	{
		ASTBuilder ab = new ASTBuilder(true);

		try
		{
			ab.addLong(5);
			ab.addOp(Operator.ARITH_SUB);
			ab.addLong(6);

			ValueExp exp1 = ab.done();

			ab.addLong(5);
			ab.addOp(Operator.ARITH_ADD);
			ab.addOp(Operator.OPEN_PAREN);
			ab.addLong(6);
			ab.addOp(Operator.ARITH_DIV);
			ab.addLong(4);
			ab.addOp(Operator.ARITH_MULT);
			ab.addLong(7);
			ab.addOp(Operator.CLOSE_PAREN);
			ab.addOp(Operator.BITWISE_RSHIFT);
			ab.addLong(2);

			ValueExp exp2 = ab.done();

			ValueExp exp3 = ab.parse(new StringReader("5-6")); //$NON-NLS-1$
			ValueExp exp4 = ab.parse(new StringReader("5 +(6/4*7 )>>2")); //$NON-NLS-1$

			ValueExp exp5 = ab.parse(new StringReader(" 4 == 2")); //$NON-NLS-1$

			Object o1 = exp1.evaluate(null);
			Object o2 = exp2.evaluate(null);
			Object o3 = exp3.evaluate(null);
			Object o4 = exp4.evaluate(null);
			Object o5 = exp5.evaluate(null);
			
			System.out.println("="+o1+","+o2); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("="+o3+","+o4); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("="+o5); //$NON-NLS-1$
		}
		catch(Exception e)
		{
			if (Trace.error)
				e.printStackTrace(); 
		}
	}


	static LocalizationManager getLocalizationManager()
	{
		return m_localizationManager;
	}
}
