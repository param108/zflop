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

import flex2.compiler.*;
import flex2.compiler.util.MimeMappings;
import flex2.compiler.util.QName;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.as3.reflect.MetaData;
import flex2.compiler.as3.reflect.NodeMagic;
import flex2.compiler.io.TextFile;
import flex2.compiler.util.MultiName;
import flex2.compiler.util.NameFormatter;
import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
import flash.swf.tools.as3.EvaluatorAdapter;
import flash.util.FileUtils;
import flash.util.Trace;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Evaluator that transcodes Embed resources, adds assets to the CompilationUnit, and turns variable level Embeds into
 * class level Embeds
 *
 * @author Paul Reilly
 * @author Brian Deitte
 */
class EmbedEvaluator extends EvaluatorAdapter
{
    private CompilationUnit unit;
    private String generatedOutputDir;
    private boolean checkDeprecation;
    private Transcoder[] transcoders;
    private Stack embedDataStack;
	private Set evaluatedClasses;
    private SymbolTable symbolTable;

    EmbedEvaluator(CompilationUnit unit, SymbolTable symbolTable, Transcoder[] transcoders,
                   String generatedOutputDir, boolean checkDeprecation)
    {
        this.unit = unit;
        this.symbolTable = symbolTable;
        this.generatedOutputDir = generatedOutputDir;
        this.transcoders = transcoders;
        this.checkDeprecation = checkDeprecation;
        embedDataStack = new Stack();
		evaluatedClasses = new HashSet();
	}

    private EmbedData getEmbedData()
    {
        EmbedData embedData = null;
        if (embedDataStack.size() != 0)
        {
            embedData = (EmbedData)embedDataStack.peek();
        }
        return embedData;
    }

    public Value evaluate(Context context, ClassDefinitionNode node)
    {
		if (!evaluatedClasses.contains(node))
		{
			evaluatedClasses.add(node);
			String packageName = NodeMagic.getPackageName(node);
			String className = node.name.name;

			EmbedData embedData = getEmbedData();
			if (embedData == null || embedData.inUse)
			{
				embedData = new EmbedData();
				embedDataStack.push(embedData);
			}
			embedData.inUse = true;
			embedData.referenceClassName = className;

			if (node.statements != null)
			{
				node.statements.evaluate(context, this);
			}

			if (embedData.hasData())
			{
				unit.addGeneratedSources( generateSources(packageName, context, node) );
				embedData.clear();
			}

			if (embedDataStack.size() > 0)
			{
				embedDataStack.pop();
			}
			embedData.inUse = false;
		}

		return null;
   }

