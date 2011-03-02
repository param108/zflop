////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.rep;

import flex2.compiler.mxml.reflect.Type;

public class XMLList extends Model
{
    private String literalXML;
    private boolean hasBindings;

    public XMLList(MxmlDocument document, Type type, Model parent, int line)
    {
        super(document, type, parent, line);
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
}
