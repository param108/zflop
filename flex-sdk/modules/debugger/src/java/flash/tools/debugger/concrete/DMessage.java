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

package flash.tools.debugger.concrete;

import java.lang.ArrayIndexOutOfBoundsException;
import java.io.UnsupportedEncodingException;

import flash.util.FieldFormat;
import flash.util.Trace;

/**
 * DMessage.java
 *
 *    Wraps the contents of a specific message and provides a set of APIs that allow for
 *    typed extraction of fields within the message.
 *
 *    No interpretation of the messages contents is performed, this is left to the
 *    user of this class.  The code was constructed in this fashion, so that it more
 *    closely mimics the DataReader/DataWriter classes used in the Player code.
 *
 *    The type of the message should be one of the InXXX or OutXXX constant integers,
 *    but no checking of conformance is provided in this class.
 */
public class DMessage
{
	/**
     * This set of constants defines the message types RECEIVED from the player
	 * through our debug socket
	 */
	public static final int InUnknown					= -1;
	public static final int InSetMenuState				=  0;
	public static final int InSetProperty				=  1;
	public static final int InExit						=  2;
	public static final int InNewObject					=  3;
	public static final int InRemoveObject				=  4;
	public static final int InTrace						=  5;
	public static final int InErrorTarget				=  6;
	public static final int InErrorExecLimit			=  7;
	public static final int InErrorWith					=  8;
	public static final int InErrorProtoLimit			=  9;
	public static final int InSetVariable				= 10;
	public static final int InDeleteVariable			= 11;
	public static final int InParam						= 12;
	public static final int InPlaceObject				= 13;
	public static final int InScript					= 14;
	public static final int InAskBreakpoints			= 15;
	public static final int InBreakAt					= 16;
	public static final int InContinue					= 17;
	public static final int InSetLocalVariables			= 18;
	public static final int InSetBreakpoint				= 19;
	public static final int InNumScript					= 20;
	public static final int InRemoveScript				= 21;
	public static final int InRemoveBreakpoint			= 22;
	public static final int InNotSynced					= 23;
	public static final int InErrorURLOpen				= 24;
	public static final int InProcessTag				= 25;
	public static final int InVersion					= 26;
	public static final int InBreakAtExt				= 27;
	public static final int InSetVariable2				= 28;
	public static final int InSquelch					= 29;
	public static final int InGetVariable				= 30;
	public static final int InFrame						= 31;
	public static final int InOption					= 32;
    public static final int InWatch						= 33;
    public static final int InGetSwf					= 34;
    public static final int InGetSwd					= 35;
	public static final int InErrorException			= 36;
	public static final int InErrorStackUnderflow		= 37;
	public static final int InErrorZeroDivide			= 38;
	public static final int InErrorScriptStuck			= 39;
	public static final int InBreakReason				= 40;
	public static final int InGetActions				= 41;
	public static final int InSwfInfo					= 42;
	public static final int InConstantPool				= 43;
	public static final int InErrorConsole				= 44;
    public static final int InGetFncNames				= 45;
	// 46 through 52 are for profiling
    public static final int InWatch2					= 55;
    // If you add another message here, adjust the following line
    // and add a new case to the inTypeName() method below.
	public static final int InSIZE						= InWatch2 + 1;	 /* last ID used +1 */

