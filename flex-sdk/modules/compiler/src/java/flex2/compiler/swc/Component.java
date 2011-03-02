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

package flex2.compiler.swc;

/**
 * A component within a SWC
 *
 * @author Brian Deitte
 */
public class Component implements flex2.tools.oem.Component
{
    private String className;
    private String name;
    private String uri;
    protected String icon;   // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    private String docs;
    protected String preview;   // changed from private to protected to support Flash Authoring - jkamerer 2007.07.30
    private String location;

    public Component()
    {
    }
    
    public Component(String className, String name, String uri)
    {
        this.className = className;
        this.name = name;
        this.uri = uri;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public String getDocs()
    {
        return docs;
    }

    public void setDocs(String docs)
    {
        this.docs = docs;
    }

    public String getPreview()
    {
        return preview;
    }

    public void setPreview(String preview)
    {
        this.preview = preview;
    }
    
    // C: note: 'location' is not an attribute of the <component> tag in catalog.xml.
    public void setLocation(String loc)
    {
    	this.location = loc;
    }

    // flex2.tools.oem.reflect.Component specific...
    // Do not use this method in the mxmlc/compc codepath.
	public String getLocation()
	{
		return location;
	}
}

