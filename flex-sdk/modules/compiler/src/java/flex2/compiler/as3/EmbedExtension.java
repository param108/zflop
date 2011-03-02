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

package flex2.compiler.as3;

import flex2.compiler.AssetInfo;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Transcoder;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.ThreadLocalToolkit;
import flex2.compiler.as3.reflect.TypeTable;
import macromedia.asc.parser.Node;
import flash.swf.tags.*;

import java.util.Iterator;
import java.util.Map;

/**
 * Compiler extension for Embed metadata
 *  
 * @author Paul Reilly
 */
public final class EmbedExtension implements Extension
{
    private Transcoder[] transcoders;
    private String generatedOutputDir;
    private boolean checkDeprecation;

    public EmbedExtension(Transcoder[] transcoders, String generatedOutputDir, boolean checkDeprecation)
    {
        this.generatedOutputDir = generatedOutputDir;
        this.transcoders = transcoders;
        this.checkDeprecation = checkDeprecation;
    }

    public void parse1(CompilationUnit unit, TypeTable typeTable)
    {
        if (unit.metadata.size() > 0)
        {
            Node node = (Node) unit.getSyntaxTree();
            flex2.compiler.Context context = unit.getContext();
            macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
            EmbedSkinClassEvaluator embedSkinClassEvaluator = new EmbedSkinClassEvaluator(unit);
            node.evaluate(cx, embedSkinClassEvaluator);
        }
    }
    
    public void parse2(CompilationUnit unit, TypeTable typeTable)
    {
        if (unit.metadata.size() > 0)
        {
            EmbedEvaluator embedEvaluator = new EmbedEvaluator(unit, typeTable.getSymbolTable(),
                                                               transcoders, generatedOutputDir,
                                                               checkDeprecation);
            embedEvaluator.setLocalizationManager(ThreadLocalToolkit.getLocalizationManager());
            Node node = (Node) unit.getSyntaxTree();
            flex2.compiler.Context context = unit.getContext();
            macromedia.asc.util.Context cx = (macromedia.asc.util.Context) context.getAttribute("cx");
            node.evaluate(cx, embedEvaluator);
        }
    }

	public void analyze1(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze2(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze3(CompilationUnit unit, TypeTable typeTable)
	{
	}

	public void analyze4(CompilationUnit unit, TypeTable typeTable)
	{
	}

    public void generate(CompilationUnit unit, TypeTable typeTable)
    {
        // make sure that symbol/class associations are sane
        for (Iterator ai = unit.getAssets().iterator(); ai.hasNext();)
        {
            Map.Entry e = (Map.Entry) ai.next();
            String className = (String) e.getKey();
            DefineTag defineTag = ((AssetInfo) e.getValue()).getDefineTag();
            flex2.compiler.abc.Class c = typeTable.getClass( className );

            if (c != null)
            {
                if ((c.getAttributes() == null) || (!c.getAttributes().hasPublic()))
                {
                    ThreadLocalToolkit.log( new NonPublicAssetClass( c.getName() ), unit.getSource().getNameForReporting() );
                }

                IncompatibleAssetClass incompatibleAssetClass = null;

                // todo - this emacs macro created nightmare should be refactored
                if ((defineTag instanceof DefineSprite) && !c.isSubclassOf( "flash.display:Sprite" ))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineSprite", "flash.display.Sprite" );
                }
                else if ((defineTag instanceof DefineBits) && !c.isSubclassOf( "flash.display:Bitmap" ))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineBits", "flash.display.Bitmap" );
                }
                else if ((defineTag instanceof DefineSound) && !c.isSubclassOf( "flash.media:Sound"))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineSound", "flash.media.Sound" );
                }
                else if ((defineTag instanceof DefineFont) && !c.isSubclassOf( "flash.text:Font"))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineFont", "flash.text.Font" );
                }
                else if ((defineTag instanceof DefineText) && !c.isSubclassOf( "flash.display:StaticText"))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineText", "flash.display.StaticText" );
                }
                else if ((defineTag instanceof DefineEditText) && !c.isSubclassOf( "flash.display:TextField"))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineEditText", "flash.display.TextField" );
                }
                else if ((defineTag instanceof DefineShape) && !c.isSubclassOf( "flash.display:Shape"))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineShape", "flash.display.Shape" );
                }
                else if ((defineTag instanceof DefineButton) && !c.isSubclassOf( "flash.display:SimpleButton"))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineButton", "flash.display.SimpleButton" );
                }
                else if ((defineTag instanceof DefineBinaryData) && !c.isSubclassOf( "flash.utils:ByteArray" ))
                {
                    incompatibleAssetClass = new IncompatibleAssetClass( c.getName(), "DefineBinaryData", "flash.utils.ByteArray" );
                }

                if (incompatibleAssetClass != null)
                {
                    ThreadLocalToolkit.log(incompatibleAssetClass, unit.getSource().getNameForReporting());
                }
            }
        }
    }

	public static class NonPublicAssetClass extends CompilerMessage.CompilerWarning
	{
	    public NonPublicAssetClass( String assetClass )
	    {
	        this.assetClass = assetClass;
	    }
	    public final String assetClass;
	}

	public static class IncompatibleAssetClass extends CompilerMessage.CompilerWarning
	{
	    public IncompatibleAssetClass( String assetClass, String assetType, String requiredBase )
	    {
	        super();
	        this.assetClass = assetClass;
	        this.assetType = assetType;
	        this.requiredBase = requiredBase;
	    }
	    public final String assetClass;
	    public final String assetType;
	    public final String requiredBase;
	}
}