	/**
	 * This set of constants defines the message types SENT to the player from our
     * debug socket (WARNING: ID space overlaps with InXXX)
	 */
	public static final int OutUnknown					= -2;
	public static final int OutZoomIn					=  0;
	public static final int OutZoomOut					=  1;
	public static final int OutZoom100					=  2;
	public static final int OutHome						=  3;
	public static final int OutSetQuality				=  4;
	public static final int OutPlay						=  5;
	public static final int OutLoop						=  6;
	public static final int OutRewind					=  7;
	public static final int OutForward					=  8;
	public static final int OutBack						=  9;
	public static final int OutPrint					= 10;
	public static final int OutSetVariable				= 11;
	public static final int OutSetProperty				= 12;
	public static final int OutExit						= 13;
	public static final int OutSetFocus					= 14;
	public static final int OutContinue					= 15;
	public static final int OutStopDebug				= 16;
	public static final int OutSetBreakpoints			= 17;
	public static final int OutRemoveBreakpoints		= 18;
	public static final int OutRemoveAllBreakpoints		= 19;
	public static final int OutStepOver					= 20;
	public static final int OutStepInto					= 21;
	public static final int OutStepOut					= 22;
	public static final int OutProcessedTag				= 23;
	public static final int OutSetSquelch				= 24;
	public static final int OutGetVariable				= 25;
	public static final int OutGetFrame					= 26;
	public static final int OutGetOption				= 27;
	public static final int OutSetOption				= 28;
	public static final int OutAddWatch					= 29; // 16-bit id; used for as2
	public static final int OutRemoveWatch				= 30; // 16-bit id; used for as2
    public static final int OutStepContinue				= 31;
    public static final int OutGetSwf				    = 32;
    public static final int OutGetSwd				    = 33;
	public static final int OutGetVariableWhichInvokesGetter = 34;
	public static final int OutGetBreakReason			= 35;
	public static final int OutGetActions				= 36;
	public static final int OutSetActions				= 37;
	public static final int OutSwfInfo					= 38;
	public static final int OutConstantPool				= 39;
    public static final int OutGetFncNames              = 40;
	// 41 through 47 are for profiling
    public static final int OutAddWatch2				= 49; // 32-bit id; used for as3
    public static final int OutRemoveWatch2				= 50; // 32-bit id; used for as3
    // If you add another message here, adjust the following line
    // and add a new case to the outTypeName() method below.
	public static final int OutSIZE						= OutGetFncNames + 1;	 /* last ID used +1 */

	/**
	 * Enums originally extracted from shared_tcserver/tcparser.h; these correspond
	 * to Flash player values that are currently in playerdebugger.h, class DebugAtomType.
	 */
	public static final int kNumberType			= 0;
	public static final int kBooleanType		= 1;
	public static final int kStringType			= 2;
	public static final int kObjectType			= 3;
	public static final int kMovieClipType		= 4;
	public static final int kNullType			= 5;
	public static final int kUndefinedType		= 6;
	public static final int kReferenceType		= 7;
	public static final int kArrayType			= 8;
	public static final int kObjectEndType		= 9;
	public static final int kStrictArrayType	= 10;
	public static final int kDateType			= 11;
	public static final int kLongStringType		= 12;
	public static final int kUnsupportedType	= 13;
	public static final int kRecordSetType		= 14;
	public static final int kXMLType			= 15;
	public static final int kTypedObjectType	= 16;
	public static final int kAvmPlusObjectType	= 17;
	public static final int kNamespaceType		= 18;
	public static final int kTraitsType			= 19;	// This one is special: When passed to the debugger, it indicates
														// that the "variable" is not a variable at all, but rather is a
														// class name.  For example, if class Y extends class X, then
														// we will send a kDTypeTraits for class Y; then we'll send all the
														// members of class Y; then we'll send a kDTypeTraits for class X;
														// and then we'll send all the members of class X.  This is only
														// used by the AVM+ debugger.

	/* byte array of our message and current index into it */
	byte[] m_content;	/* the data bytes of the message */
	int	   m_index;		/* current position within the content array */
	int    m_type;		/* one of OutXXX or InXXX integer constants */

	/* Debugging only: The contents of this message, formatted as a string for display */
	private StringBuffer m_debugFormatted;
	/* Debugging only: The number of bytes from the input that we have formatted into m_debugFormatted */
	private int m_debugFormattedThroughIndex;

	/* used by our cache to create empty DMessages */
	public DMessage(int size)
	{
		m_content = new byte[size];
		m_debugFormatted = new StringBuffer();
		m_debugFormattedThroughIndex = 0;
		clear();
	}

	/* getters/setters */
	public int    getType()				{ return m_type; }
	public String getInTypeName()		{ return inTypeName(getType()); }
	public String getOutTypeName()		{ return outTypeName(getType()); }
	public byte[] getData()				{ return m_content; }
	public int    getSize()				{ return (m_content == null) ? 0 : m_content.length; }
	public int    getRemaining()		{ return getSize()-m_index; }
	public int    getPosition()			{ return m_index; }
	public void   setType(int t)		{ m_type = t; }

	/**
	 * Allow the message to be 're-parsed' by someone else
	 */
	public void reset()
	{
		m_index = 0;
	}

	/**
	 * Allow the message to be reused later
	 */
	public void clear()
	{
		setType(-1);
		m_debugFormatted.setLength(0);
		m_debugFormattedThroughIndex = 0;
		reset();
	}

