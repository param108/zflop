////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler;

import flex2.compiler.abc.MetaData;
import flex2.compiler.css.Styles;
import flex2.compiler.css.StylesContainer;
import flex2.compiler.util.*;
import macromedia.asc.util.ByteList;

import java.util.*;

/**
 * @author Clement Wong
 */
public final class CompilationUnit
{
    // Jono: When adding a getter/setter to CU, be sure to update Source.copy()
    
	public static final int Empty = 0;
	public static final int SyntaxTree = 1;
	public static final int abc = 2;
	public static final int Done = 4;

	static final String COMPILATION_UNIT = CompilationUnit.class.getName();

	// C: not a public constructor
	CompilationUnit(Source source, Object syntaxTree, Context context)
	{
		this.source = source;
		this.syntaxTree = syntaxTree;
		this.context = context;

		reset();
	}

	/**
	 * CompilationUnit()
	 */
	private Source source;

	/**
	 * Compiler.parse()
	 */
	private Object syntaxTree;

	/**
	 * CompilationUnit()
	 */
	private Context context;

	/**
	 * Compiler.parse()
	 */
	private Assets assets;

	/**
	 * Compiler.parse(), analyze1234(), generate()
	 */
	private int state;
	private int workflow;

	/**
	 * Compiler.generate()
	 */
	public ByteList bytes;

	/**
	 * Compiler.parse(), AS3 metadata only
	 */
	public Set metadata; // List<MetaDataNode> - MetaDataNodes may reference huge DefinitionNode

	/**
	 * Compiler.parse(), AS3 metadata only, doesn't pull in dependencies
	 */
	public MetaData swfMetaData;

	/**
	 * Compiler.parse(), AS3 metadata only, doesn't pull in dependencies
	 */
	public MetaData iconFile;

	/**
	 * Compiler.parse(), AS3 metadata only, not this unit's dependency, processed by getExtraSources()
	 */
	public String loaderClass;

	/**
	 * Compiler.analyze4(), AS3 metadata only, module factory base class, not this unit's dependency
	 */
	public String loaderClassBase;

	/**
	 * Compiler.parse(), PreLink (styles), AS3 metadata, not this unit's dependencies, processed by getExtraSources()
	 */
	public Set extraClasses; // Set<String>

	/**
	 * inline components, embeds, WatcherSetupUtil, unit.expressions, processed by addGeneratedSources()
	 */
	private Map generatedSources; // Map<QName, Source>

	/**
	 * ApplicationBuilder, should persist
	 */
	public Map auxGenerateInfo; // context gets cleared, need something to survive

	/**
	 * Compiler.parse(), AS3 metadata only, FlexInit class's dependencies, not this unit's dependencies, CompcPreLink, PreLink, in this unit's swc
	 */
	private Set accessibilityClasses;

	/**
	 * Compiler.parse(), AS3 metadata only, processed by getExtraSources(), not this unit's dependency, in this unit's swc
	 */
    public Map licensedClassReqs; // class-in-this-unit to licensed product id

	/**
	 * Compiler.parse(), AS3 metadata only, doesn't pull in dependencies, PreLink
	 */
    public Map remoteClassAliases; // class-in-this-unit to alias

	/**
	 * Compiler.parse(), AS3 metadata only, doesn't pull in dependencies, FlexInit, PreLink
	 */
	public Map effectTriggers;

	/**
	 * Compiler.parse(), PreLink (fontface rules), AS3 metadata, not this unit's dependencies, FlexInit only references by names
	 */
	public Set mixins; // Set<String> - classes in this unit to call init on

	/**
	 * Compiler.parse(), AS3 metadata only, not this unit's dependencies, processed by getExtraSources()
	 */
	public Set resourceBundles; // Set<String>- classes in this unit to add as resource bundles
	public Set resourceBundleHistory; // Set<String>- classes in this unit to add as resource bundles

