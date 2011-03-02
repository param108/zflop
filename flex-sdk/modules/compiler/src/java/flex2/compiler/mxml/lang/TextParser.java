////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flash.css.Descriptor;
import flash.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MXML text parser, used to parse attribute values and text content. See also TextParseHandler.
 * Some utility functionality is also exposed in static methods.
 */
public abstract class TextParser
{
	/**
	 * valid percentage expressions are: [whitespace] positive-whole-or-decimal-number [whitespace] % [whitespace]
	 */
	private static final Pattern percentagePattern = Pattern.compile("\\s*((\\d+)(.(\\d)+)?)\\s*%\\s*");

	/**
	 * valid qualified names are series of 1 or more leading-alpha-or-_-followed-by-alphanumerics words, separated by dots
	 */
	private static final Pattern qualifiedNamePattern = Pattern.compile("([a-zA-Z_]\\w*)(\\.([a-zA-Z_]\\w*))*");

	/**
	 * valid AS RegExps are: / 0-or-more-of-anything / 0-or-more-flag chars. We leave pattern validation to ASC.
	 */
	private static final Pattern regExpPattern = Pattern.compile("/.*/[gimsx]*");

	//	error codes
	public final static int Ok = 0;
	public final static int ErrTypeNotEmbeddable = 1;		//	@Embed in a bad spot
	public final static int ErrInvalidTextForType = 2;		//	can't make text work as a serialized instance of type
	public final static int ErrInvalidPercentage = 3;		//	malformed percentage expression
	public final static int ErrTypeNotSerializable = 4;		//	type doesn't have a text representation at all
	public final static int ErrPercentagesNotAllowed = 5;	//	percentage not allowed here
	public final static int ErrTypeNotContextRootable = 6;	//	@ContextRoot in a bad spot
	public final static int ErrUnrecognizedAtFunction = 7;	//	@huh?()
	public final static int ErrUndefinedContextRoot = 8;	//	context-root not defined

	//	processing flags
	public final static int FlagInCDATA = 1;
	public final static int FlagCollapseWhiteSpace = 2;
	public final static int FlagConvertColorNames = 4;
	public final static int FlagAllowPercentages = 8;

	private final TypeTable typeTable;

    public TextParser(TypeTable typeTable)
	{
		this.typeTable = typeTable;
	}

	/**
	 * called when an @ContextRoot has been recognized, in an ok spot. Handler should return a String
	 * @param text original @ContextRoot expression, unmodified
	 * @return whatever you want parse() to return
	 */
	protected abstract String contextRoot(String text);

	/**
	 * called when an @Embed has been recognized, in an ok spot. Handler should return a VO
	 * @param text original @Embed expression, unmodified
	 * @param type
	 * @return whatever you want parse() to return
	 */
	protected abstract Object embed(String text, Type type);

	/**
	 * called when an @Resource has been recognized, in an ok spot. Handler should return a VO
	 * @param text original @Resource expression, unmodified
	 * @param type
	 * @return whatever you want parse() to return
	 */
	protected abstract Object resource(String text, Type type);

	/**
	 * called when a binding expression has been parsed. Handler should return a VO
	 * @param converted converted binding expression
	 * @return whatever you want parse() to return
	 */
	protected abstract Object bindingExpression(String converted);

	/**
	 * called when a valid percentage string has been parsed. Callback allows subs to do nasty stuff like
	 * property-name swapping, etc.
	 * @param pct canonicalized percentage string
	 * @return whatever you want parse() to return
	 */
	protected abstract Object percentage(String pct);

	/**
	 * called when an array expression has been parsed. Handler should return a VO
	 * @param entries Collection of parsed array entries
	 * @param arrayElementType
	 * @return whatever you want parse() to return
	 */
	protected abstract Object array(Collection entries, Type arrayElementType);

	/**
	 * called when a text value has been parsed for type Function. Handler normally returns the unmodified name
	 */
	protected abstract Object functionText(String name);

	/**
	 * called when a text value has been parsed for type Class. Handler normally returns the unmodified name
	 * if type == Class; a Primitive if type is an instance factory.
	 */
	protected abstract Object className(String name, Type type);

