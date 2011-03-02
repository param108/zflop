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

package flex2.compiler.mxml.rep;

import flash.util.StringUtils;
import flex2.compiler.CompilationUnit;
import flex2.compiler.Context;
import flex2.compiler.css.Styles;
import flex2.compiler.css.StylesContainer;
import flex2.compiler.mxml.Configuration;
import flex2.compiler.mxml.gen.CodeFragmentList;
import flex2.compiler.mxml.gen.DescriptorGenerator;
import flex2.compiler.mxml.gen.TextGen;
import flex2.compiler.mxml.lang.FrameworkDefs;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.mxml.reflect.Property;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.mxml.reflect.TypeTable;
import flex2.compiler.mxml.rep.decl.InitializedPropertyDeclaration;
import flex2.compiler.mxml.rep.decl.PropertyDeclaration;
import flex2.compiler.mxml.rep.decl.UninitializedPropertyDeclaration;
import flex2.compiler.mxml.rep.init.ValueInitializer;
import flex2.compiler.util.*;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.IteratorChain;

import java.util.*;

/**
 *
 */
public final class MxmlDocument
{
	private final CompilationUnit unit;
	private final TypeTable typeTable;
	private final DocumentInfo info;

	private Model root;
	private final Map declarations;
	private final List bindingExpressions;
	private final Map atEmbeds;
	private final Map atResources; // bundleName -> AtResource instance
	private final Set typeRefs;
	private final StylesContainer stylesContainer;

	private String preloader;
	private boolean usePreloader;

	private DualModeLineNumberMap lineNumberMap;

	private Map anonIdCounts;	//	Type -> Integer

	private boolean bindingImportsAdded;	//	HACK see ensureBindingImports()

	public MxmlDocument(CompilationUnit unit, TypeTable typeTable, DocumentInfo info, Configuration configuration)
	{
		this.unit = unit;
		this.typeTable = typeTable;
		this.info = info;

		root = null;
		declarations = new TreeMap();
		bindingExpressions = new ArrayList();
		atEmbeds = new TreeMap();
		atResources = new TreeMap();
		typeRefs = new TreeSet();
		stylesContainer = new StylesContainer(unit, typeTable.getPerCompileData(), configuration);
		stylesContainer.setMxmlDocument(this);

		preloader = NameFormatter.toDot(FrameworkDefs.classDownloadProgressBar);
		usePreloader = true;

		lineNumberMap = null;

		anonIdCounts = new HashMap();

		bindingImportsAdded = false;

		//	transfer binding expressions out to Context
		//	TODO this should happen somewhere else
		Context context = unit.getContext();
		context.setAttribute(Context.BINDING_EXPRESSIONS, bindingExpressions);
	}

	public final CompilationUnit getCompilationUnit()
	{
		return unit;
	}

	public final String getSourcePath()
	{
		return unit.getSource().getName();
	}

	public final boolean getIsMain()
	{
		return unit.isRoot();
	}

	public final TypeTable getTypeTable()
	{
		return typeTable;
	}

	public final String getClassName()
	{
		return info.getClassName();
	}

	public final String getConvertedClassName()
	{
		return "_" + StringUtils.substitute(getClassName(), ".", "_");
	}

	public final String getPackageName()
	{
		return info.getPackageName();
	}

	public final QName getQName()
	{
		return info.getQName();
	}

	public final Type getSuperClass()
	{
		return getRoot().getType();
	}

	public final String getSuperClassName()
	{
		return NameFormatter.toDot(getSuperClass().getName());
	}

	public final boolean getHasInterfaces()
	{
		return info.getInterfaceNames().size() > 0;
	}

	public final boolean getIsInlineComponent()
	{
		return info.getRootNode().isInlineComponent();
	}

	/**
	 * TODO set this and various other stuff from Info, at construction time
	 */
	public void setRoot(Model root)
	{
		this.root = root;

		//	TODO what follows can move into ctor once root is set there from info

		if (getIsContainer())
		{
			addImport(NameFormatter.toDot(StandardDefs.CLASS_UICOMPONENTDESCRIPTOR), root.getXmlLineNumber());
		}

		String outerDocClassName = info.getRootNode().getOuterDocumentClassName();
		if (outerDocClassName != null)
		{
			addDeclaration(DocumentInfo.OUTER_DOCUMENT_PROP, outerDocClassName, 0, false, true, false);
		}
	}

	/**
	 *
	 */
	public final Model getRoot()
	{
		assert root != null : "root component not set";
		return root;
	}

