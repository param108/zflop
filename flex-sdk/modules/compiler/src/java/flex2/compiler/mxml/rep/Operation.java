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

package flex2.compiler.mxml.rep;

/**
 */
public class Operation
{
    public static final int CONCURRENCY_MULTIPLE    = 0;
    public static final int CONCURRENCY_SINGLE      = 1;
    public static final int CONCURRENCY_LAST        = 2;

    public String name;
    public EventHandler faultHandler;
    public EventHandler resultHandler;
    public int concurrency;
    public String resultFormat;
    public Model myRequest;
    public String showBusyCursor;
    public boolean makeObjectsBindable;

    public Operation(String name, String faultHandlerText, String resultHandlerText,
                     int concurrency, String resultFormat, int xmlLineNumber, String showBusyCursor,
                     boolean makeObjectsBindable)
    {
        this.name = name;

        if (faultHandlerText != null)
        {
            faultHandler = new EventHandler(null, null, faultHandlerText);
            faultHandler.setXmlLineNumber(xmlLineNumber);
        }

        if (resultHandlerText != null)
        {
            resultHandler = new EventHandler(null, null, resultHandlerText);
            resultHandler.setXmlLineNumber(xmlLineNumber);
        }

        this.concurrency = concurrency;
        this.resultFormat = resultFormat;
        this.showBusyCursor = showBusyCursor;
        this.makeObjectsBindable = makeObjectsBindable;
    }
}
