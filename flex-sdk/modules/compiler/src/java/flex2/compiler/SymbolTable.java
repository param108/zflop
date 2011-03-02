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

import flex2.compiler.abc.*;
import flex2.compiler.abc.Class;
import flex2.compiler.as3.BytecodeEmitter;
import flex2.compiler.as3.binding.TypeAnalyzer;
import flex2.compiler.common.Configuration;
import flex2.compiler.css.StyleConflictException;
import flex2.compiler.css.Styles;
import flex2.compiler.util.*;
import flex2.tools.oem.ProgressMeter;
import macromedia.asc.util.ContextStatics;
import macromedia.asc.util.ObjectList;

import java.util.*;

/**
 * Inter-compiler symbol table for type lookup.
 *
 * @author Clement Wong
 */
public final class SymbolTable
{
	// These may look funny, but they line up with the values that ASC uses.
	public static final String internalNamespace = "internal";
	public static final String privateNamespace = "private";
	public static final String protectedNamespace = "protected";
	public static final String publicNamespace = "";
	public static final String unnamedPackage = "";
	public static final String[] VISIBILITY_NAMESPACES = new String[] {SymbolTable.publicNamespace,
																	   SymbolTable.protectedNamespace,
																	   SymbolTable.internalNamespace,
																       SymbolTable.privateNamespace};

	public static final String NOTYPE = "*";
	public static final String STRING = "String";
	public static final String BOOLEAN = "Boolean";
	public static final String NUMBER = "Number";
	public static final String INT = "int";
	public static final String UINT = "uint";
	public static final String NAMESPACE = "Namespace";
	public static final String FUNCTION = "Function";
	public static final String CLASS = "Class";
	public static final String ARRAY = "Array";
	public static final String OBJECT = "Object";
	public static final String XML = "XML";
	public static final String XML_LIST = "XMLList";
	public static final String REGEXP = "RegExp";
	public static final String EVENT = "flash.events:Event";

	static class NoType implements flex2.compiler.abc.Class
	{
		public Variable getVariable(String[] namespaces, String name, boolean inherited)
		{
			return null;
		}

		public QName[] getVariableNames()
		{
			assert false;
			return null;
		}

		public Method getMethod(String[] namespaces, String name, boolean inherited)
		{
			return null;
		}

		public QName[] getMethodNames()
		{
			assert false;
			return null;
		}

		public Method getGetter(String[] namespaces, String name, boolean inherited)
		{
			return null;
		}

		public QName[] getGetterNames()
		{
			assert false;
			return null;
		}

		public Method getSetter(String[] namespaces, String name, boolean inherited)
		{
			return null;
		}

		public QName[] getSetterNames()
		{
			assert false;
			return null;
		}

		public Namespace getNamespace(String nsName)
		{
			return null;
		}

		public String getName()
		{
			return NOTYPE;
		}

		public String getSuperTypeName()
		{
			return null;
		}

		public String[] getInterfaceNames()
		{
			return null;
		}

		public Attributes getAttributes()
		{
			return null;
		}

		public List getMetaData(boolean inherited) // List<MetaData>
		{
			return null;
		}

		public List getMetaData(String name, boolean inherited)
		{
			return null;
		}

		public boolean implementsInterface(String interfaceName)
		{
			return false;
		}

		public boolean isSubclassOf(String baseName)
		{
			return false;
		}

		public boolean isInterface()
		{
			assert false;
			return false;
		}

		public void setTypeTable(Object typeTable)
		{
		}
	}

	private static final NoType NoTypeClass = new NoType();

	public SymbolTable(boolean bang, int dialect, boolean suppressWarnings, int targetAVM, ObjectList use_namespaces) 
	{
		classTable = new HashMap(300);
		styles = new Styles();
		
		perCompileData = new ContextStatics();
		perCompileData.use_static_semantics = bang;
		perCompileData.dialect = dialect;
		perCompileData.languageID = macromedia.asc.util.Context.getLanguageID(Locale.getDefault().getCountry().toUpperCase());
        perCompileData.setAbcVersion(targetAVM);
        
        // set up use_namespaces anytime before parsing begins
        assert use_namespaces != null;
        perCompileData.use_namespaces.addAll(use_namespaces);
        
		ContextStatics.useVerboseErrors = false;
		
		qNameTable = new QNameMap(300);
		multiNames = new HashMap(1024);
		macromedia.asc.util.Context cx = new macromedia.asc.util.Context(perCompileData);      
		emitter = new BytecodeEmitter(cx, null, false);
		cx.setEmitter(emitter);
		typeAnalyzer = new TypeAnalyzer(this);
		
		rbNames = new HashMap();
		rbNameTable = new HashMap();
	}

	public SymbolTable(ContextStatics contextStatics)
	{
		classTable = new HashMap(300);
		styles = new Styles();
		
		perCompileData = contextStatics;
		
		qNameTable = new QNameMap(300);
		multiNames = new HashMap(1024);
		macromedia.asc.util.Context cx = new macromedia.asc.util.Context(perCompileData);
		emitter = new BytecodeEmitter(cx, null, false);
		cx.setEmitter(emitter);
		typeAnalyzer = new TypeAnalyzer(this);
		
		rbNames = new HashMap();
		rbNameTable = new HashMap();
	}

	private final Map classTable; // Map<String, Class>

	// C: if possible, move styles out of SymbolTable...
	private final Styles styles;

	// C: ContextStatics stays here because it holds namespace and type info...
	public final ContextStatics perCompileData;

	private final QNameMap qNameTable; // QName --> Source
	private final Map multiNames; // MultiName --> QName

	// C: This single instance is for ConstantEvaluator to calculate doubles only.
	public final BytecodeEmitter emitter;

	private Context context;

