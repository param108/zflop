////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep;

import flex2.compiler.mxml.reflect.Event;
import flex2.compiler.mxml.reflect.Type;

/**
 * An EventHandler represents an occur
 * MxmlParser
 * MxmlDocument maintains
 */
public class EventHandler implements LineNumberMapped {
    private Model model;
	private Event event;
	private String eventHandlerText;
    private String documentFunctionName;

	public EventHandler(Model model, Event event, String eventHandlerText) {
		this.model = model;
		this.event = event;
		this.eventHandlerText = eventHandlerText;
	}

    public Model getModel() {
        return model;
    }

	public String getName() {
		return event.getName();
	}

	public Type getType() {
		return event.getType();
	}

	public String getEventHandlerText() {
		return eventHandlerText;
	}

    public String getDocumentFunctionName() {
        if (documentFunctionName == null)
        {
            StringBuffer buf = new StringBuffer();
            buf.append("__");
            buildNameChain(buf, model);
            buf.append("_");
            buf.append(event.getName());
            documentFunctionName = buf.toString();
        }
        return documentFunctionName;
    }

    private boolean buildNameChain(StringBuffer buf, Model mod)
    {
        if (mod != null)
        {
            if (mod.getId() != null)
            {
                buf.append(mod.getId());
            }
            else
            {
                boolean needSep = buildNameChain(buf, mod.getParent());
                if (needSep)
                {
                    buf.append("_");
                }
                assert mod.getParentIndex() != null;
                buf.append(mod.getParentIndex());
            }
            return true;
        }
        return false;
    }

	// Implement LineNumberMapped interface
	/**
	 * The line number where this model occurred in xml.  -1 if this model is synthetic
	 * and has no creation site in MXML.
	 */
	private int xmlLineNumber;

	public int getXmlLineNumber() {
		return xmlLineNumber;
	}

	public void setXmlLineNumber(int xmlLineNumber) {
		this.xmlLineNumber = xmlLineNumber;
	}

}