	public QNameList topLevelDefinitions;
	public MultiNameSet inheritance; // MultiName or QName
	public MultiNameSet types; // MultiName or QName
	public MultiNameSet expressions; // MultiName or QName
	public MultiNameSet namespaces; // MultiName or QName
	public Set importPackageStatements; // String
	public QNameSet importDefinitionStatements; // QName

	public MultiNameMap inheritanceHistory;
	public MultiNameMap typeHistory;
	public MultiNameMap namespaceHistory;
	public MultiNameMap expressionHistory;

	public Styles styles;

	/**
	 * only MXML components set StylesContainer
	 */
	private StylesContainer stylesContainer;

	public boolean hasTypeInfo;
	public Object typeInfo; // ObjectValue
	public Map classTable; // String, flex2.compiler.abc.Class
    
    /**
     * The CRC32 of the class signature, coming from SignatureExtension.
     * Null means a signature wasn't generated.
     * 
     * Used by SignatureExtension, PersistenceStore, API::validateCompilationUnits 
     */
    // TODO Only AS Sources can have these, should this really be here?
    // TODO it doesn't feel right to do this here, since Signatures are Extensions...
    //      where should an extension store its data?
    private Long signatureChecksum;
    
    public void setSignatureChecksum(Long signatureChecksum)
    {
        this.signatureChecksum = signatureChecksum;
    }
    
    public Long getSignatureChecksum()
    {
        return signatureChecksum;
    }
    
    public boolean hasSignatureChecksum()
    {
        return signatureChecksum != null;
    }

	/**
	 * equivalent to setting this = new CompilationUnit(this.source, this.syntaxTree, this.context)
	 */
	void resetKeepTypeInfo()
	{
		assets = new Assets();
        
        //TODO is this correct? should it go in reset()? if here, I assume the CU will get run
        //     through the AS3 compiler again?
        signatureChecksum = null;

		state = Empty;
		workflow = 0;

		if (bytes == null)
		{
			bytes = new ByteList();
		}
		else
		{
			bytes.clear();
		}

		if (metadata == null)
		{
			metadata = new HashSet();
		}
		else
		{
			metadata.clear();
		}

		swfMetaData = null;
		iconFile = null;
		loaderClass = null;

		if (extraClasses == null)
		{
			extraClasses = new HashSet();
		}
		else
		{
			extraClasses.clear();
		}

		generatedSources = null;
		auxGenerateInfo = null;
		accessibilityClasses = null;

		if (remoteClassAliases == null)
		{
			remoteClassAliases = new HashMap(1);
		}
		else
		{
			remoteClassAliases.clear();
		}

        if (licensedClassReqs == null)
        {
            licensedClassReqs = new HashMap(1);
        }
        else
        {
            licensedClassReqs.clear();
        }

        if (effectTriggers == null)
		{
			effectTriggers = new HashMap(1);
		}
		else
		{
			effectTriggers.clear();
		}

		mixins = new HashSet(2);

		if (resourceBundles == null)
		{
			resourceBundles = new HashSet(1);
			resourceBundleHistory = new HashSet(1);
		}
		else
		{
			resourceBundles.clear();
			resourceBundleHistory.clear();
		}

		if (topLevelDefinitions == null)
		{
			topLevelDefinitions = new QNameList(source.isSourcePathOwner() || source.isSourceListOwner() ? 1 : 8);
		}
		else
		{
			topLevelDefinitions.clear();
		}

		if (inheritance == null)
		{
			inheritance = new MultiNameSet(1, 2);
			types = new MultiNameSet(3, 8);
			expressions = new MultiNameSet(4, 8);
			namespaces = new MultiNameSet(2, 2);
			importPackageStatements = new HashSet(16);
			importDefinitionStatements = new QNameSet(16);

			inheritanceHistory = new MultiNameMap(2);
			typeHistory = new MultiNameMap(8);
			expressionHistory = new MultiNameMap(8);
			namespaceHistory = new MultiNameMap(2);
		}
		else
		{
			inheritance.clear();
			types.clear();
			expressions.clear();
			namespaces.clear();
			importPackageStatements.clear();
			importDefinitionStatements.clear();

			inheritanceHistory.clear();
			typeHistory.clear();
			expressionHistory.clear();
			namespaceHistory.clear();
		}

		if (styles == null)
		{
			styles = new Styles(2);
		}
		else
		{
			styles.clear();
		}
		
		checkBits = 0;
	}

