////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep;

import flex2.compiler.mxml.reflect.Type;

public class XML extends Model
{
    private String literalXML;
	private boolean isE4X;
    private boolean hasBindings;

    public XML(MxmlDocument document, Type t, Model parent, boolean isE4X, int line)
    {
        super(document, t, parent, line);
		this.literalXML = null;
		this.isE4X = isE4X;
    }

    public boolean hasBindings() {
        return hasBindings;
    }

    public void setHasBindings(boolean hasBindings) {
        this.hasBindings = hasBindings;
    }

    public String getLiteralXML() {
        return literalXML;
    }

    public void setLiteralXML(String literalXML) {
        this.literalXML = literalXML;
    }

	public boolean getIsE4X() {
		return isE4X;
	}
}
