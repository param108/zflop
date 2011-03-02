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

package flex2.linker;

import flash.swf.Frame;
import flash.swf.Movie;
import flash.swf.tags.*;
import flash.swf.types.FlashUUID;
import flash.swf.types.Rect;
import flash.util.Trace;
import flex2.compiler.AssetInfo;
import flex2.compiler.CompilationUnit;
import flex2.compiler.DependencyGraph;
import flex2.compiler.Source;
import flex2.compiler.util.*;
import flex2.tools.VersionInfo;
import flex2.tools.PreLink;

import java.util.*;

/**
 * @author Clement Wong
 */
public class SimpleMovie extends Movie
{
	public SimpleMovie(Configuration configuration)
    {
        if (configuration.width() != null)
	    {
		    try
		    {
			    width = Integer.parseInt(configuration.width());
		    }
		    catch(NumberFormatException nfe)
		    {
			    ThreadLocalToolkit.log(new PreLink.CouldNotParseNumber(configuration.width(), "width"));
		    }
	        userSpecifiedWidth = true;
	    }
	    else if (configuration.widthPercent() != null)
	    {
		    width = configuration.defaultWidth();
	        widthPercent = configuration.widthPercent();
	    }
		else
	    {
		    width = configuration.defaultWidth();
	    }

	    if (configuration.height() != null)
	    {
		    try
		    {
			    height = Integer.parseInt(configuration.height());
		    }
		    catch(NumberFormatException nfe)
		    {
			    ThreadLocalToolkit.log(new PreLink.CouldNotParseNumber(configuration.height(), "height"));
		    }
	        userSpecifiedHeight = true;
	    }
	    else if (configuration.heightPercent() != null)
	    {
		    height = configuration.defaultHeight();
	        heightPercent = configuration.heightPercent();
	    }
		else
	    {
		    height = configuration.defaultHeight();
	    }

		size = new Rect(width * 20, height * 20);

        if ((configuration.scriptLimitsSet()))
        {
            scriptLimits = new ScriptLimits( configuration.getScriptRecursionLimit(),
                                             configuration.getScriptTimeLimit() );
        }

        framerate = configuration.getFrameRate();
        version = configuration.getTargetPlayerMajorVersion();
		bgcolor = new SetBackgroundColor(configuration.backgroundColor());
		if (configuration.generateDebugTags())
		{
			enableDebugger = new EnableDebugger(MD5Crypt.md5Crypt(configuration.debugPassword()));
			uuid = new FlashUUID();
		}

        // SWF 8 File Attributes Support
        if (version >= 8)
        {
            fileAttributes = new FileAttributes();
            fileAttributes.actionScript3 = (version >= 9);

            if (configuration.useNetwork())
            {
                fileAttributes.useNetwork = true;
	            fileAttributes.actionScript3 = (version >= 9);
            }
	        String metadataStr = configuration.getMetadata();
            if (metadataStr != null)
            {
                metadata = new Metadata();
                metadata.xml = metadataStr;
                fileAttributes.hasMetadata = true;
            }
        }

        long build = 0;
        byte majorVersion = 0;
        byte minorVersion = 0;
        
        try
        {
            majorVersion = (byte)Integer.parseInt(VersionInfo.FLEX_MAJOR_VERSION);
            minorVersion = (byte)Integer.parseInt(VersionInfo.FLEX_MINOR_VERSION);
            build = Long.parseLong( VersionInfo.getBuild() );
        }
        catch (NumberFormatException numberFormatException)
        {
            // preilly: for now just ignore empty or bogus build numbers.
        }

        
        productInfo = new ProductInfo(ProductInfo.ABOBE_FLEX_PRODUCT, 6,
                                        majorVersion, minorVersion, build, 
                                        System.currentTimeMillis());
		pageTitle = configuration.pageTitle();

		lazyInit = configuration.lazyInit();
        rootClassName = formatSymbolClassName( configuration.getRootClassName() );

        if (rootClassName == null)
            rootClassName = formatSymbolClassName( configuration.getMainDefinition() );
        
        exportedUnits = new LinkedHashMap();
	}

	protected boolean lazyInit;
    protected String rootClassName;
    protected Map exportedUnits;
    
    protected boolean generateLinkReport, generateRBList;
    protected String linkReport;
	protected String rbList;

    protected static String formatSymbolClassName( String className )
    {
        // symbolclass list likes dotted classnames
        return (className == null)? null : className.replace( ':', '.' );
    }

	public void generate(List units) throws LinkerException // List<CompilationUnit>
	{
		// create a dependency graph based on source file dependencies...
        final DependencyGraph dependencies = extractCompilationUnitInfo(units);

        // create a single frame - this is a simple movie
		final Frame frame = new Frame();
		frames = new ArrayList();
		frames.add(frame);

        exportDependencies(dependencies, frame);

        topLevelClass = formatSymbolClassName( rootClassName );

        if (ThreadLocalToolkit.errorCount() > 0) {
  			throw new LinkerException.LinkingFailed();
        }
	}