    public Value evaluate(Context context, MetaDataNode node)
    {
        Node def = node.def;
        if ( "Embed".equals(node.id) )
        {
            if (def instanceof VariableDefinitionNode)
            {
                VariableDefinitionNode variableDefinition = (VariableDefinitionNode) def;

                if ((variableDefinition.list != null) &&
                    (variableDefinition.list.items != null) &&
                    (variableDefinition.list.items.size() > 0))
                {
                    Object item = variableDefinition.list.items.get(0);

                    if (item instanceof VariableBindingNode)
                    {
                        VariableBindingNode variableBinding = (VariableBindingNode) item;

                        if (variableBinding.initializer == null)
                        {
                            EmbedData embedData = getEmbedData();
                            if (embedData == null)
                            {
                                context.localizedError2(node.pos(), new EmbedOnlyOnClassesAndVars());
                                return null;
                            }
                            String name = variableBinding.variable.identifier.name;
                            String className = embedData.referenceClassName + "_" + name;
                            unit.expressions.add(new MultiName(NameFormatter.toColon(className)));
                            Map values = getMetaDataValues(node, context);

                            // Yeah, I feel dirty doing this, but I don't feel like making a separate map.
                            if (!values.containsKey( Transcoder.FILE ))
                            {
                            	if (context.input.origin.indexOf('\\') != -1)
                            	{
                            		values.put( Transcoder.FILE, context.input.origin.replace('\\', '/'));
                            		values.put( Transcoder.PATHSEP, "true");
                            	}
                            	else
                            	{
                            		values.put( Transcoder.FILE, context.input.origin );
                            	}
                                values.put( Transcoder.LINE, new Integer( context.input.getLnNum( node.pos() ) ).toString() );
                                values.put( Transcoder.COLUMN, new Integer( context.input.getColPos( node.pos() ) ).toString() );
                            }

                            getEmbedData().class2params.put( className, values );

                            String type = NodeMagic.lookupType( variableBinding );
                            boolean correctType = false;
                            if (type != null)
                            {
                                if (type.equals( "Class" ))
                                {
                                    correctType = true;
                                    variableBinding.initializer = generateClassInitializer( className );
                                }
                                else if (type.equals( "String" ))
                                {
                                    correctType = true;
                                    // FIXME- need way to introduce class dep from string embeds
                                    variableBinding.initializer = generateStringInitializer( className );
                                }
                            }

                            if (!correctType)
                            {
	                            context.localizedError2(node.pos(), new UnsupportedTypeForEmbed());
                            }

                        }
                        else
                        {
	                        context.localizedError2(node.pos(), new InvalidEmbedVariable());
                        }
                    }
                }
            }
            else if (def instanceof ClassDefinitionNode)
            {
                ClassDefinitionNode cdn = (ClassDefinitionNode)def;
                String pkg = NodeMagic.getPackageName(cdn);
                String cls = ((pkg == null) || (pkg.length() == 0)) ? cdn.name.name : pkg + "." + cdn.name.name;
                Map values = getMetaDataValues(node, context);

                // Yeah, I feel dirty doing this, but I don't feel like making a separate map.
                if (!values.containsKey( Transcoder.FILE ))
                {
                	if (context.input.origin.indexOf('\\') != -1)
                	{
                		values.put( Transcoder.FILE, context.input.origin.replace('\\', '/'));
                		values.put( Transcoder.PATHSEP, "true");
                	}
                	else
                	{
                		values.put( Transcoder.FILE, context.input.origin );
                	}
                    values.put( Transcoder.LINE, new Integer( context.input.getLnNum( node.pos() ) ) );
                    values.put( Transcoder.COLUMN, new Integer( context.input.getColPos( node.pos() ) ) );
                }

                Transcoder.TranscodingResults asset = EmbedUtil.transcode(transcoders, unit, symbolTable,
                                                                          cls, values,
                                                                          context.input.getLnNum( node.pos() ),
                                                                          context.input.getColPos( node.pos() ),
                                                                          false);

                if ((asset == null) && values.containsKey(Transcoder.SOURCE))
                {
                    context.localizedError2(node.pos(), new UnableToTranscode(values.get(Transcoder.SOURCE)));
                }

                // code below to should given a warning once we figure out non-AS2 way to call stop.  Since
                // this will most likely be a solution that's generated in a class, won't be supported on
                // classes.  Should also put this warning in MxmlDocument if it isn't moved to var level embeds
                //if (asset.defineTag instanceof DefineSprite && ((DefineSprite)asset.defineTag).needsStop)
                // {}

                // TODO: compare DefineTag/associatedClass against given class
            }
            else
            {
	            context.localizedError2(node.pos(), new EmbedOnlyOnClassesAndVars());
            }
        }

        return null;
    }

    private Map getMetaDataValues(MetaDataNode node, Context context)
    {
        MetaData metaData = new MetaData(node);
        int len = metaData.count();
        Map values = new HashMap();
        for (int i = 0; i < len; i++)
        {
            String key = metaData.getKey(i);
            String value = metaData.getValue(i);
            // FIXME: look for place where source is being added to generated Embeds remove the key.equals check
            if (key == null || key.equals(Transcoder.SOURCE))
            {
                int octothorpe = value.indexOf( "#" );
                if (octothorpe != -1)
                {
                    values.put(Transcoder.SOURCE, value.substring( 0, octothorpe ));
                    values.put(Transcoder.SYMBOL, value.substring( octothorpe + 1));
                }
                else
                {
                    values.put(Transcoder.SOURCE, value);
                }
            }
            else
            {
                values.put(key, value);
            }
        }
        
        if (checkDeprecation && (values.containsKey("flashType") || values.containsKey("flash-type")))
        {
        	String deprecated = (values.containsKey("flashType")) ? "flashType" : "flash-type";
        	String replacement = (values.containsKey("flashType")) ? "advancedAntiAliasing" : "advanced-anti-aliasing";
        	context.localizedError2(node.pos(), new DeprecatedAttribute(deprecated, replacement, "3.0"));
        }
        
        return values;
    }

    public Value evaluate(Context context, ProgramNode node)
    {
        embedDataStack = new Stack();

        super.evaluate(context, node);

        embedDataStack = null;

        return null;
    }

