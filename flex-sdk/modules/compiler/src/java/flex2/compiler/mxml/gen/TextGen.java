////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.gen;

import java.util.Iterator;

/**
 * pile of text generation utilities
 * TODO where to?
 */
public class TextGen
{
	/**
	 * Enclose a string in double quotes. NOTE: does not encode embedded double quotes, hence the name.
	 */
	public static String quoteWord(String s)
	{
		return '\"' + s + '\"';
	}

	/**
	 * Takes iterator over String, and separator string, and returns a separated list as String, e.g.
	 * ["a","b","c"] and ", " -> "a, b, c"
	 */
	public static String toSepList(Iterator stringIter, String sep)
	{
		StringBuffer sb = new StringBuffer();

		if (stringIter.hasNext())
		{
			sb.append((String)stringIter.next());
		}

		while (stringIter.hasNext())
		{
			sb.append(sep);
			sb.append((String)stringIter.next());
		}

		return sb.toString();
	}

	/**
	 * ["a","b","c"] and ", " -> "a, b, c"
	 */
	public static String toCommaList(Iterator stringIter)
	{
		return toSepList(stringIter, ", ");
	}

	/**
	 * ["a","b","c"] and "<prefix>" -> "<prefix>a<prefix>b<prefix>c"
	 */
	public static String prefix(Iterator stringIter, String prefix)
	{
		StringBuffer sb = new StringBuffer();

		while (stringIter.hasNext())
		{
			sb.append(prefix);
			sb.append((String)stringIter.next());
		}

		return sb.toString();
	}

	/**
	 * "(s)" -> "s", others unchanged
	 */
	public static String stripParens(String sourceExpression)
	{
		if (sourceExpression.startsWith("(") && sourceExpression.endsWith(")"))
		{
			sourceExpression = sourceExpression.substring(1, sourceExpression.length() - 1);
		}
		return sourceExpression;
	}

}