	/**
	 *
	 */
	public final void addDeclaration(Model model, boolean topLevel)
	{
		if (!inheritedPropertyUsageError(model.getId(), model.getType(), model.getXmlLineNumber()))
		{
			declarations.put(model.getId(), new InitializedPropertyDeclaration(model, topLevel, model.getXmlLineNumber()));
		}
	}

	/**
	 *
	 */
	public final void addDeclaration(String id, String typeName, int line, boolean inspectable, boolean topLevel, boolean idIsAutogenerated)
	{
		if (!inheritedPropertyUsageError(id, root.getType().getTypeTable().getType(typeName), line))
		{
			declarations.put(id, new UninitializedPropertyDeclaration(id, typeName, line, inspectable, topLevel, idIsAutogenerated));
		}
	}

	/**
	 *
	 */
	public final void ensureDeclaration(Model model)
	{
		if (!isDeclared(model))
		{
			addDeclaration(model, false);
		}
	}

	/**
	 * true iff the document has a property (induced or explicit) named by the model's id
	 * TODO remove
	 */
	public final boolean isDeclared(Model model)
	{
		String id = model.getId();
		return id != null && isDeclared(id);
	}

	/**
	 * true iff the document has a property (induced or explicit) named by the id
	 */
	public final boolean isDeclared(String id)
	{
		return declarations.containsKey(id);
	}

	/**
	 * NOTE: suppress declaration of inherited properties
	 */
	public final Iterator getDeclarationIterator()
	{
		final Type superType = getSuperClass();

		return new FilterIterator(declarations.values().iterator(), new Predicate()
		{
			public boolean evaluate(Object object)
			{
				return superType.getProperty(((PropertyDeclaration)object).getName()) == null;
			}
		});
	}

	/**
	 *
	 */
	private final Iterator getTopLevelDeclarationIterator()
	{
		return new FilterIterator(declarations.values().iterator(), new Predicate()
		{
			public boolean evaluate(Object object)
			{
				return ((PropertyDeclaration)object).getTopLevel();
			}
		});
	}

	/**
	 *
	 */
	private final Iterator getTopLevelInitializerIterator()
	{
		return new FilterIterator(getTopLevelDeclarationIterator(), new Predicate()
		{
			public boolean evaluate(Object object)
			{
				return object instanceof InitializedPropertyDeclaration;
			}
		});
	}

	/**
	 * a little trickiness here: we need to initialize both our superclass properties, and document variables that
	 * have initializers
	 */
	public final Iterator getPropertyInitializerIterator()
	{
		return new IteratorChain(
				root.getPropertyInitializerIterator(false),
				getTopLevelInitializerIterator());
	}

	/**
	 * return an iterator over visual children that haven't been marked described.
	 *
	 * TODO visual children are marked described by the descriptor generator, so there is some order-of-codegen
	 * sensitivity here. It's the only such dependency, but at some point descriptor codegen and
	 * marking-of-isDescribed should be split apart.
	 */
	public final Iterator getProceduralVisualChildInitializerIterator()
	{
		if (root instanceof MovieClip)
		{
			return new FilterIterator(((MovieClip)root).getChildInitializerIterator(), new Predicate()
			{
				public boolean evaluate(Object object)
				{
					ValueInitializer init = (ValueInitializer)object;
					Object value = init.getValue();
					return !(value instanceof Model) || !((Model)value).isDescribed();
				}
			});
		}
		else
		{
			return Collections.EMPTY_LIST.iterator();
		}
	}

	/**
	 * For Flex 2.0, visual children are always described - i.e., initialized using the UICOmponentDescriptor-based
	 * DI machinery, rather than by procedural code. Future variations of this approach can be tested by modifying
	 * what this method returns - e.g. by querying a config variable, making a per-document decision (say, based on
	 * metadata or base class), or whatever.
	 *
	 * (Note that DI is always used for Repeater contents, independent of this setting.)
	 */
	public final boolean getDescribeVisualChildren()
	{
		return true;
	}

	/**
	 * iterator over all definitions from our toplevel declarations and root initializers
	 */
	public final Iterator getDefinitionIterator()
	{
		IteratorList iterList = new IteratorList();

		Model.addDefinitionIterators(iterList, getTopLevelInitializerIterator());

		iterList.add(root.getSubDefinitionsIterator());

		return iterList.toIterator();
	}

	/**
	 *
	 */
	public final void addBindingExpression(BindingExpression expr)
	{
		expr.setId(bindingExpressions.size());
		bindingExpressions.add(expr);
		info.addInterfaceName(NameFormatter.toDot(StandardDefs.INTERFACE_IBINDINGCLIENT), -1);
	}

	public final List getBindingExpressions()
	{
		return bindingExpressions;
	}

