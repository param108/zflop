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

package flex2.compiler.css;

import flash.css.Descriptor;
import flash.css.Rule;
import flash.css.StyleDeclaration;
import flex2.compiler.Source;
import flex2.compiler.mxml.lang.FrameworkDefs;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.rep.AtEmbed;
import flex2.compiler.mxml.rep.MxmlDocument;
import flex2.compiler.util.CompilerMessage.CompilerError;
import flex2.compiler.util.ThreadLocalToolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.css.sac.LexicalUnit;

import macromedia.asc.util.ContextStatics;

public class StyleDef
{
    private String name;
    private boolean isTypeSelector;
    private Map atEmbeds = new HashMap();
    private Set imports = new HashSet();
    private Map styles = new HashMap();
    private List effectStyles = new ArrayList();

    private MxmlDocument mxmlDocument;
    private Source source;
    private int lineNumber;
    private ContextStatics perCompileData;

    private static final String CLASS_REFERENCE = "ClassReference(";
    private static final String EMBED = "Embed(";

    StyleDef(String name,
             boolean isTypeSelector,
             MxmlDocument mxmlDocument,
             Source source,
             int lineNumber,
             ContextStatics perCompileData)
    {
        this.name = name;
        this.isTypeSelector = isTypeSelector;
        this.mxmlDocument = mxmlDocument;
        this.source = source;
        this.lineNumber = lineNumber;
        this.perCompileData = perCompileData;
    }

    StyleDef(String name,
             boolean isTypeSelector,
             Source source,
             int lineNumber,
             ContextStatics perCompileData)
    {
        this.name = name;
        this.isTypeSelector = isTypeSelector;
        this.source = source;
        this.lineNumber = lineNumber;
        this.perCompileData = perCompileData;
    }

    /**
	 *
	 */
    void addRule(Rule rule)
    {
        StyleDeclaration declaration = rule.getStyle();

        Iterator propertyIterator = declaration.iterator();

        while ( propertyIterator.hasNext() )
        {
            String descriptorName = (String) propertyIterator.next();

            Descriptor descriptor = declaration.getPropertyValue(descriptorName);

            String propertyName = dehyphenize( descriptor.getName() );

            try
            {
                if (propertyName.equals("fontFamily"))
                {
                    processFontFamily(descriptor);
                }
                else
                {
                    processStyle(descriptor, propertyName);
                }
            }
            catch (CompilerError compilerError)
            {
            	compilerError.setPath(descriptor.getPath());
            	compilerError.setLine(descriptor.getLineNumber());
                ThreadLocalToolkit.log(compilerError);
            }
        }
    }

    /**
	 *
	 */
    public boolean isTypeSelector()
    {
        return isTypeSelector;
    }

	/**
     * This method is useful for converting CSS style declaration
     * names, like font-size, into valid ActionScript identifiers,
     * like fontSize.
     */
    public static String dehyphenize(String string)
    {
        StringBuffer stringBuffer = new StringBuffer();

        int start = 0;
        int end = string.indexOf('-');

        while (end >= 0)
        {
            stringBuffer.append( string.substring(start, end) );
            stringBuffer.append( Character.toUpperCase( string.charAt(end + 1) ) );
            start = end + 2;
            end = string.indexOf('-', start);
        }

        stringBuffer.append( string.substring(start) );

        return stringBuffer.toString();
    }

	private boolean addAtEmbed(AtEmbed atEmbed)
	{
        if (mxmlDocument != null)
        {
            mxmlDocument.addAtEmbed(atEmbed);
        }
        else if (!atEmbeds.containsKey(atEmbed.getPropName()))
        {
            atEmbeds.put(atEmbed.getPropName(), atEmbed);
        }

        return true;
	}

    public Collection getAtEmbeds()
    {
        return atEmbeds.values();
    }

    public List getEffectStyles()
    {
        return effectStyles;
    }

    public Set getImports()
    {
        return imports;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public String getName()
    {
        return name;
    }

    public Collection getStyles()
    {
        return styles.values();
    }

    public String getTypeName()
    {
        return dehyphenize(name);
    }

    private String processEmbed(String value, String styleSheetPath, int line)
    {
        String result = null;
        AtEmbed atEmbed = AtEmbed.create(perCompileData, source, value,
                                         styleSheetPath, line, "_embed_css_");

        if (atEmbed != null)
        {
            addAtEmbed(atEmbed);
            result = atEmbed.getPropName();
        }

        return result;
    }

    private String processClassReference(String value, String styleSheetPath, int line)
    {
        String result = null;
        String parameter = value.substring(CLASS_REFERENCE.length(), value.length() - 1).trim();

        if ((parameter.charAt(0) == '"') && (parameter.indexOf('"', 1) == parameter.length() - 1))
        {
            result = parameter.substring(1, parameter.length() - 1);
            imports.add(new Import(result, line));
        }
		else if (parameter.equals("null"))
		{
			result = parameter;
		}
        else
        {
            InvalidClassReference invalidClassReference = new InvalidClassReference();
            invalidClassReference.path = styleSheetPath;
            invalidClassReference.line = line;
            ThreadLocalToolkit.log(invalidClassReference);
        }

        return result;
    }

    private void processFontFamily(Descriptor descriptor)
    {
        String fontFamily = descriptor.getIdentAsString();
        StyleProperty stylesProperty = new StyleProperty("fontFamily",
                                                         "\"" + fontFamily + "\"",
                                                         descriptor.getLineNumber());
        styles.put(stylesProperty.getName(), stylesProperty);
    }

    private void processStyle(Descriptor descriptor, String propertyName)
        throws CompilerError
    {
		// We no longer put effect styles into a special array
		if ( propertyName.endsWith("Effect") )
        {
			effectStyles.add( propertyName );
		}
		
        String value = processStyle(descriptor);        

        if (value != null)
        {
            StyleProperty styleProperty = new StyleProperty(propertyName, value,
                                                            descriptor.getLineNumber());
            styles.put(styleProperty.getName(), styleProperty);
        }
    }

    private String processStyle(Descriptor descriptor)
        throws CompilerError
    {
		String value = descriptor.getValueAsString();

        if (value.startsWith(EMBED))
        {
            value = processEmbed(value, descriptor.getPath(), descriptor.getLineNumber());
        }
        else if (value.startsWith(CLASS_REFERENCE))
        {
            value = processClassReference(value, descriptor.getPath(), descriptor.getLineNumber());
        }

        // Strip the quotes for CSS identifiers, which are properties
        // of the Mxml document.  This allows us to support minimal
        // data binding functionality for the CSS object styling feature.
        if ((mxmlDocument != null) &&
            (descriptor.getValue().getLexicalUnitType() == LexicalUnit.SAC_IDENT) &&
            (value != null) && 
            (value.length() > 2) &&
            ((value.charAt(0) == '\'') || (value.charAt(0) == '\"')) &&
            ((value.charAt(value.length() - 1) == '\'') || (value.charAt(value.length() - 1) == '\"')))
        {
            String potentialProperty = value.substring(1, value.length() - 1);

            Type type = mxmlDocument.getRoot().getType();

            if (type != null && type.getProperty(potentialProperty) != null)
            {
                value = potentialProperty;
            }

            // deprecated - Flex 1.5 support only
            if ( FrameworkDefs.isBuiltinEffectName(potentialProperty) )
            {
                mxmlDocument.addTypeRef(FrameworkDefs.builtinEffectsPackage + '.' + potentialProperty, descriptor.getLineNumber());
            }
        }

        return value;
    }

    public static class InvalidClassReference extends CompilerError
    {
        public InvalidClassReference()
        {
        }
    }
}
