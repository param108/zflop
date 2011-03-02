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

package flex2.compiler.i18n;

import java.util.HashSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;

import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;
import flex2.compiler.io.ResourceFile;
import flex2.compiler.util.MimeMappings;

/**
 * @author Brian Deitte
 */
public class PropertyTranslationFormat implements TranslationFormat
{
    private String[] mimeTypes;

    public PropertyTranslationFormat()
    {
        mimeTypes = new String[]{MimeMappings.PROPERTIES};
    }

    public boolean isSupported(String mimeType)
    {
        return mimeTypes[0].equals(mimeType);
    }

    public String[] getSupportedMimeTypes()
    {
        return mimeTypes;
    }

    public TranslationInfo getTranslationSet(CompilerConfiguration configuration, SymbolTable symbolTable, Source source, String locale)
            throws TranslationException
    {
        final PropertyText p = new PropertyText(configuration, symbolTable, source, locale);
	    Reader r = null;

        try
        {
        	InputStream in = getInputStream(source, locale);
        	if (in != null)
        	{
        		r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        		p.load(r);
        	}
        }
        catch (IOException ex)
        {
            throw new TranslationException(ex);
        }
        finally
        {
            if (r != null)
            {
                try
                {
                    r.close();
                }
                catch (IOException ex)
                {
                }
            }
        }
        
        return new TranslationInfo()
        {
			public Set getClassReferences()
			{
				return p.imports;
			}

			public Set getEmeds()
			{
				return new HashSet(p.atEmbeds.values());
			}

			public Set getTranslationSet()
			{
				return p.entrySet();
			}	
        };
    }
    
    private InputStream getInputStream(Source source, String locale) throws IOException
	{
		ResourceFile f = (ResourceFile) source.getBackingFile();
		f.setLocale(locale);
		return f.getInputStream();
	}
}
