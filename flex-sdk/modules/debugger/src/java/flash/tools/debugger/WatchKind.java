////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flash.tools.debugger;

/**
 * A descriptor for the type of watchpoint.
 * It may be one of three values; read, write or
 * both read and write.
 * @since Version 2
 */
public interface WatchKind
{
	/* kind of a watchpoint (one of) */
    public static final int NONE					=  0;
    public static final int READ					=  1;
    public static final int WRITE					=  2;
    public static final int READWRITE				=  3;
}