	// C: please see CompilerConfiguration.suppressWarningsInIncremental
	private boolean suppressWarnings;
	
	private final Map rbNames; // String --> QName[]
	private final Map rbNameTable; // String --> Source
	
	public int tick = 0;
	public int currentPercentage = 0;
	
	public void adjustProgress()
	{
		ProgressMeter meter = ThreadLocalToolkit.getProgressMeter();
		
		for (int i = currentPercentage + 1; meter != null && i <= 100; i++)
		{
			meter.percentDone(i);
		}
	}
	
	public void registerClass(String className, Class cls)
	{
		assert className.indexOf('/') == -1;

		classTable.put(className, cls);
	}

	public Class getClass(String className)
	{
		assert className == null || className.indexOf('/') == -1;

		if (className != null)
		{
			return ("*".equals(className)) ? NoTypeClass : (Class) classTable.get(className);
		}
		else
		{
			return null;
		}
	}

	// app-wide style management

	public void registerStyles(Styles newStyles) throws StyleConflictException
	{
		styles.addStyles(newStyles);
	}

	public MetaData getStyle(String styleName)
	{
		if (styleName != null)
		{
			return (MetaData) styles.getStyle(styleName);
		}
		else
		{
			return null;
		}
	}

	public Styles getStyles()
	{
		return styles;
	}

	/**
	 * It is possible for a Source to define multiple definitions. This method creates mappings between
	 * the definitions and the Source instance.
	 */
	void registerQNames(QNameList qNames, Source source)
	{
		for (int i = 0, size = qNames.size(); i < size; i++)
		{
			QName qN = (QName) qNames.get(i);
			qNameTable.put(qN, source);
		}
	}

	/**
	 * If API.resolveMultiName() is successful, the QName result should be associated with a Source object.
	 * Store the mapping here...
	 *
	 * @param qName ClassDefinitionNode.cframe.classname
	 * @param source Source
	 */
	public void registerQName(QName qName, Source source)
	{
		Source old = (Source) qNameTable.get(qName);
		if (old == null)
		{
			qNameTable.put(new QName(qName), source);
		}
		else if (!old.getName().equals(source.getName()))
		{
			assert false : qName + " defined in " + old + " and " + source.getName();
		}
	}
	
	public void registerResourceBundle(String rbName, Source source)
	{
		/*
		Source old = (Source) rbNameTable.get(rbName);
		if (old == null)
		{
			rbNameTable.put(rbName, source);
		}
		*/
		rbNameTable.put(rbName, source);
	}

	/**
	 * If API.resolveMultiName() is successful, the QName result should be associated with a Source object.
	 * This method allows for quick lookups given a qname.
	 */
	public Source findSourceByQName(QName qName)
	{
		return (Source) qNameTable.get(qName);
	}

	/**
	 * If API.resolveMultiName() is successful, the QName result should be associated with a Source object.
	 * This method allows for quick lookups given a qname.
	 */
	public Source findSourceByQName(String namespaceURI, String localPart)
	{
		return (Source) qNameTable.get(namespaceURI, localPart);
	}
	
	public Source findSourceByResourceBundleName(String rbName)
	{
		return (Source) rbNameTable.get(rbName);
	}

	/**
	 * If API.resolveMultiName() successfully resolves a multiname to a qname, the result will be stored here.
	 */
	void registerMultiName(MultiName multiName, QName qName)
	{
		multiNames.put(multiName, qName);
	}
	
	void registerResourceBundleName(String rbName, QName[] qNames)
	{
		rbNames.put(rbName, qNames);
	}

	/**
	 * If API.resolveMultiName() successfully resolves a multiname to a qname, the result will be stored here.
	 * This method allows for quick lookup.
	 */
	public QName isMultiNameResolved(MultiName multiName)
	{
		return (QName) multiNames.get(multiName);
	}
	
	public QName[] isResourceBundleResolved(String rbName)
	{
		return (QName[]) rbNames.get(rbName);
	}

	/**
	 * placeholder for transient data
	 */
	public Context getContext()
	{
		if (context == null)
		{
			context = new Context();
		}

		return context;
	}

	/**
	 * dereference the flex2.compiler.abc.Class instances from flex2.compiler.as3.reflect.TypeTable. This is
	 * necessary for lowering the peak memory. It also makes the instances reusable in subsequent compilations.
	 */
	public void cleanClassTable()
	{
		for (Iterator i = classTable.keySet().iterator(); i.hasNext();)
		{
			flex2.compiler.abc.Class c = (flex2.compiler.abc.Class) classTable.get(i.next());
			c.setTypeTable(null);
		}
	}

	// The following is for TypeAnalyzer only... please do not expand the usage to the other classes...

	private TypeAnalyzer typeAnalyzer;

	public TypeAnalyzer getTypeAnalyzer()
	{
		return typeAnalyzer;
	}
    
    public boolean getSuppressWarningsIncremental()
    {
        return suppressWarnings;
    }
    
    /**
     * This pattern comes up often when creating a new SymbolTable
     */
    public static SymbolTable newSymbolTable(Configuration configuration)
    {
        return new SymbolTable(
                      configuration.getCompilerConfiguration().strict(),
                      configuration.getCompilerConfiguration().dialect(),
                      configuration.getCompilerConfiguration().suppressWarningsInIncremental(),
                      configuration.getTargetPlayerTargetAVM(),
                      configuration.getTargetPlayerRequiredUseNamespaces());
    }
    
    public void register(String rbName, QName[] qNames, Source source)
    {
		if (source != null)
		{
			registerResourceBundleName(rbName, qNames);
			registerResourceBundle(rbName, source);
			
			for (int i = 0, length = qNames == null ? 0 : qNames.length; i < length; i++)
			{
				registerQName(qNames[i], source);
			}
		}
    }
}