	/**
	 * Extract the next byte
	 */
	public int getByte() throws ArrayIndexOutOfBoundsException
	{
		if (m_index+1 > m_content.length)
			throw new ArrayIndexOutOfBoundsException(m_content.length-m_index+" < 1"); //$NON-NLS-1$

		int value = m_content[m_index++];
		debugAppendNumber(value, 1);
		return value;
	}

 	/**
	 * Extract the next 2 bytes, which form a 16b integer, from the message
	 */
	public int getWord() throws ArrayIndexOutOfBoundsException
	{
		if (m_index+2 > m_content.length)
			throw new ArrayIndexOutOfBoundsException(m_content.length-m_index+" < 2"); //$NON-NLS-1$

		int b0 = m_content[m_index++];
		int b1 = m_content[m_index++];

		int value  = ((b1 << 8) & 0xff00) | (b0 & 0xff);
		debugAppendNumber(value, 2);
		return value;
	}

	/**
	 * Extract the next 4 bytes, which form a 32b integer, from the message
	 */
	public long getDWord() throws ArrayIndexOutOfBoundsException
	{
		if (m_index+4 > m_content.length)
			throw new ArrayIndexOutOfBoundsException(m_content.length-m_index+" < 4"); //$NON-NLS-1$

		int b0 = m_content[m_index++];
		int b1 = m_content[m_index++];
		int b2 = m_content[m_index++];
		int b3 = m_content[m_index++];

		long value = ((b3 << 24) & 0xff000000) | ((b2 << 16) & 0xff0000) | ((b1 << 8) & 0xff00) | (b0 & 0xff);
		debugAppendNumber(value, 4);
		return value;
	}

	/**
	 * Heart wrenchingly slow but since we don't have a length so we can't
	 * do much better
	 */
	public String getString() throws ArrayIndexOutOfBoundsException
	{
		int startAt = m_index;
		boolean done = false;

		/* scan looking for a terminating null */
		while(!done)
		{
		    int ch = m_content[m_index++];
			if (ch == 0)
				done = true;
			else if (m_index > m_content.length)
				throw new ArrayIndexOutOfBoundsException("no string terminator found @"+m_index); //$NON-NLS-1$
		}

		/* build a new string and return it */
		String s;
		try
		{
			// The player I believe uses UTF-8?
			s = new String(m_content, startAt, m_index-startAt-1, "UTF-8"); //$NON-NLS-1$
		}
		catch(UnsupportedEncodingException uee)
		{
			// couldn't convert so let's try the default
			s = new String(m_content, startAt, m_index-startAt-1);
		}
		debugAppendString(s);
		return s;
	}

	/**
	 * Append a byte to the end of the message
	 */
	public void putByte(byte b0) throws ArrayIndexOutOfBoundsException
	{
		if (m_index+1 > m_content.length)
			throw new ArrayIndexOutOfBoundsException(m_content.length-m_index+" < 1"); //$NON-NLS-1$

		m_content[m_index++] = b0;
		debugAppendNumber(b0, 1);
	}

	/**
	 * Append 2 bytes, which form a 16b integer, into the message
	 */
	public void putWord(int val) throws ArrayIndexOutOfBoundsException
	{
		if (m_index+2 > m_content.length)
			throw new ArrayIndexOutOfBoundsException(m_content.length-m_index+" < 2"); //$NON-NLS-1$

		m_content[m_index++] = (byte)(val);
		m_content[m_index++] = (byte)(val >> 8);

		debugAppendNumber(val, 2);
	}

	/**
	 * Append 4 bytes, which form a 32b integer, into the message
	 */
	public void putDWord(long val) throws ArrayIndexOutOfBoundsException
	{
		if (m_index+4 > m_content.length)
			throw new ArrayIndexOutOfBoundsException(m_content.length-m_index+" < 4"); //$NON-NLS-1$

		m_content[m_index++] = (byte)(val);
		m_content[m_index++] = (byte)(val >> 8);
		m_content[m_index++] = (byte)(val >> 16);
		m_content[m_index++] = (byte)(val >> 24);

		debugAppendNumber(val, 4);
	}

