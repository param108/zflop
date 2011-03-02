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

package flex2.compiler.common;

import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.config.ConfigurationInfo;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Date;
import java.text.DateFormat;

/**
 * @author Roger Gonzalez
 */
public class MetadataConfiguration
{
    public boolean isSet()
    {
        return ((localizedTitles.size() > 0) || (localizedDescriptions.size() > 0) ||
		        (publishers.size() > 0) || (creators.size() > 0) || (contributors.size() > 0) || (langs.size() > 0) ||
		        (date != null));
    }
    
    private static String f( String in )
    {
        in = in.trim();
        return ((in.indexOf('<') == -1) && (in.indexOf( '>') == -1))? in : ("<![CDATA[" + in + "]]>");
    }
    
    public String toString()
    {
	    if (!isSet())
	    {
		    return null;
	    }

        StringBuffer sb = new StringBuffer();
	    sb.append("<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>");
	    sb.append("<rdf:Description rdf:about='' xmlns:dc='http://purl.org/dc/elements/1.1'>");
	    sb.append("<dc:format>application/x-shockwave-flash</dc:format>");

        if (localizedTitles.size() > 0)
        {
            if ((localizedTitles.size() == 1) && (localizedTitles.get("x-default") != null))
            {
                sb.append("<dc:title>" + f((String)localizedTitles.get("x-default")) + "</dc:title>");
            }
            else
            {
                sb.append("<dc:title><rdf:Alt>");
                for (Iterator it = localizedTitles.entrySet().iterator(); it.hasNext();)
                {
                    Map.Entry e = (Map.Entry) it.next();
                    sb.append("<rdf:li xml:lang='" + e.getKey() + "'>" + f((String) e.getValue()) + "</rdf:li>");
                }
                sb.append("</rdf:Alt></dc:title>");
            }
        }
        if (localizedDescriptions.size() > 0)
        {
            if ((localizedDescriptions.size() == 1) && (localizedDescriptions.get("x-default") != null))
            {
                sb.append("<dc:description>" + f((String) localizedDescriptions.get("x-default")) + "</dc:description>");
            }
            else
            {
                sb.append("<dc:description><rdf:Alt>");
                for (Iterator it = localizedDescriptions.entrySet().iterator(); it.hasNext();)
                {
                    Map.Entry e = (Map.Entry) it.next();
                    sb.append("<rdf:li xml:lang='" + (String) e.getKey() + "'>" + f((String) e.getValue()) + "</rdf:li>");
                }
                sb.append("</rdf:Alt></dc:description>");
            }
        }
        // FIXME - I suspect we need rdf:Bag for these when there are more than one?  --rg

        for (Iterator it = publishers.iterator(); it.hasNext();)
        {
            sb.append("<dc:publisher>" + f((String) it.next()) + "</dc:publisher>");
        }

        for (Iterator it = creators.iterator(); it.hasNext();)
        {
            sb.append("<dc:creator>" + f((String) it.next()) + "</dc:creator>");
        }

        for (Iterator it = contributors.iterator(); it.hasNext();)
        {
            sb.append("<dc:contributor>" + f((String) it.next()) + "</dc:contributor>");
        }

        for (Iterator it = langs.iterator(); it.hasNext();)
        {
            sb.append("<dc:language>" + f((String) it.next()) + "</dc:language>");
        }

	    if (date == null)
	    {
		    date = DateFormat.getDateInstance().format(new Date());
	    }

        sb.append("<dc:date>" + f(date) + "</dc:date>");
        sb.append("</rdf:Description></rdf:RDF>");
        return sb.toString();
    }

    //
    // 'metadata.contributor' option
    //
 
    private Set contributors = new TreeSet();

    public void cfgContributor( ConfigurationValue cv, String name )
    {
        contributors.add( name );
    }

    public static ConfigurationInfo getContributorInfo()
    {
        return new ConfigurationInfo( 1, "name" )
        {
            public boolean isAdvanced()
            {
                return false;
            }

            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
    // 'metadata.creator' option
    //
 
    private Set creators = new TreeSet();

    public void cfgCreator( ConfigurationValue cv, String name )
    {
        creators.add( name );
    }

    public static ConfigurationInfo getCreatorInfo()
    {
        return new ConfigurationInfo( 1, "name" )
        {
            public boolean isAdvanced()
            {
                return false;
            }

            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
    // 'metadata.date' option
    //
 
    public String date = null;
    
    public void cfgDate( ConfigurationValue cv, String text)
    {
        date = text;
    }

    public static ConfigurationInfo getDateInfo()
    {
        return new ConfigurationInfo( 1, "text" )
        {
            public boolean isAdvanced()
            {
                return false;
            }
        };
    }

    //
    // 'metadata.description' option
    //
 
    private Map localizedDescriptions = new HashMap();

    public void cfgDescription( ConfigurationValue cv, String text )
    {
        localizedDescriptions.put( "x-default", text );
    }

    public static ConfigurationInfo getDescriptionInfo()
    {
        return new ConfigurationInfo( 1, "text" )
        {
            public boolean isAdvanced()
            {
                return false;
            }
        };
    }

    //
    // 'metadata.language' option
    //
 
    public TreeSet langs = new TreeSet();

    public void cfgLanguage( ConfigurationValue cv, String code )
    {
        langs.add( code );
    }

    public static ConfigurationInfo getLanguageInfo()
    {
        return new ConfigurationInfo( 1, "code" )
        {
            public boolean isAdvanced()
            {
                return false;
            }

            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
    // 'metadata.localized-description' option
    //
 
    public void cfgLocalizedDescription( ConfigurationValue cv, String text, String lang )
    {
        localizedDescriptions.put( lang, text );
    }

    public static ConfigurationInfo getLocalizedDescriptionInfo()
    {
        return new ConfigurationInfo( 2, new String[] {"text", "lang"} )
        {
            public boolean isAdvanced()
            {               
			    return false;
            }
            
            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
    // 'metadata.localized-title' option
    //
    
   public void cfgLocalizedTitle( ConfigurationValue cv, String title, String lang )
    {
        localizedTitles.put( lang, title );
    }

    public static ConfigurationInfo getLocalizedTitleInfo()
    {
        return new ConfigurationInfo( 2, new String[] {"title", "lang"} )
        {
            public boolean isAdvanced()
            {
                return false;
            }

            public boolean allowMultiple()
            {
                return true;
            }
        };
    }

    //
    // 'metadata.publisher' option
    //
 
    private Set publishers = new TreeSet();

    public void cfgPublisher( ConfigurationValue cv, String name )
    {
        publishers.add( name );
    }

    public static ConfigurationInfo getPublisherInfo()
    {
        return new ConfigurationInfo( 1, "name" )
        {
            public boolean isAdvanced()
            {
                return false;
            }

            public boolean allowMultiple()
            {
                return true;
            }
        };
    }
    //
    // 'metadata.title' option
    //
    
    private Map localizedTitles = new HashMap();

    public void cfgTitle( ConfigurationValue cv, String title )
    {
        localizedTitles.put( "x-default", title );
    }

    public static ConfigurationInfo getTitleInfo()
    {
        return new ConfigurationInfo( 1, "text" )
        {
            public boolean isAdvanced()
            {
                return false;
            }
        };
    }
}