	/**
	 * called on a parse error
	 * @param error one of the constants defined in this interface
	 * @param text erroneous text
	 * @param type type for which parse was requested
	 * @param arrayElementType
	 */
	protected abstract void error(int error, String text, Type type, Type arrayElementType);

	/**
	 * type-directed text parsing. Search order is:
	 * 1. look for a binding expression, if applicable.
	 * 2. then look for an embed.
	 * 3. then look for a resource.
	 * 4. if nothing is found above, attempt to deserialize a value of the specified type[arrayElementType]
	 */
	protected Object parse(String text, Type type, Type arrayElementType, int flags)
	{
		if (!inCDATA(flags))
		{
			//	binding?
			String bindingExpressionText = getBindingExpressionFromString(text);
			if (bindingExpressionText != null)
			{
				// C: If 'text' has leading line breaks in CDATA, bindingExpression() should add the number of line breaks
				//    to the line number when setting up the binding expression.
				return bindingExpression(bindingExpressionText);
			}
			else
			{
				text = cleanupBindingEscapes(text);
			}

			//	@func() ?
			String atFunctionName = getAtFunctionName(text);
			if (atFunctionName != null)
			{
				return parseAtFunction(atFunctionName, text, type, arrayElementType, flags);
			}
		}

		//	ordinary value
		return parseValue(text, type, arrayElementType, flags);
	}

	/**
	 * do type-directed deserialization of typed constant value from text
	 * NOTE using equals() not isAssignableTo() for type testing - all tested classes are final as of 8/4/05
	 * TODO assertions confirming type finality
	 */
	private Object parseValue(String text, Type type, Type arrayElementType, int flags)
	{
		boolean isint = false, isuint = false;	//	temps

		Object result = null;

		if (type.equals(typeTable.noType) || type.equals(typeTable.objectType))
		{
			result = parseObject(text, arrayElementType, flags);
		}
		else if (type.equals(typeTable.stringType))
		{
			result = parseString(text, flags);
		}
		else if (type.equals(typeTable.numberType) ||
				(isint = type.equals(typeTable.intType)) ||
				(isuint = type.equals(typeTable.uintType)))
		{
			if (text.indexOf('%') >= 0)
			{
				if (allowPercentages(flags))
				{
					if ((result = parsePercentage(text)) != null)
					{
						result = percentage((String)result);
					}
					else
					{
						result = new ParseError(ErrInvalidPercentage);
					}
				}
				else
				{
					result = new ParseError(ErrPercentagesNotAllowed);
				}
			}
			else
			{
				result = isint ? parseInt(text, flags) :
						isuint ? parseUInt(text, flags) :
						parseNumber(text, flags);
			}
		}
		else if (type.equals(typeTable.booleanType))
		{
			result = parseBoolean(text);
		}
		else if (type.equals(typeTable.regExpType))
		{
			result = parseRegExp(text);
		}
		else if (type.equals(typeTable.arrayType))
		{
			Collection c = parseArray(text, arrayElementType, true);
			result = c != null ? array(c, arrayElementType) : null;
		}
		else if (type.equals(typeTable.functionType))
		{
			String f = parseFunction(text);
			result = f != null ? functionText(f) : null;
		}
		else if (acceptsClassRef(type))
		{
			String c = parseClassName(text);
			result = c != null ? className(c, type) : null;
		}
		else
		{
			result = new ParseError(ErrTypeNotSerializable);
		}

		//	handle/return

		if (result == null)
		{
			result = new ParseError(ErrInvalidTextForType);
		}

		if (result instanceof ParseError)
		{
			error(((ParseError)result).errno, text, type, arrayElementType);
			return null;
		}
		else
		{
			return result;
		}
	}

	/**
	 *
	 */
	private boolean acceptsClassRef(Type type)
	{
		return type.equals(typeTable.classType) || StandardDefs.isInstanceGenerator(type);
	}

	/**
	 *
	 */
	private Object parseObject(String text, Type arrayElementType, int flags)
	{
		String temp = text.trim();

		Object result;
		if ((result = parseBoolean(temp)) != null)
		{
			return result;
		}
		else if ((result = parseArray(temp, arrayElementType, false)) != null)
		{
			return array((Collection) result, arrayElementType);
		}
		else if ((result = parseNumber(temp, flags)) != null)
		{
			return result;
		}
		else
		{
			return text;
		}
	}