	void reset()
	{
		resetKeepTypeInfo();
		removeTypeInfo();
	}

	void removeTypeInfo()
	{
		hasTypeInfo = false;
		typeInfo = null;
		if (classTable == null)
		{
			classTable = new HashMap((source.isSourcePathOwner() || source.isSourceListOwner()) ? 4 : 8); // Map<String, Class>
		}
		else
		{
			classTable.clear();
		}
	}

	public boolean isRoot()
	{
		return source.isRoot();
	}

	// used by InterfaceAnalyzer.createInlineComponentUnit()
	public void addGeneratedSource(QName defName, Source source)
	{
		if (generatedSources == null)
		{
			generatedSources = new HashMap();
		}
		generatedSources.put(defName, source);
	}

	// used by DataBindingExtension, EmbedEvaluator
	public void addGeneratedSources(Map generatedSources)
	{
		if (generatedSources != null)
		{
			if (this.generatedSources == null)
			{
				this.generatedSources = new HashMap();
			}
			this.generatedSources.putAll(generatedSources);
		}
	}

	public void clearGeneratedSources()
	{
		generatedSources = null;
	}

	public Map getGeneratedSources()
	{
		return generatedSources;
	}

	public Source getSource()
	{
		return source;
	}

	void setState(int flag)
	{
		state |= flag;
		if (flag == abc)
		{
			syntaxTree = null;
			// source.setPathResolver(null);
			source.disconnectLogger();
			Map misc = (Map) context.getAttribute("SwcScript.misc");
			if (misc != null)
			{
				misc.put(COMPILATION_UNIT, this);
			}
		}
		else if (flag == Done)
		{
			hasTypeInfo = typeInfo != null;
			context.clear();
			metadata.clear();
			source.clearSourceFragments();
		}
	}

	int getState()
	{
		return state;
	}

	public boolean isBytecodeAvailable()
	{
		return (state & abc) != 0;
	}

	public boolean isDone()
	{
		return (state & Done) != 0;
	}

	void setWorkflow(int flag)
	{
		workflow |= flag;
	}

	int getWorkflow()
	{
		return workflow;
	}

	public Object getSyntaxTree()
	{
		return syntaxTree;
	}

	public void setSyntaxTree(Object syntaxTree)
	{
		this.syntaxTree = syntaxTree;
	}

	public Context getContext()
	{
		return context;
	}

	public Assets getAssets()
	{
		return assets;
	}

	public void addAccessibilityClasses(CompilationUnit u)
	{
		if (u.accessibilityClasses != null)
		{
			if (accessibilityClasses == null)
			{
				accessibilityClasses = new HashSet();
			}
			accessibilityClasses.addAll(u.accessibilityClasses);
		}
	}

	public void addAccessibilityClass(MetaData metadata)
	{
		if (accessibilityClasses == null)
		{
			accessibilityClasses = new HashSet();
		}

		String accessibilityClass = metadata.getValue("implementation");
		if (!accessibilityClasses.contains(accessibilityClass))
		{
			accessibilityClasses.add(accessibilityClass);
		}
	}

	public Set getAccessibilityClasses()
	{
		return accessibilityClasses;
	}

	public byte[] getByteCodes()
	{
		return bytes.toByteArray(false);
	}

	public StylesContainer getStylesContainer()
	{
		return stylesContainer;
	}

	public void setStylesContainer(StylesContainer stylesContainer)
	{
		this.stylesContainer = stylesContainer;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof CompilationUnit)
		{
			return ((CompilationUnit) obj).getSource() == getSource();
		}
		else
		{
			return false;
		}
	}
	
	// C: There is no need to persist this value. Ideally it should be in Context, but using Integer
	//    is going to be a bit slower than using int.
	public int checkBits = 0;

	public String toString()
	{
		return source.getName();
	}
}