    private Source generateSource(String packageName, String className, Map embedMap, Context cx, Node node )
    {
        Source result = null;
        int line = embedMap.containsKey( Transcoder.LINE ) ? (Integer.parseInt( embedMap.get( Transcoder.LINE ).toString() )) : -1;
        int col = embedMap.containsKey( Transcoder.COLUMN ) ? (Integer.parseInt( embedMap.get( Transcoder.COLUMN ).toString() )) : -1;
        String path = embedMap.containsKey( Transcoder.FILE ) ? (String) embedMap.get( Transcoder.FILE ) : "";
        String pathSep = embedMap.containsKey( Transcoder.PATHSEP ) ? (String) embedMap.get( Transcoder.PATHSEP ) : null;
        if ("true".equals(pathSep))
        {
        	path = path.replace('/', '\\');
        }

        String packagePrefix = packageName == null || packageName.equals( "" ) ? "" : packageName + ".";
        String nameForReporting = path != null ? path : unit.getSource().getNameForReporting();

        try
        {
            Transcoder.TranscodingResults asset = EmbedUtil.transcode(transcoders, unit, symbolTable,
                                                                      packagePrefix + className,
                                                                      embedMap, line, col, true);

            if (asset != null)
            {
                String generatedName = (packagePrefix + className).replace( '.', File.separatorChar ) + ".as";

                if (generatedOutputDir != null)
                {
                    try
                    {
                        FileUtils.writeClassToFile(generatedOutputDir, packagePrefix, className + ".as", asset.generatedCode);
                    }
                    catch(IOException ioe)
                    {
                        if (Trace.error)
                        {
                            ioe.printStackTrace();
                        }
                        cx.localizedError(nameForReporting, line, col, ioe.getLocalizedMessage(), "");
                    }
                }

                // timestamp of this compiler-generated Source should match the asset file timestamp
                String relative = "";
                if (packageName != null)
                {
                    relative = packageName.replace( '.', '/' );
                }
                result = new Source(new TextFile(asset.generatedCode, generatedName, null, MimeMappings.AS,
                                                asset.assetSource != null ? asset.assetSource.getLastModified() : -1),
                                   relative, className, null, false, false, false);
                result.setAssetInfo(unit.getAssets().get(className));
                result.setPathResolver(unit.getSource().getPathResolver());
            }
    	    else if (embedMap.containsKey(Transcoder.SOURCE))
    	    {
    		    Object what = embedMap.get(Transcoder.SOURCE);
    		    cx.localizedError2(nameForReporting, line, col, new UnableToTranscode(what), "");
    	    }
        }
        catch (Exception e)
        {
            if (Trace.error)
            {
                e.printStackTrace();
            }
            cx.localizedError2(nameForReporting, line, col, new UnableToCreateSource(packagePrefix + className), null);
        }

        return result;
    }

    private Map generateSources( String packageName, Context cx, Node node)
    {
        Map sources = new HashMap();
        EmbedData embedData = getEmbedData();

        for (Iterator iterator = embedData.class2params.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry e = (Map.Entry) iterator.next();
            String className = (String) e.getKey();
            Map params = (Map) e.getValue();

            Source source = generateSource( packageName, className, params, cx, node);
            if (source != null)
                sources.put( new QName(packageName, className), source );
        }
        return sources;
    }

    private MemberExpressionNode generateClassInitializer(String name)
    {
        IdentifierNode identifier = new IdentifierNode(name, 0);

        GetExpressionNode getExpression = new GetExpressionNode(identifier);

        getExpression.pos(0);

        MemberExpressionNode result = new MemberExpressionNode(null, getExpression, 0);

        return result;
    }

    private LiteralStringNode generateStringInitializer(String name)
    {
        LiteralStringNode literalString = new LiteralStringNode(name);

        literalString.pos(0);

        return literalString;
    }

    // EmbedData holds all embeds for a given class
    class EmbedData
    {
        public String referenceClassName;
        public Map class2params = new HashMap();    // unique prefix -> map of embed params
        public boolean inUse;

        public void clear()
        {
            if (hasData())
            {
                class2params = new HashMap();
            }
        }

        public boolean hasData()
        {
            return (class2params.size() != 0);
        }
    }

	// error messages

	public static class UnableToTranscode extends CompilerMessage.CompilerError
	{
		public UnableToTranscode(Object what)
		{
			super();
			this.what = what;
		}

		public final Object what;
	}

	public static class UnableToCreateSource extends CompilerMessage.CompilerError
	{
		public UnableToCreateSource(String name)
		{
			super();
			this.name = name;
		}

		public final String name;
	}

	public static class UnsupportedTypeForEmbed extends CompilerMessage.CompilerError
	{
		public UnsupportedTypeForEmbed()
		{
			super();
		}
	}

	public static class InvalidEmbedVariable extends CompilerMessage.CompilerError
	{
		public InvalidEmbedVariable()
		{
			super();
		}
	}

	public static class EmbedOnlyOnClassesAndVars extends CompilerMessage.CompilerError
	{
		public EmbedOnlyOnClassesAndVars()
		{
			super();
		}
	}
	
	public static class DeprecatedAttribute extends CompilerMessage.CompilerWarning
	{
		public DeprecatedAttribute(String deprecated, String replacement, String since)
		{
			this.deprecated = deprecated;
			this.replacement = replacement;
			this.since = since;
		}
		
		public final String deprecated, replacement, since;
	}
}