	public final boolean addAtEmbed(AtEmbed atEmbed)
	{
        if (!atEmbeds.containsKey(atEmbed.getPropName()))
        {
            atEmbeds.put(atEmbed.getPropName(), atEmbed);
        }
        return true;
	}

	public final Collection getAtEmbeds()
	{
		return atEmbeds.values();
	}

	public final boolean addAtResource(AtResource atResource)
	{
        // @Resource codegen (AtResource.getValueExpression()) requires mx.resources.ResourceManager
        addImport(NameFormatter.toDot( StandardDefs.CLASS_RESOURCEMANAGER ),
                  atResource.getXmlLineNumber());
		atResources.put(atResource.getBundle(), atResource);
		return true;
	}

	public final Collection getAtResources()
	{
		return atResources.values();
	}

	/**
	 *
	 */
	public final void addTypeRef(String typeRef, int line)
	{
		addImport(typeRef, line);
		typeRefs.add(typeRef);
	}

	public final Collection getTypeRefs()
	{
		return typeRefs;
	}

	/**
	 *
	 */
	public final void addImport(String name, int line)
	{
		info.addImportName(name, line);
	}

	public final Collection getImports()
	{
		ensureBindingImports();
		return info.getImportNames();
	}

	//	HACK: because essential stuff in a BindingExpression is set up *after* addBindingExpression() is called
	//	on it, we have to wait until the last minute before adding their destination types to the import list.
	//	TODO clean up BindingExpression setup. A BE should be completely configured by the time it's added here.
	private final void ensureBindingImports()
	{
		if (!bindingImportsAdded)
		{
			for (Iterator iter = bindingExpressions.iterator(); iter.hasNext(); )
			{
				BindingExpression expr = (BindingExpression)iter.next();
				addImport(expr.getDestinationTypeName(), expr.getXmlLineNumber());
			}
			bindingImportsAdded = true;
		}
	}

	/**
	 *
	 */
	public final void addScript(Script script)
	{
		info.addScript(script);
	}

	public final List getScripts()
	{
		return info.getScripts();
	}

	/**
	 *
	 */
	public final void addMetadata(Script metaDataSource)
	{
		String text = metaDataSource.getText();
		assert text != null;

		// FIXME - when people stop abusing metadata, this hack can be nuked.
		if (!text.startsWith("["))
		{
			info.getMetadata().add(0, metaDataSource);
		}
		else
		{
			info.addMetadata(metaDataSource);
		}
	}

	public final List getMetadata()
	{
		return info.getMetadata();
	}

	/**
	 *
	 */
	public StylesContainer getStylesContainer()
	{
		return stylesContainer;
	}

	/**
	 *
	 */
	public Iterator getInheritingStyleNameIterator()
	{
		final Styles styles = typeTable.getStyles();
		return new FilterIterator(styles.getStyleNames(), new Predicate()
			{
				public boolean evaluate(Object obj) { return styles.isInheritingStyle((String)obj); }
			});
	}

	/**
	 *
	 */
	public boolean getIsContainer()
	{
		return StandardDefs.isContainer(root.getType());
	}

	public boolean getIsIUIComponent()
	{
		return StandardDefs.isIUIComponent(root.getType());
	}

	public final void setLineNumberMap(DualModeLineNumberMap lineNumberMap)
	{
		this.lineNumberMap = lineNumberMap;
	}

	public final DualModeLineNumberMap getLineNumberMap()
	{
		return lineNumberMap;
	}

	public final void setPreloader(String preloader)
	{
		this.preloader = preloader;
	}

	public final String getPreloader()
	{
		return preloader;
	}

	public void setUsePreloader(boolean usePreloader)
	{
		this.usePreloader = usePreloader;
	}

	public final boolean getUsePreloader()
	{
		return usePreloader;
	}

	/**
	 * NOTE the phase situation here if super is MXML that is being compiled in this run. In that case, this call will
	 * be examining the "public signature" type put together by InterfaceCompiler, *not* the fully built component.
	 */
	public final boolean superHasPublicProperty(String name)
	{
		return getSuperClass().getProperty(name) != null;
	}

	/**
	 *
	 */
	void ensureId(Model model)
	{
		if (model.getId() == null)
		{
			Type type = model.getType();
			assert type != null;

			int i = getAnonIndex(model.getType());

			String id = "_" + NameFormatter.toDot(info.getClassName()).replace('.', '_') +
						"_" + NameFormatter.retrieveClassName(type.getName()) + i;
			// String id = "_" + NameFormatter.retrieveClassName(type.getName()) + i;

			model.setId(id, true);
		}
	}

