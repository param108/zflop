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

package flex2.compiler.css;

import flash.css.FontFaceRule;
import flash.css.Rule;
import flash.css.StyleDeclaration;
import flash.util.Trace;
import flex2.compiler.Source;
import flex2.compiler.Transcoder;
import flex2.compiler.mxml.rep.AtEmbed;
import flex2.compiler.util.DualModeLineNumberMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import macromedia.asc.util.ContextStatics;

public class StyleModule
{
    private String name;
    private List fontFaceRules;
    private Map styleDefs;
    private Source source;
    private ContextStatics perCompileData;
    private Map atEmbeds = new HashMap();
    private DualModeLineNumberMap lineNumberMap;

    public StyleModule(String name, Source source, ContextStatics perCompileData)
    {
        this.name = name;
        this.source = source;
        this.perCompileData = perCompileData;
        fontFaceRules = new ArrayList();
        styleDefs = new HashMap();
    }

    public void addFontFaceRule(FontFaceRule fontFaceRule)
    {
        assert fontFaceRule != null;

        String family = fontFaceRule.getFamily();
        boolean bold = fontFaceRule.isBold();
        boolean italic = fontFaceRule.isItalic();

        if (FontFaceRule.getRule(fontFaceRules, family, bold, italic) == null)
        {
            fontFaceRules.add(fontFaceRule);

            //    add embed for font
            String propName = "_embed__font_" + family + "_" + (bold? "bold":"medium") + "_" + (italic? "italic":"normal");
            Map embedParams = fontFaceRule.getEmbedParams();
            StyleDeclaration styleDeclaration = fontFaceRule.getStyle();
            String path = styleDeclaration.getPath();

            if (path.indexOf('\\') > -1)
            {
                embedParams.put( Transcoder.FILE, path.replace('\\', '/') );
                embedParams.put( Transcoder.PATHSEP, "true" );
            }
            else
            {
                embedParams.put( Transcoder.FILE, path );            
            }

            embedParams.put( Transcoder.LINE, Integer.toString(styleDeclaration.getLineNumber()) );
            AtEmbed atEmbed = AtEmbed.create(propName, fontFaceRule.getStyle().getLineNumber(), embedParams, false);
            atEmbeds.put(atEmbed.getPropName(), atEmbed);
        }
        else if (Trace.font)
        {
            Trace.trace("Font face already existed for " + family + " bold? " + bold + " italic? " + italic);
        }
    }

    public void addSelector(String name, boolean isTypeSelector, Rule rule, int lineNumber)
    {
        StyleDef styleDef = (StyleDef) styleDefs.get(name);

        if (styleDef == null)
        {
            styleDef = new StyleDef(name, isTypeSelector, source, lineNumber, perCompileData);
            styleDefs.put(name, styleDef);
        }

        styleDef.addRule(rule);
    }

    public Collection getAtEmbeds()
    {
        Iterator styleDefIterator = styleDefs.values().iterator();

        while ( styleDefIterator.hasNext() )
        {
            StyleDef styleDef = (StyleDef) styleDefIterator.next();

            Iterator atEmbedIterator = styleDef.getAtEmbeds().iterator();

            while ( atEmbedIterator.hasNext() )
            {
                AtEmbed atEmbed = (AtEmbed) atEmbedIterator.next();

                if (!atEmbeds.containsKey(atEmbed.getPropName()))
                {
                    atEmbeds.put(atEmbed.getPropName(), atEmbed);
                }
            }
        }

        return atEmbeds.values();
    }

    public List getFontFaceRules()
    {
        return fontFaceRules;
    }

    public Set getImports()
    {
        Set result = new HashSet();
        Iterator styleDefIterator = styleDefs.values().iterator();

        while ( styleDefIterator.hasNext() )
        {
            StyleDef styleDef = (StyleDef) styleDefIterator.next();
            result.addAll(styleDef.getImports());
        }

        return result;
    }

    public DualModeLineNumberMap getLineNumberMap()
    {
        return lineNumberMap;
    }

    public String getName()
    {
        return name;
    }

    public Source getSource()
    {
        return source;
    }

    public Collection getStyleDefs()
    {
        return styleDefs.values();
    }

    public void setLineNumberMap(DualModeLineNumberMap lineNumberMap)
    {
        this.lineNumberMap = lineNumberMap;
    }
}