    private DependencyGraph extractCompilationUnitInfo(List units)
    {
        final DependencyGraph dependencies = new DependencyGraph(); // DependencyGraph<CompilationUnit>
	    CompilationUnit main = null;

        for (int i = 0, length = units.size(); i < length; i++)
        {
            CompilationUnit u = (CompilationUnit) units.get(i);
            Source s = u.getSource();
            String path = s.getName();

	        if (!u.isRoot())
	        {
		        // C: should also setup dependencies based on CompilationUnit.inheritance...
		        //    it still works because the list is already sorted and this is a SimpleMovie!
                dependencies.put(path, u);
		        dependencies.addVertex(new Vertex(path));
	        }
	        else
	        {
		        main = u;
	        }
        }

        if (main != null)
        {
    	    // C: should also setup dependencies based on CompilationUnit.inheritance...
    	    //    it still works because the list is already sorted and this is a SimpleMovie!
            dependencies.put(main.getSource().getName(), main);
    	    dependencies.addVertex(new Vertex(main.getSource().getName()));
        }

        return dependencies;
    }

    protected void exportUnitOnFrame( CompilationUnit u, Frame frame, boolean lazy )
    {
        if (u == null || u.getSource().isInternal())
        {
            return;
        }
        
        for (Iterator it = u.getAssets().iterator(); it.hasNext();) // Iterator<Map.Entry<String, AssetInfo>>
        {
            Map.Entry entry = (Map.Entry) it.next(); // Map.Entry<String, DefineSprite>
            String className = (String) entry.getKey(); // entry.getKey();
            AssetInfo assetInfo = (AssetInfo) entry.getValue(); // entry.getValue();
            DefineTag tag = assetInfo.getDefineTag();

            if (Trace.dependency)
                Trace.trace( u.getSource().getName() + " depends on symbolClass " + className );

            frame.addSymbolClass( formatSymbolClassName( className ), tag );

            // FIXME - this should be unnecessary, but as long as we generate the
            // FIXME - DisplayObject.mapSymbolToClass, we need it!

            if (tag.name != null)
                frame.addExport( tag );
        }

        // temporary, see note in FontVisitor
        List fonts = u.getAssets().getFonts();
        if (fonts != null)
        {
            for (Iterator iterator = fonts.iterator(); iterator.hasNext();)
            {
                DefineFont font = (DefineFont) iterator.next();
                // Special casing this as DefineFont should NOT be linked by name and can
                // have duplicate names as there can be bold and italic versions of the
                // same font name in the same movie
                // Note that the linkage between DefineText -> DefineFont is done via tag id, not name
                frame.addFont( font );
            }
        }

        DoABC tag = null;
        // FIXME: shouldn't we only be giving a name to SWFs created for a SWC?
        String name = NameFormatter.nameFromSource(u.getSource());
        if (u.getSource().isEntryPoint() || !lazy)
        {
            tag = new DoABC( name, 0 );
            // ThreadLocalToolkit.logInfo(fileName + " will be initialized eagerly at runtime.");
        }
        else
        {
            tag = new DoABC( name, 1 );
        }

        tag.abc = u.getByteCodes();
        frame.doABCs.add( tag );
        exportedUnits.put(u, frame);
    }

	private void exportDependencies(final DependencyGraph dependencies, final Frame frame)
	{
		Set cycles = Algorithms.detectCycles(dependencies); // Set<Vertex>
		if (cycles.size() == 0)
		{
			// export compilation units
			Algorithms.topologicalSort(dependencies, new Visitor() // Visitor<Vertex<String>>
			{
				public void visit(Object v) // Vertex<String> v
				{
					String fileName = (String) ((Vertex) v).getWeight(); // fileName = v.getWeight();
					CompilationUnit u = (CompilationUnit) dependencies.get(fileName);

					exportUnitOnFrame( u, frame, lazyInit );
				}
			});
		}
		else
		{
			ThreadLocalToolkit.logError("The following forms a cycle in the dependency graph: " + cycles.toString());
		}
	}

    public static int getCodeHash( Frame f )
    {
        int h = f.symbolClass.hashCode();
        for (Iterator ci = f.doABCs.iterator(); ci.hasNext();)
        {

            DoABC doABC = (DoABC) ci.next();
            for (int i = 0; i < doABC.abc.length; ++i)
                h += (doABC.abc[i] * DefineTag.PRIME);
        }
        return h & 0xffffff;
    }
    

    public List getExportedUnits()
    {
        return new ArrayList(exportedUnits.keySet());
    }
    
    public List getExportedUnitsByFrame(Frame f)
    {
    	List a = new ArrayList();
    	
    	for (Iterator i = exportedUnits.keySet().iterator(); i.hasNext(); )
    	{
    		CompilationUnit u = (CompilationUnit) i.next();
    		Frame frame = (Frame) exportedUnits.get(u);
    		if (f == frame)
    		{
    			a.add(u);
    		}
    	}
    	
    	return a;
    }
    
    public String getLinkReport()
    {
    	return linkReport;
    }
    
    public String getRBList()
    {
    	return rbList;
    }

}
