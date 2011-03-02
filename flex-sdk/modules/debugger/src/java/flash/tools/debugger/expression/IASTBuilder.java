////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger.expression;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.EmptyStackException;

public interface IASTBuilder
{
	/**
	 * A rather stupid parser that should do a fairly good job at
	 * parsing a general expression string 
	 * 
	 * Special mode for processing '.' since a.0 is legal in ActionScript
	 * 
	 * Exceptions:
	 *	EmptyStackException - no expression was built!
	 *  UnknownOperationException - there was an unknown operation placed into the expression
	 *  IncompleteExpressionException - most likely missing parenthesis.
	 *  ParseException - a general parsing error occurred.
	 * 
	 */
	public ValueExp parse(Reader in)
		throws IOException, EmptyStackException, UnknownOperationException, IncompleteExpressionException, ParseException;

	public ValueExp parse(Reader in, boolean ignoreUnknownCharacters)
		throws IOException, EmptyStackException, UnknownOperationException, IncompleteExpressionException, ParseException;
}
