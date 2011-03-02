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

package flex2.compiler.util;

import java.util.*;

/**
 * Map MIME types to file extensions
 * 
 * @author Clement Wong
 */
public final class MimeMappings
{
	public static final String MXML = "text/mxml";
	public static final String AS = "text/as";
	public static final String ABC = "application/x-actionscript-bytecode";
	public static final String CSS = "text/css";
	public static final String PROPERTIES = "text/properties";
	public static final String JPEG = "image/jpeg";
	public static final String JPG = "image/jpg";
	public static final String PNG = "image/png";
	public static final String GIF = "image/gif";
	public static final String SVG = "image/svg";
	public static final String SVG_XML = "image/svg-xml";
	public static final String MP3 = "audio/mpeg";
	public static final String FLASH = "application/x-shockwave-flash";
	public static final String XML = "text/xml";
	public static final String TTF = "application/x-font-truetype";
	public static final String TTC = "application/x-font-truetype-collection";
	public static final String OTF = "application/x-font-opentype";
	public static final String FONT = "application/x-font";
	public static final String TIFF = "image/tiff";
	public static final String SKIN = "skin";
	
    public MimeMappings()
    {
    	mimeMappings = new HashMap();

    	set(MXML, ".mxml");
    	set(AS, ".as");
    	set(ABC, ".abc");
    	set(CSS, ".css");
    	set(PROPERTIES, ".properties");
    	set(JPEG, new String[] { ".jpg", ".jpeg" });
    	set(JPG, new String[] { ".jpg", ".jpeg" });
    	set(PNG, ".png");
    	set(GIF, ".gif");
    	set(SVG, new String[] { ".svg", ".svgz" });
    	set(MP3, ".mp3");
    	set(FLASH, ".swf");
    	set(XML, ".xml");
    	set(TTF, ".ttf");
    	set(TTC, ".ttc");
    	set(OTF, ".otf");
    	set(TIFF, new String[] { ".tiff", ".tif" });
    }
    
    private Map mimeMappings;
    
    /**
     * set file extensions. existing file extensions will be overriden.
     * 
     * @param mimeType
     * @param extensions
     */
    public void set(String mimeType, String[] extensions)
    {
    	mimeMappings.put(mimeType, extensions);
    }
        
    /**
     * set file extensions. existing file extensions will be overriden.
     * 
     * @param mimeType
     * @param extension
     */
    public void set(String mimeType, String extension)
    {
    	mimeMappings.put(mimeType, extension);
    }

    /**
     * add file extensions. keep the existing file extensions.
     * 
     * @param mimeType
     * @param extensions
     */
    public void add(String mimeType, String[] extensions)
    {
    	if (extensions == null)
    	{
    		return;
    	}
    	
    	Object value = mimeMappings.get(mimeType);
    	String[] a = null;
    	if (value instanceof String[])
    	{
    		String[] old = (String[]) value;
    		a = new String[old.length + extensions.length];
    		System.arraycopy(old, 0, a, 0, old.length);
    		System.arraycopy(extensions, 0, a, a.length, extensions.length);
    	}
    	else if (value instanceof String)
    	{
    		a = new String[1 + extensions.length];
    		a[0] = (String) value;
    		System.arraycopy(extensions, 0, a, 1, extensions.length);    		
    	}
    	else
    	{
    		a = new String[extensions.length];
    		System.arraycopy(extensions, 0, a, 0, extensions.length);
    	}
    	
    	mimeMappings.put(mimeType, a);
    }

    /**
     * add file extension. keep the existing file extensions.
     * 
     * @param mimeType
     * @param extensions
     */
    public void add(String mimeType, String extension)
    {
    	if (extension == null)
    	{
    		return;
    	}
    	
    	Object value = mimeMappings.get(mimeType);
    	String[] a = null;
    	if (value instanceof String[])
    	{
    		String[] old = (String[]) value;
    		a = new String[old.length + 1];
    		System.arraycopy(old, 0, a, 0, old.length);
    		a[a.length - 1] = extension;
        	
        	mimeMappings.put(mimeType, a);
    	}
    	else if (value instanceof String)
    	{
    		a = new String[2];
    		a[0] = (String) value;
    		a[1] = extension;    		
        	
        	mimeMappings.put(mimeType, a);
    	}
    	else
    	{
    		mimeMappings.put(mimeType, extension);
    	}
    }
    
    /**
     * remove file extensions.
     * 
     * @param mimeType
     */
    public void remove(String mimeType)
    {
    	mimeMappings.remove(mimeType);
    }

    /**
     * find a MIME type based on the specified name.
     * 
     * @param name
     * @return
     */
    public String findMimeType(String name)
    {
    	for (Iterator i = mimeMappings.keySet().iterator(); i.hasNext();)
    	{
    		String mimeType = (String) i.next();
    		Object value = mimeMappings.get(mimeType);
    		
    		if (value instanceof String[])
    		{
    			String[] extensions = (String[]) value;
    			for (int j = 0, size = extensions.length; j < size; j++)
    			{
    				int nlen = name.length();
    				int elen = extensions[j].length();
    				if (nlen > elen && name.regionMatches(true, nlen - elen, extensions[j], 0, elen))
    				{
    					return mimeType;
    				}
    			}
    		}
    		else if (value instanceof String)
    		{
				int nlen = name.length();
				int elen = ((String) value).length();
				if (nlen > elen && name.regionMatches(true, nlen - elen, (String) value, 0, elen))
				{
					return mimeType;
				}
    		}
    	}
    	
    	return null;
    }

    /**
     * find a file extension based on the specified MIME type.
     * 
     * @param mimeType
     * @return
     */
	public String findExtension(String mimeType)
	{
		Object value = mimeMappings.get(mimeType);
		if (value instanceof String[])
		{
			// C: should really return a list of extensions...
			return ((String[]) value)[0];
		}
		else if (value instanceof String)
		{
			return (String) value;
		}
		else
		{
			return null;
		}
	}

	// By default, the static methods use ThreacLocal. if the ThreadLocal isn't available,
	// the methods will use the static version...
	
	private static MimeMappings statics = new MimeMappings();
	
    public static String getMimeType(String name)
    {
    	MimeMappings mappings = ThreadLocalToolkit.getMimeMappings();
    	if (mappings == null)
    	{
        	synchronized(statics)
        	{
        		return statics.findMimeType(name);
        	}
    	}
    	else
    	{    		
    		return mappings.findMimeType(name);
    	}
    }
    
	public static String getExtension(String mimeType)
	{		
    	MimeMappings mappings = ThreadLocalToolkit.getMimeMappings();
    	if (mappings == null)
    	{
        	synchronized(statics)
        	{
        		return statics.findExtension(mimeType);
        	}
    	}
    	else
    	{
    		return mappings.findExtension(mimeType);
    	}
	}
}