	/**
	 * Helper to get the number of bytes that a string will need when it is sent
	 * across the socket to the Flash player.  Do *not* use string.getLength(),
	 * because that will return an incorrect result for strings that have non-
	 * ASCII characters.
	 */
	public static int getStringLength(String s)
	{
		try
		{
			return s.getBytes("UTF-8").length; //$NON-NLS-1$
		}
		catch (UnsupportedEncodingException e)
		{
			if (Trace.error) Trace.trace(e.toString());
			return 0;
		}
	}

	/**
	 * Place a string into the message (using UTF-8 encoding)
	 */
	public void putString(String s) throws ArrayIndexOutOfBoundsException, UnsupportedEncodingException
	{
		/* convert the string into a byte array */
		byte[] bytes = s.getBytes("UTF-8"); //$NON-NLS-1$
		int length = bytes.length;
		int endAt = m_index + length + 1;

		if (endAt > m_content.length)
			throw new ArrayIndexOutOfBoundsException(endAt+" > "+m_content.length); //$NON-NLS-1$

		/* copy the string as a byte array */
		System.arraycopy(bytes, 0, m_content, m_index, length);
		m_index += length;

		/* now the null terminator */
		m_content[m_index++] = '\0';
		
		debugAppendString(s);
	}

	// Debugging helper function: we've parsed a number out of the stream of input bytes,
	// so record that as a hex number of the appropriate length, e.g. "0x12" or "0x1234"
	// or "0x12345678", depending on numBytes.
	private void debugAppendNumber(long value, int numBytes)
	{
		if (PlayerSession.m_debugMsgOn || PlayerSession.m_debugMsgFileOn)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("0x"); //$NON-NLS-1$
			FieldFormat.formatLongToHex(sb, value, numBytes * 2, true);
			debugAppend(sb.toString());
		}
	}

	// Debugging helper function: we've parsed a string out of the stream of input bytes,
	// so record it as a quoted string in the formatted debugging output.
	private void debugAppendString(String s)
	{
		if (PlayerSession.m_debugMsgOn || PlayerSession.m_debugMsgFileOn)
			debugAppend('"' + s + '"');
	}

	// Debugging helper function: append a string to the formatted debugging output.
	private void debugAppend(String s)
	{
		if (PlayerSession.m_debugMsgOn || PlayerSession.m_debugMsgFileOn)
		{
			if (m_index > m_debugFormattedThroughIndex)
			{
				m_debugFormattedThroughIndex = m_index;
				if (m_debugFormatted.length() > 0)
					m_debugFormatted.append(' ');
				m_debugFormatted.append(s);
			}
		}
	}

	public String inToString() { return inToString(16); }

	public String inToString(int maxContentBytes)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getInTypeName());
		sb.append('[');
		sb.append(getSize());
		sb.append("] "); //$NON-NLS-1$
		if (getSize() > 0)
			appendContent(sb, maxContentBytes);

		return sb.toString();
	}

	public String outToString() { return outToString(16); }

	public String outToString(int maxContentBytes)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getOutTypeName());
		sb.append('[');
		sb.append(getSize());
		sb.append("] "); //$NON-NLS-1$
		if (getSize() > 0)
			appendContent(sb, maxContentBytes);

		return sb.toString();
	}

	public StringBuffer appendContent(StringBuffer sb, int max)
	{
		int size = getSize();
		byte[] data = getData();
		int i = 0;

		// First, output formatted content -- content for which some of the other functions
		// in this class, such as getDWord and getString, did formatting.
		sb.append(m_debugFormatted);

		// Now, for any left-over bytes which no one bothered to parse, output them as hex. 
		for(i=0; i<max && i+m_debugFormattedThroughIndex<size; i++)
		{
			int v = data[i+m_debugFormattedThroughIndex] & 0xff;
			sb.append(" 0x"); //$NON-NLS-1$
			FieldFormat.formatLongToHex(sb, v, 2, true);
		}

		if (i+m_debugFormattedThroughIndex < size)
			sb.append(" ..."); //$NON-NLS-1$

		return sb;
	}

	/**
	 * Convenience function for converting a type into a name used mainly for debugging
	 * but can also be used during trace facility of command line session
	 */
	public static String inTypeName(int type)
	{
        String s = "InUnknown(" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$

		switch(type)
		{
			case InSetMenuState:
				s = "InSetMenuState"; //$NON-NLS-1$
				break;

			case InSetProperty:
				s = "InSetProperty"; //$NON-NLS-1$
				break;

			case InExit:
				s = "InExit"; //$NON-NLS-1$
				break;

			case InNewObject:
				s = "InNewObject"; //$NON-NLS-1$
				break;

			case InRemoveObject:
				s = "InRemoveObject"; //$NON-NLS-1$
				break;

		    case InTrace:
				s = "InTrace"; //$NON-NLS-1$
				break;

			case InErrorTarget:
				s = "InErrorTarget"; //$NON-NLS-1$
				break;

			case InErrorExecLimit:
				s = "InErrorExecLimit"; //$NON-NLS-1$
				break;

			case InErrorWith:
				s = "InErrorWith"; //$NON-NLS-1$
				break;

			case InErrorProtoLimit:
				s = "InErrorProtoLimit"; //$NON-NLS-1$
				break;

			case InSetVariable:
				s = "InSetVariable"; //$NON-NLS-1$
				break;

			case InDeleteVariable:
				s = "InDeleteVariable"; //$NON-NLS-1$
				break;

			case InParam:
				s = "InParam"; //$NON-NLS-1$
				break;

			case InPlaceObject:
				s = "InPlaceObject"; //$NON-NLS-1$
				break;

			case InScript:
				s = "InScript"; //$NON-NLS-1$
				break;

			case InAskBreakpoints:
				s = "InAskBreakpoints"; //$NON-NLS-1$
				break;

			case InBreakAt:
				s = "InBreakAt"; //$NON-NLS-1$
				break;

			case InContinue:
				s = "InContinue"; //$NON-NLS-1$
				break;

			case InSetLocalVariables:
				s = "InSetLocalVariables"; //$NON-NLS-1$
				break;

			case InSetBreakpoint:
				s = "InSetBreakpoint"; //$NON-NLS-1$
				break;

			case InNumScript:
				s = "InNumScript"; //$NON-NLS-1$
				break;

			case InRemoveScript:
				s = "InRemoveScript"; //$NON-NLS-1$
				break;

			case InRemoveBreakpoint:
				s = "InRemoveBreakpoint"; //$NON-NLS-1$
				break;

			case InNotSynced:
				s = "InNotSynced"; //$NON-NLS-1$
				break;

			case InErrorURLOpen:
				s = "InErrorURLOpen"; //$NON-NLS-1$
				break;

			case InProcessTag:
				s = "InProcessTag"; //$NON-NLS-1$
				break;

			case InVersion:
				s = "InVersion"; //$NON-NLS-1$
				break;

			case InBreakAtExt:
				s = "InBreakAtExt"; //$NON-NLS-1$
				break;

			case InSetVariable2:
				s = "InSetVariable2"; //$NON-NLS-1$
				break;

			case InSquelch:
				s = "InSquelch"; //$NON-NLS-1$
				break;

			case InGetVariable:
				s = "InGetVariable"; //$NON-NLS-1$
				break;

			case InFrame:
				s = "InFrame"; //$NON-NLS-1$
				break;

			case InOption:
				s = "InOption"; //$NON-NLS-1$
				break;

			case InWatch:
				s = "InWatch"; //$NON-NLS-1$
				break;

			case InGetSwf:
				s = "InGetSwf"; //$NON-NLS-1$
				break;

			case InGetSwd:
				s = "InGetSwd"; //$NON-NLS-1$
				break;

			case InErrorException:
				s = "InErrorException"; //$NON-NLS-1$
				break;

			case InErrorStackUnderflow:
				s = "InErrorStackUnderflow"; //$NON-NLS-1$
				break;

			case InErrorZeroDivide:
				s = "InErrorZeroDivide"; //$NON-NLS-1$
				break;

			case InErrorScriptStuck:
				s = "InErrorScriptStuck"; //$NON-NLS-1$
				break;

			case InBreakReason:
				s = "InBreakReason"; //$NON-NLS-1$
				break;

			case InGetActions:
				s = "InGetActions"; //$NON-NLS-1$
				break;

			case InSwfInfo:
				s = "InSwfInfo"; //$NON-NLS-1$
				break;

			case InConstantPool:
				s = "InConstantPool"; //$NON-NLS-1$
				break;

			case InErrorConsole:
				s = "InErrorConsole"; //$NON-NLS-1$
				break;

            case InGetFncNames:
                s = "InGetFncNames"; //$NON-NLS-1$
                break;
		}
		return s;
	}

	/**
	 * Convenience function for converting a type into a name used mainly for debugging
	 * but can also be used during trace facility of command line session
	 */
	public static String outTypeName(int type)
	{
		String s = "OutUnknown(" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$

		switch(type)
		{
			case OutZoomIn:
				s = "OutZoomIn"; //$NON-NLS-1$
				break;

			case OutZoomOut:
				s = "OutZoomOut"; //$NON-NLS-1$
				break;

			case OutZoom100:
				s = "OutZoom100"; //$NON-NLS-1$
				break;

			case OutHome:
				s = "OutHome"; //$NON-NLS-1$
				break;

			case OutSetQuality:
				s = "OutSetQuality"; //$NON-NLS-1$
				break;

			case OutPlay:
				s = "OutPlay"; //$NON-NLS-1$
				break;

			case OutLoop:
				s = "OutLoop"; //$NON-NLS-1$
				break;

			case OutRewind:
				s = "OutRewind"; //$NON-NLS-1$
				break;

			case OutForward:
				s = "OutForward"; //$NON-NLS-1$
				break;

			case OutBack:
				s = "OutBack"; //$NON-NLS-1$
				break;

			case OutPrint:
				s = "OutPrint"; //$NON-NLS-1$
				break;

			case OutSetVariable:
				s = "OutSetVariable"; //$NON-NLS-1$
				break;

			case OutSetProperty:
				s = "OutSetProperty"; //$NON-NLS-1$
				break;

			case OutExit:
				s = "OutExit"; //$NON-NLS-1$
				break;

			case OutSetFocus:
				s = "OutSetFocus"; //$NON-NLS-1$
				break;

			case OutContinue:
				s = "OutContinue"; //$NON-NLS-1$
				break;

			case OutStopDebug:
				s = "OutStopDebug"; //$NON-NLS-1$
				break;

			case OutSetBreakpoints:
				s = "OutSetBreakpoints"; //$NON-NLS-1$
				break;

			case OutRemoveBreakpoints:
				s = "OutRemoveBreakpoints"; //$NON-NLS-1$
				break;

			case OutRemoveAllBreakpoints:
				s = "OutRemoveAllBreakpoints"; //$NON-NLS-1$
				break;

			case OutStepOver:
				s = "OutStepOver"; //$NON-NLS-1$
				break;

			case OutStepInto:
				s = "OutStepInto"; //$NON-NLS-1$
				break;

			case OutStepOut:
				s = "OutStepOut"; //$NON-NLS-1$
				break;

			case OutProcessedTag:
				s = "OutProcessedTag"; //$NON-NLS-1$
				break;

			case OutSetSquelch:
				s = "OutSetSquelch"; //$NON-NLS-1$
				break;

			case OutGetVariable:
				s = "OutGetVariable"; //$NON-NLS-1$
				break;

			case OutGetFrame:
				s = "OutGetFrame"; //$NON-NLS-1$
				break;

			case OutGetOption:
				s = "OutGetOption"; //$NON-NLS-1$
				break;

			case OutSetOption:
				s = "OutSetOption"; //$NON-NLS-1$
				break;

			case OutAddWatch:
				s = "OutAddWatch"; //$NON-NLS-1$
				break;

			case OutRemoveWatch:
				s = "OutRemoveWatch"; //$NON-NLS-1$
				break;

			case OutStepContinue:
				s = "OutStepContinue"; //$NON-NLS-1$
				break;

			case OutGetSwf:
				s = "OutGetSwf"; //$NON-NLS-1$
				break;

			case OutGetSwd:
				s = "OutGetSwd"; //$NON-NLS-1$
				break;

			case OutGetVariableWhichInvokesGetter:
				s = "OutGetVariableWhichInvokesGetter"; //$NON-NLS-1$
				break;

			case OutGetBreakReason:
				s = "OutGetBreakReason"; //$NON-NLS-1$
				break;

			case OutGetActions:
				s = "OutGetActions"; //$NON-NLS-1$
				break;

			case OutSetActions:
				s = "OutSetActions"; //$NON-NLS-1$
				break;

			case OutSwfInfo:
				s = "OutSwfInfo"; //$NON-NLS-1$
				break;

			case OutConstantPool:
				s = "OutConstantPool"; //$NON-NLS-1$
				break;

            case OutGetFncNames:
                s = "OutGetFncNames"; //$NON-NLS-1$
                break;

		}
   		return s;
	}
}