	/**
	 * Note: we use the leaf name as our key, rather than the full classname (Foo rather than a.b.c.Foo).
	 * This allows us to use generated function names like _Foo, rather than _a_b_c_Foo, for readability.
	 */
	private int getAnonIndex(Type type)
	{
		String typeName = NameFormatter.retrieveClassName(type.getName());
		Integer cell = (Integer)anonIdCounts.get(typeName);
		int i = cell == null ? 1 : cell.intValue();
		anonIdCounts.put(typeName, new Integer(i + 1));
		return i;
	}

	/**
	 *
	 */
	public void resolveTwoWayBindings()
	{
		Map destinationMap = new HashMap();

		Iterator iterator = bindingExpressions.iterator();

		while (iterator.hasNext())
		{
			BindingExpression bindingExpression = (BindingExpression) iterator.next();
			String sourceExpression = TextGen.stripParens(bindingExpression.getSourceExpression());
			String destinationPath = bindingExpression.getDestinationPath(false);
			BindingExpression match = (BindingExpression) destinationMap.get(sourceExpression);
			if ((match != null) && destinationPath.equals(TextGen.stripParens(match.getSourceExpression())))
			{
				bindingExpression.setTwoWayCounterpart(match);
			}
			else
			{
				destinationMap.put(destinationPath, bindingExpression);
			}
		}
	}

	/**
	 *
	 */
	public String getInterfaceList()
	{
		List names = new ArrayList(info.getInterfaceNames().size());
		for (Iterator i = info.getInterfaceNames().iterator(); i.hasNext();)
		{
			names.add(((DocumentInfo.NameInfo) i.next()).getName());
		}
		return TextGen.toCommaList(names.iterator());
	}

	/**
	 *
	 */
    public String getWatcherSetupUtilClassName()
    {
        StringBuffer stringBuffer = new StringBuffer("_");

        String packageName = getPackageName();

        if ((packageName != null) && (packageName.length() > 0))
        {
            stringBuffer.append( packageName.replace('.', '_') );
            stringBuffer.append("_");
        }

        stringBuffer.append( getClassName() );
        stringBuffer.append("WatcherSetupUtil");

        return stringBuffer.toString();
    }

	/**
	 *
	 */
	public CodeFragmentList getDescriptorDeclaration(String name)
	{
		CodeFragmentList fragList = DescriptorGenerator.getDescriptorInitializerFragments(
				getRoot(), FrameworkDefs.requiredTopLevelDescriptorProperties);

		fragList.add(0, "private var " + name + " : " + NameFormatter.toDot(StandardDefs.CLASS_UICOMPONENTDESCRIPTOR) + " = ", 0);

		return fragList;
	}

	/**
	 * If an inherited property by the given name exists, we check usage constraints.
	 * @return true iff inherited property exists, and an assignment to it (under the given type) is an error.
	 */
	private final boolean inheritedPropertyUsageError(String name, Type type, int line)
	{
		assert root != null : "root null in checkInherited";

		Property prop = root.getType().getProperty(name);

		if (prop != null)
		{
			if (!prop.hasPublic())
			{
				ThreadLocalToolkit.log(new NonPublicInheritedPropertyInit(name), getSourcePath(), line);
				return true;
			}

			if (prop.readOnly())
			{
				ThreadLocalToolkit.log(new ReadOnlyInheritedPropertyInit(name), getSourcePath(), line);
				return true;
			}

			if (!type.isAssignableTo(prop.getType()))
			{
				ThreadLocalToolkit.log(
						new TypeIncompatibleInheritedPropertyInit(
								name,
								NameFormatter.toDot(prop.getType().getName()),
								NameFormatter.toDot(type.getName())),
						getSourcePath(), line);

				return true;
			}
		}

		return false;
	}

	/**
	 * delegate to FrameworkDefs for names of generated management vars
	 */
	public static List getBindingManagementVars()
	{
		return FrameworkDefs.bindingManagementVars; 
	}

	/**
	 * CompilerErrors
	 */
	public static class NonPublicInheritedPropertyInit extends CompilerMessage.CompilerError
	{
		public String name;
		public NonPublicInheritedPropertyInit(String name) { this.name = name; }
	}

	public static class ReadOnlyInheritedPropertyInit extends CompilerMessage.CompilerError
	{
		public String name;
		public ReadOnlyInheritedPropertyInit(String name) { this.name = name; }
	}

	public static class TypeIncompatibleInheritedPropertyInit extends CompilerMessage.CompilerError
	{
		public String name, propertyType, valueType;
		public TypeIncompatibleInheritedPropertyInit(String name, String propertyType, String valueType)
		{
			this.name = name;
			this.propertyType = propertyType;
			this.valueType = valueType;
		}
	}
}
