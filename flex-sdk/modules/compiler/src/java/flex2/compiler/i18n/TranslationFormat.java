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

import flex2.compiler.common.CompilerConfiguration;
import flex2.compiler.Source;
import flex2.compiler.SymbolTable;

/**
 * @author Brian Deitte
 */
public interface TranslationFormat
{
    /**
     * Let the compiler know whether the given mimeType is supported by this class
     */
    public boolean isSupported(String mimeType);

    /**
     * Returns the mimeTypes supported by this class.  The mimeType has to be known to the compiler.
     * Known types include MimeMappings.AS, MimeMappings.PROPERTIES, MimeMappings.MXML
     */
    public String[] getSupportedMimeTypes();

    /**
     * Process the given file and return a Set of Map.Entry values with String keys and values.
     */
    public TranslationInfo getTranslationSet(CompilerConfiguration configuration, SymbolTable symbolTable, Source source, String locale)
    	throws TranslationException;
}
