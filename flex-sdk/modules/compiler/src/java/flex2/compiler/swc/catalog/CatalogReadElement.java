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

package flex2.compiler.swc.catalog;

/**
 * An element within catalog.xml.  This is used to provide context when reading the catalog.xml (that is, we
 * automatically know where to parse next).  See CatalogReader for its usage
 *
 * @author Brian Deitte
 */
public abstract class CatalogReadElement
{
    public abstract CatalogReadElement readElement(ReadContext context);

    public void endElement(ReadContext context)
    {
        // by default, don't do anything
    }
}
