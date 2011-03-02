////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.mxml.dom;

import flex2.compiler.util.QName;

import java.util.HashSet;
import java.util.Set;

public class XMLListNode extends Node
{
    // static final Set<QName> attributes;
    public static final Set attributes;

    static
    {
        // attributes = new HashSet<QName>();
        attributes = new HashSet();
        attributes.add(new QName("", "id"));
    }


    XMLListNode(String uri, String localName)
    {
        this(uri, localName, 0);
    }

    XMLListNode(String uri, String localName, int size)
    {
        super(uri, localName, size);
    }
    
    public void analyze(Analyzer analyzer)
    {
        analyzer.prepare(this);
        analyzer.analyze(this);
    }

}