	/**
	 *
	 */
	private Collection parseArray(String text, Type elementType, boolean coerceSingleton)
	{
		text = text.trim();

		if (!isArray(text))
		{
			if (coerceSingleton)
			{
				Object element = parseValue(text, elementType, typeTable.objectType, 0);
				return element != null ? Collections.singleton(element) : null;
			}
			else
			{
				return null;
			}
		}

		if (isEmptyArray(text))
		{
			return Collections.EMPTY_LIST;
		}

		Collection result = new ArrayList();
		StringBuffer buffer = new StringBuffer();
		char quoteChar = '\'';
		boolean inQuotes = false;

		for (int index = 1, length = text.length(); index < length; index++)
		{
			char c = text.charAt(index);

			switch (c)
			{
			case '[':
				if (inQuotes)
				{
					buffer.append(c);
				}
				else
				{
					//	TODO nested arrays?
				}
				break;
			case '"':
			case '\'':
				if (inQuotes)
				{
					if (quoteChar == c)
					{
						inQuotes = false;
					}
					else
					{
						buffer.append(c);
					}
				}
				else
				{
					inQuotes = true;
					quoteChar = c;
				}
				break;
			case ',':
			case ']':
				if (inQuotes)
				{
					buffer.append(c);
				}
				else
				{
					String elementText = buffer.toString().trim();
					buffer = new StringBuffer();

					//	NOTE clear any special-processing flags, on the interpretation that they only apply to top-level scalars.
					//	TODO multi-level typed arrays? :)
					Object element = parseValue(elementText, elementType, typeTable.objectType, 0);
                    if (element != null)
					{
						result.add(element);
					}
					else
					{
						return null;
					}
				}
				break;
			default:
				buffer.append(c);
			}
		}

		return result;
	}

	/**
	 *
	 */
	private boolean hasLeadingZeros(String s)
	{
		boolean result = false;
		int n = s.length();
		if (n > 1 && s.charAt(0) == '0' &&
			!(s.startsWith("0x") || s.startsWith("0X") || s.startsWith("0.")))
		{
			result = true;
		}
		return result;
	}

