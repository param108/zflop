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
public class Method
{
    public String name;
    public EventHandler faultHandler;
    public EventHandler resultHandler;
    public int concurrency;
    public Model myRequest;
    public String showBusyCursor;
    public boolean makeObjectsBindable;

    public Method(String name, String faultHandlerText, String resultHandlerText,
                  int concurrency, int xmlLineNumber, String showBusyCursor,
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
        this.showBusyCursor = showBusyCursor;
        this.makeObjectsBindable = makeObjectsBindable;
    }
}