	/**
	 * We accept 0x and # prefixes.
	 */
	private Integer parseInt(String s, int flags)
	{
		if (convertColorNames(flags))
		{
			String c = Descriptor.convertColorName(s);
			if (c != null)
			{
				s = c;
			}
		}

		try
		{
			// Don't parse int's with leading zeros, which are not octal.
			// For example, a MA zip code, 02127.
			if (hasLeadingZeros(s))
			{
                return null;
			}
			else
			{
				return Integer.decode(s);
			}
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 *
	 */
	private Long parseUInt(String s, int flags)
	{
		if (convertColorNames(flags))
		{
			String c = Descriptor.convertColorName(s);
			if (c != null)
			{
				s = c;
			}
		}

		try
		{
			// Don't parse uint's with leading zeros, which are not octal.
			// For example, a MA zip code, 02127.
			if (hasLeadingZeros(s))
			{
                return null;
			}
			else
			{
				Long l = Long.decode(s);
				long val = l.longValue();
				return (val == java.lang.Math.abs(val) && val <= 0xffffffffL) ? l : null;
			}
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 *
	 */
	private Number parseNumber(String s, int flags)
	{
		// Don't parse Number's with leading zeros, which are not octal.
		// For example, a MA zip code, 02127.
		if (hasLeadingZeros(s))
		{
			return null;
		}

		Integer integer = parseInt(s, flags);
		if (integer != null)
		{
			return integer;
		}
		else
		{
			try
			{
				return Double.valueOf(s);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}
	}

	/**
	 *
	 */
	private String parseString(String text, int flags)
	{
		if (collapseWhiteSpace(flags) && !inCDATA(flags))
		{
			text = StringUtils.collapseWhitespace(text, ' ');
		}

		if (text.length() > 1 && text.charAt(0) == '\\' && "\\@".indexOf(text.charAt(1)) >= 0)
		{
			//	'\' is being used to begin the string with a literal '\' or '@'
			//	NOTE: currently, we only attach special meaning to "@name(...)" when it begins a string.
			text = text.substring(1);
		}

		return text;
	}

	/**
	 *
	 */
	private static Boolean parseBoolean(String text)
	{
		// If we get false, make sure its because the user specified 'false'
		Boolean b = Boolean.valueOf(StringUtils.collapseWhitespace(text, ' '));
		return b.booleanValue() || text.equalsIgnoreCase("false") ? b : null;
	}

	/**
	 *
	 */
	private static String parseRegExp(String text)
	{
		Matcher m = regExpPattern.matcher(text);
		return m.matches() ? m.group(0) : null;
	}

	/**
	 * NOTE returns canonicalized percentage expression in a String - not the percentage itself
	 */
	private static String parsePercentage(String text)
	{
		Matcher m = percentagePattern.matcher(text);
		return m.matches() ? m.group(1) + '%' : null;
	}

	/**
	 * TODO there was a TODO in 1.5 about parsing source code... ?
	 */
	private static String parseFunction(String text)
	{
		return text.trim();
	}

	/**
	 *
	 */
	public static String parseClassName(String text)
	{
		String name = text.trim();
		return isQualifiedName(name) ? name : null;
	}

	/**
	 *
	 */
	private static class ParseError
	{
		final int errno;
		ParseError(final int errno) { this.errno = errno; }
	}

	/**
	 * flag extraction
	 */
	private static final boolean inCDATA(int flags) { return (flags & FlagInCDATA) == FlagInCDATA; }
	private static final boolean collapseWhiteSpace(int flags) { return (flags & FlagCollapseWhiteSpace) == FlagCollapseWhiteSpace; }
	private static final boolean convertColorNames(int flags) { return (flags & FlagConvertColorNames) == FlagConvertColorNames; }
	private static final boolean allowPercentages(int flags) { return (flags & FlagAllowPercentages) == FlagAllowPercentages; }

	/**
	 * TODO make private
	 * Get rid of backslashes that were escaping curly braces
	 * @param toClean
	 * @return the cleaned string
	 */
	public static String cleanupBindingEscapes(String toClean)
	{
		toClean = StringUtils.cleanupEscapedChar('{', toClean);
		toClean = StringUtils.cleanupEscapedChar('}', toClean);
		return toClean;
	}

	/**
	 * TODO make private
	 * replace backslashes for curly braces with &#7d; &#7b;
	 * @param toClean
	 * @return the cleaned string
	 */
	public static String replaceBindingEscapesForE4X(String toClean)
	{
		toClean = StringUtils.cleanupEscapedCharForXML('{', toClean);
		toClean = StringUtils.cleanupEscapedCharForXML('}', toClean);
		return toClean;
	}
	
	/**
	 *
	 */
	public static boolean isBindingExpression(String s)
	{
		int openBraceIdx = StringUtils.findNthUnescaped('{', 1, s);
		if (openBraceIdx == -1)
		{
			return false;
		}

		int closeBraceIdx = StringUtils.findClosingToken('{', '}', s, openBraceIdx);
		if (closeBraceIdx == -1)
		{
			return false;
		}

		return true;
	}

	/**
	 * TODO make private
	 * Given a single string see if it is a binding expression and return a new expression that separates
	 * out strings and AS code.  If it is not a binding expression return null.
	 * E.g.,
	 * input: Hello {firstName}, my name is {myName}.
	 * output: "Hello " + firstName + ", my name is " + myName + "."
	 * @param s
	 * @return the converted string or null if it was not a binding expression
	 */
	public static String getBindingExpressionFromString(String s)
	{
		assert s != null;

		int openBraceIdx = StringUtils.findNthUnescaped('{', 1, s);
		if (openBraceIdx == -1)
		{
			return null;
		}

		int closeBraceIdx = StringUtils.findClosingToken('{', '}', s, openBraceIdx);
		if (closeBraceIdx == -1)
		{
			return null;
		}

		StringBuffer buf = new StringBuffer();

		//first attach the leading part of the string, all the way up to the opening brace
		String lead = s.substring(0, openBraceIdx);
		//only if there was non-whitespace
		if (!lead.trim().equals(""))
		{
			buf.append(StringUtils.formatString(cleanupBindingEscapes(lead)));
			buf.append(" + ");
		}

		//now loop, attaching the piece between braces and the next string if it exists
		while (openBraceIdx != -1)
		{
			//attach this { } (don't include the braces but do use parentheses to group the thing together)
			buf.append("(");
			String contents = s.substring(openBraceIdx + 1, closeBraceIdx);
			if (contents.trim().equals(""))
			{
				//	logWarning("Empty {} in binding expression.");
				contents = "''";
			}
			buf.append(contents);
			buf.append(")");
			//now see if there's a tail part to add
			int lastClose = closeBraceIdx;
			openBraceIdx = StringUtils.findNextUnescaped('{', lastClose, s);
			if (openBraceIdx != -1)
			{
				buf.append(" + ");
				closeBraceIdx = StringUtils.findClosingToken('{', '}', s, openBraceIdx);
				if (closeBraceIdx != -1)
				{
					buf.append(StringUtils.formatString(cleanupBindingEscapes(s.substring(lastClose + 1, openBraceIdx))));
					buf.append(" + ");
				}
				else
				{
					buf.append(StringUtils.formatString(s.substring(lastClose + 1)));
					openBraceIdx = -1; //make sure to finish the loop
				}
			}
			else
			{
				String tail = s.substring(lastClose + 1);
				if (!tail.trim().equals(""))
				{
					buf.append(" + ");
					buf.append(StringUtils.formatString(cleanupBindingEscapes(tail)));
				}
			}
		}

        return buf.toString();
	}

	/**
	 * 
	 */
	//TODO ideally private like getBindingExpressionFromString should be...
	public static String getAtFunctionName(String value)
	{
		value = value.trim();

		if (value.length() > 1 && value.charAt(0) == '@')
		{
			int openParen = value.indexOf('(');

			// A function must have an open paren and a close paren after the open paren.
			if (openParen > 1 && value.indexOf(')') > openParen)
			{
				return value.substring(1, openParen);
			}
		}

		return null;
	}

	/**
	 *
	 */
	private Object parseAtFunction(String functionName, String text, Type type, Type arrayElementType, int flags)
	{
		Object result = null;

		if ("Embed".equals(functionName))
		{
			// @Embed requires that lvalue accept String or Class
			if (typeTable.stringType.isAssignableTo(type) || acceptsClassRef(type))
			{
				result = embed(text, type);
			}
			else
			{
				error(ErrTypeNotEmbeddable, text, type, arrayElementType);
			}
		}
		else if ("ContextRoot".equals(functionName))
		{
			// @ContextRoot requires a String lvalue
			if (typeTable.stringType.isAssignableTo(type))
			{
				result = contextRoot(text);
			}
			else
			{
				error(ErrTypeNotContextRootable, text, type, arrayElementType);
			}
		}
		else if ("Resource".equals(functionName))
		{
			result = resource(text, type);
		}
		else
		{
			error(ErrUnrecognizedAtFunction, text, type, arrayElementType);
		}

		return result;
	}

	/**
	 *
	 */
	private static boolean isArray(String text)
	{
		boolean result = true;

		if ((text.length() < 2) ||
				(text.charAt(0) != '[') ||
				(text.charAt(text.length() - 1) != ']'))
		{
			result = false;
		}

		return result;
	}

	private static boolean isEmptyArray(String text)
	{
		boolean result = false;

		if (isArray(text) && text.substring(1, text.length() - 1).trim().length() == 0)
		{
			result = true;
		}

		return result;
	}

	/**
	 * test if this is a valid identifier, and is not an actionscript keyword.
	 */
	public static boolean isValidIdentifier(String id)
	{
		if (id.length() == 0 || !isIdentifierFirstChar(id.charAt(0)))
		{
			return false;
		}

		for (int i=1; i < id.length(); i++)
		{
			if (!isIdentifierChar(id.charAt(i)))
			{
				return false;
			}
		}

		if (StandardDefs.isReservedWord(id))
		{
			return false;
		}

		return true;
	}

	/**
	 *
	 */
	private static boolean isIdentifierFirstChar(char ch)
    {
        return Character.isJavaIdentifierStart(ch);
    }

	/**
	 *
	 */
	private static boolean isIdentifierChar(int ch)
    {
        return ch != -1 && Character.isJavaIdentifierPart((char)ch);
    }

	/**
	 *
	 */
	private static boolean isQualifiedName(String text)
	{
		return qualifiedNamePattern.matcher(text).matches() && !StandardDefs.isReservedWord(text);
	}
}
