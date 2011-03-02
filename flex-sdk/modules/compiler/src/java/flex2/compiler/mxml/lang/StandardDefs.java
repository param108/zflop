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

package flex2.compiler.mxml.lang;

import flex2.compiler.mxml.dom.MethodNode;
import flex2.compiler.mxml.dom.Node;
import flex2.compiler.mxml.dom.OperationNode;
import flex2.compiler.mxml.reflect.Type;
import flex2.compiler.util.NameFormatter;

import java.util.*;

/**
 * MXML standard (i.e., framework-independent) AS support classes, packages, import sets, etc.
 * NOTE: definition names (interface*, class*) are generally stored here in internal format, usable for typetable lookups.
 * Use NameFormatter.toDot() to convert to source code format.
 * (Note: an exception is lists of import names, in dot format already.)
 */
public class StandardDefs
{
	//  swcs
	public static final String SWC_AIRGLOBAL = "airglobal.swc";
	public static final String SWC_AVMPLUS = "avmplus.swc";
	public static final String SWC_PLAYERGLOBAL = "playerglobal.swc";

	//	packages
	public static final String PACKAGE_FLASH_ACCESSIBILITY = "flash.accessibility";
	public static final String PACKAGE_FLASH_DATA = "flash.data";
	public static final String PACKAGE_FLASH_DEBUGGER = "flash.debugger";
	public static final String PACKAGE_FLASH_DESKTOP = "flash.desktop";
	public static final String PACKAGE_FLASH_DISPLAY = "flash.display";
	public static final String PACKAGE_FLASH_ERRORS = "flash.errors";
	public static final String PACKAGE_FLASH_EVENTS = "flash.events";
	public static final String PACKAGE_FLASH_EXTERNAL = "flash.external";
	public static final String PACKAGE_FLASH_FILESYSTEM = "flash.filesystem";
	public static final String PACKAGE_FLASH_FILTERS = "flash.filters";
	public static final String PACKAGE_FLASH_GEOM = "flash.geom";
	public static final String PACKAGE_FLASH_HTML = "flash.html";
	public static final String PACKAGE_FLASH_HTML_SCRIPT = "flash.html.script";
	public static final String PACKAGE_FLASH_MEDIA = "flash.media";
	public static final String PACKAGE_FLASH_NET = "flash.net";
	public static final String PACKAGE_FLASH_PRINTING = "flash.printing";
	public static final String PACKAGE_FLASH_PROFILER = "flash.profiler";
	public static final String PACKAGE_FLASH_SYSTEM = "flash.system";
	public static final String PACKAGE_FLASH_TEXT = "flash.text";
	public static final String PACKAGE_FLASH_UI = "flash.ui";
	public static final String PACKAGE_FLASH_UTILS = "flash.utils";
	public static final String PACKAGE_FLASH_XML = "flash.xml";
	public static final String PACKAGE_MX_BINDING = "mx.binding";
	public static final String PACKAGE_MX_CORE = "mx.core";
	public static final String PACKAGE_MX_DATA = "mx.data";
	public static final String PACKAGE_MX_DATA_UTILS = "mx.data.utils";
	public static final String PACKAGE_MX_EFFECTS = "mx.effects";
	public static final String PACKAGE_MX_EVENTS = "mx.events";
	public static final String PACKAGE_MX_MANAGERS = "mx.managers";
	public static final String PACKAGE_MX_MESSAGE_CONFIG = "mx.message.config";
	public static final String PACKAGE_MX_MODULES = "mx.modules";
	public static final String PACKAGE_MX_PRELOADERS = "mx.preloaders";
	public static final String PACKAGE_MX_RESOURCE = "mx.resources";
	public static final String PACKAGE_MX_RPC = "mx.rpc";	//	TODO to FramewkDefs? RpcDefs?
	public static final String PACKAGE_MX_RPC_XML = "mx.rpc.xml";	//	TODO to FramewkDefs? RpcDefs?
	public static final String PACKAGE_MX_STYLES = "mx.styles";
	public static final String PACKAGE_MX_UTILS = "mx.utils";

	//	namespaces
	public static final String NAMESPACE_MX_INTERNAL_LOCALNAME = "mx_internal";
	public static final String NAMESPACE_MX_INTERNAL_URI = "http://www.adobe.com/2006/flex/mx/internal";
	public static final String NAMESPACE_MX_INTERNAL = PACKAGE_MX_CORE + ":" + NAMESPACE_MX_INTERNAL_LOCALNAME;

	//	interfaces
	public static final String INTERFACE_IBINDINGCLIENT = NameFormatter.toColon(PACKAGE_MX_BINDING, "IBindingClient");
	public static final String INTERFACE_ICHILDLIST = NameFormatter.toColon(PACKAGE_MX_CORE, "IChildList");
	public static final String INTERFACE_ICONTAINER = NameFormatter.toColon(PACKAGE_MX_CORE, "IContainer");
	public static final String INTERFACE_IDEFERREDINSTANCE = NameFormatter.toColon(PACKAGE_MX_CORE, "IDeferredInstance");
	public static final String INTERFACE_IDEFERREDINSTANTIATIONUICOMPONENT = NameFormatter.toColon(PACKAGE_MX_CORE, "IDeferredInstantiationUIComponent");
	public static final String INTERFACE_IEVENTDISPATCHER = NameFormatter.toColon(PACKAGE_FLASH_EVENTS, "IEventDispatcher");
	public static final String INTERFACE_IFACTORY = NameFormatter.toColon(PACKAGE_MX_CORE, "IFactory");
	public static final String INTERFACE_IFLEXDISPLAYOBJECT = NameFormatter.toColon(PACKAGE_MX_CORE, "IFlexDisplayObject");
	public static final String INTERFACE_IFOCUSMANAGERCONTAINER = NameFormatter.toColon(PACKAGE_MX_MANAGERS, "IFocusManagerContainer");
	public static final String INTERFACE_IINVALIDATING = NameFormatter.toColon(PACKAGE_MX_CORE, "IInvalidating");
	public static final String INTERFACE_ILAYOUTMANAGERCLIENT = NameFormatter.toColon(PACKAGE_MX_MANAGERS, "ILayoutManagerClient");
	public static final String INTERFACE_IMANAGED = NameFormatter.toColon(PACKAGE_MX_DATA, "IManaged");
	public static final String INTERFACE_IMODULEINFO = NameFormatter.toColon(PACKAGE_MX_MODULES, "IModuleInfo");
	public static final String INTERFACE_IMXMLOBJECT = NameFormatter.toColon(PACKAGE_MX_CORE, "IMXMLObject");
	public static final String INTERFACE_IPROPERTYCHANGENOTIFIER = NameFormatter.toColon(PACKAGE_MX_CORE, "IPropertyChangeNotifier");
	public static final String INTERFACE_IRAWCHILDRENCONTAINER = NameFormatter.toColon(PACKAGE_MX_CORE, "IRawChildrenContainer");
	public static final String INTERFACE_ISIMPLESTYLECLIENT = NameFormatter.toColon(PACKAGE_MX_STYLES, "ISimpleStyleClient");
	public static final String INTERFACE_ISTYLECLIENT = NameFormatter.toColon(PACKAGE_MX_STYLES, "IStyleClient");
	public static final String INTERFACE_ISYSTEMMANAGER = NameFormatter.toColon(PACKAGE_MX_MANAGERS, "ISystemManager");
	public static final String INTERFACE_IUICOMPONENT = NameFormatter.toColon(PACKAGE_MX_CORE, "IUIComponent");

	//	classes
	public static final String CLASS_ABSTRACTSERVICE = NameFormatter.toColon(PACKAGE_MX_RPC, "AbstractService");
    public static final String CLASS_APPLICATION = NameFormatter.toColon(PACKAGE_MX_CORE, "Application");
	public static final String CLASS_APPLICATIONDOMAIN = NameFormatter.toColon(PACKAGE_FLASH_SYSTEM, "ApplicationDomain");
	public static final String CLASS_BINDINGMANAGER = NameFormatter.toColon(PACKAGE_MX_BINDING, "BindingManager");
	public static final String CLASS_CLASSFACTORY = NameFormatter.toColon(PACKAGE_MX_CORE, "ClassFactory");
	public static final String CLASS_CSSSTYLEDECLARATION = NameFormatter.toColon(PACKAGE_MX_STYLES, "CSSStyleDeclaration");
	public static final String CLASS_DEFERREDINSTANCEFROMCLASS = NameFormatter.toColon(PACKAGE_MX_CORE, "DeferredInstanceFromClass");
	public static final String CLASS_DEFERREDINSTANCEFROMFUNCTION = NameFormatter.toColon(PACKAGE_MX_CORE, "DeferredInstanceFromFunction");
	public static final String CLASS_DOWNLOADPROGRESSBAR = NameFormatter.toColon(PACKAGE_MX_PRELOADERS, "DownloadProgressBar");
	public static final String CLASS_EFFECT = NameFormatter.toColon(PACKAGE_MX_EFFECTS, "Effect");
	public static final String CLASS_EVENT = NameFormatter.toColon(PACKAGE_FLASH_EVENTS, "Event");
	public static final String CLASS_EVENTDISPATCHER = NameFormatter.toColon(PACKAGE_FLASH_EVENTS, "EventDispatcher");
	public static final String CLASS_FLEXEVENT = NameFormatter.toColon(PACKAGE_MX_EVENTS, "FlexEvent");
	public static final String CLASS_FLEXSPRITE = NameFormatter.toColon(PACKAGE_MX_CORE, "FlexSprite");
	public static final String CLASS_LOADERCONFIG = NameFormatter.toColon(PACKAGE_MX_MESSAGE_CONFIG, "LoaderConfig");
	public static final String CLASS_MANAGED = NameFormatter.toColon(PACKAGE_MX_DATA_UTILS, "Managed");
	public static final String CLASS_MODULEEVENT = NameFormatter.toColon(PACKAGE_MX_MODULES, "ModuleEvent");
	public static final String CLASS_MODULEMANAGER = NameFormatter.toColon(PACKAGE_MX_MODULES, "ModuleManager");
	public static final String CLASS_NAMESPACEUTIL = NameFormatter.toColon(PACKAGE_MX_RPC_XML, "NamespaceUtil");
	public static final String CLASS_OBJECTPROXY = NameFormatter.toColon(PACKAGE_MX_UTILS, "ObjectProxy");
	public static final String CLASS_PRELOADER = NameFormatter.toColon(PACKAGE_MX_PRELOADERS, "Preloader");
	public static final String CLASS_PROPERTYCHANGEEVENT = NameFormatter.toColon(PACKAGE_MX_EVENTS, "PropertyChangeEvent");
    public static final String CLASS_RESOURCEMANAGER = NameFormatter.toColon(PACKAGE_MX_RESOURCE, "ResourceManager");
	public static final String CLASS_REPEATER = NameFormatter.toColon(PACKAGE_MX_CORE, "Repeater");
	public static final String CLASS_STYLEEVENT = NameFormatter.toColon(PACKAGE_MX_EVENTS, "StyleEvent");
	public static final String CLASS_STYLEMANAGER = NameFormatter.toColon(PACKAGE_MX_STYLES, "StyleManager");
	public static final String CLASS_SYSTEMCHILDRENLIST = NameFormatter.toColon(PACKAGE_MX_MANAGERS, "SystemChildrenList");
	public static final String CLASS_SYSTEMMANAGER = NameFormatter.toColon(PACKAGE_MX_MANAGERS, "SystemManager");
	public static final String CLASS_SYSTEMRAWCHILDRENLIST = NameFormatter.toColon(PACKAGE_MX_MANAGERS, "SystemRawChildrenList");
	public static final String CLASS_UICOMPONENT = NameFormatter.toColon(PACKAGE_MX_CORE, "UIComponent");	//	TODO only needed for states - remove
	public static final String CLASS_UICOMPONENTDESCRIPTOR = NameFormatter.toColon(PACKAGE_MX_CORE, "UIComponentDescriptor");
	public static final String CLASS_UIDUTIL = NameFormatter.toColon(PACKAGE_MX_UTILS, "UIDUtil");
	public static final String CLASS_XML = "XML";
	public static final String CLASS_XMLLIST = "XMLList";
	public static final String CLASS_XMLNODE = NameFormatter.toColon(PACKAGE_FLASH_XML, "XMLNode");
	public static final String CLASS_XMLUTIL = NameFormatter.toColon(PACKAGE_MX_UTILS, "XMLUtil");

    //	properties
	public static final String PROP_CLASSFACTORY_GENERATOR = "generator";
	public static final String PROP_CLASSFACTORY_PROPERTIES = "properties";
	public static final String PROP_UICOMPONENT_STATES = "states";
	public static final String PROP_CONTAINER_CHILDREPEATERS = "childRepeaters";

	//	metadata
	//	TODO still lots of string constants for these in TypeTable
	public static final String MD_ACCESSIBILITYCLASS = "AccessibilityClass";
	public static final String MD_ARRAYELEMENTTYPE = "ArrayElementType";
	public static final String MD_BINDABLE = "Bindable";
	public static final String MD_CHANGEEVENT = "ChangeEvent";
	public static final String MD_COLLAPSEWHITESPACE = "CollapseWhiteSpace";
	public static final String MD_DEFAULTPROPERTY = "DefaultProperty";
	public static final String MD_DEPRECATED = "Deprecated";
	public static final String MD_EFFECT = "Effect";
	public static final String MD_EMBED = "Embed";
	public static final String MD_EVENT = "Event";
	public static final String MD_FRAME = "Frame";
	public static final String MD_ICONFILE = "IconFile";
	public static final String MD_INSPECTABLE = "Inspectable";
	public static final String MD_INSTANCETYPE = "InstanceType";
	public static final String MD_MANAGED = "Managed";
	public static final String MD_MIXIN = "Mixin";
	public static final String MD_NONCOMMITTINGCHANGEEVENT = "NonCommittingChangeEvent";
	public static final String MD_PERCENTPROXY = "PercentProxy";
	public static final String MD_REMOTECLASS = "RemoteClass";
	public static final String MD_REQUIRESLICENSE = "RequiresLicense";
	public static final String MD_RESOURCEBUNDLE = "ResourceBundle";
	public static final String MD_STYLE = "Style";
	public static final String MD_SWF = "SWF";
	public static final String MD_TRANSIENT = "Transient";

	//	metadata param names
	public static final String MDPARAM_BINDABLE_EVENT = "event";
	public static final String MDPARAM_TYPE = "type";
    public static final String MDPARAM_DESTINATION = "destination";
    public static final String MDPARAM_MODE = "mode";

    //	metadata param values
	public static final String MDPARAM_STYLE_FORMAT_COLOR = "Color";
	public static final String MDPARAM_INSPECTABLE_FORMAT_COLOR = "Color";
	public static final String MDPARAM_PROPERTY_CHANGE = "propertyChange";

    public static final String MDPARAM_MANAGED_MODE_HIERARCHICAL = "hierarchical";
    public static final String MDPARAM_MANAGED_MODE_ASSOCIATION = "association";
    public static final String MDPARAM_MANAGED_MODE_MANUAL = "manual";

    
    public static final String[] DefaultAS3Metadata = new String[] {StandardDefs.MD_BINDABLE,
    																StandardDefs.MD_MANAGED,
    																StandardDefs.MD_CHANGEEVENT,
    																StandardDefs.MD_NONCOMMITTINGCHANGEEVENT,
    																StandardDefs.MD_TRANSIENT};

	//  keywords
	public static final String NULL = "null";
	public static final String UNDEFINED = "undefined";

	/**
	 * implicit imports - not MXML support, but auto-imported to facilitate user script code.
	 */
	public static final Set implicitImports = new HashSet();
	static
	{
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_ACCESSIBILITY));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_DEBUGGER));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_DISPLAY));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_ERRORS));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_EVENTS));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_EXTERNAL));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_FILTERS));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_GEOM));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_MEDIA));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_NET));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_PRINTING));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_PROFILER));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_SYSTEM));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_TEXT));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_UI));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_UTILS));
		implicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_XML));
	}

	/**
	 * implicit imports that exist only in AIR
	 */
	public static final Set airOnlyImplicitImports = new HashSet();
	static
	{
		airOnlyImplicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_DATA));
		airOnlyImplicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_DESKTOP));
		airOnlyImplicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_FILESYSTEM));
		airOnlyImplicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_HTML));
		airOnlyImplicitImports.add(NameFormatter.toDotStar(PACKAGE_FLASH_HTML_SCRIPT));
	}

	/**
	 * standard (framework-independent) MXML support imports
	 */
	public static final Set standardMxmlImports = new HashSet();
	static
	{
		standardMxmlImports.add(NameFormatter.toDotStar(PACKAGE_MX_STYLES));
		standardMxmlImports.add(NameFormatter.toDotStar(PACKAGE_MX_BINDING));

		standardMxmlImports.add(NameFormatter.toDot(NAMESPACE_MX_INTERNAL));

		standardMxmlImports.add(NameFormatter.toDot(INTERFACE_IDEFERREDINSTANCE));	//	TODO make these conditional on use
		standardMxmlImports.add(NameFormatter.toDot(INTERFACE_IFACTORY));	//	TODO make these conditional on use
		standardMxmlImports.add(NameFormatter.toDot(INTERFACE_IPROPERTYCHANGENOTIFIER));

		standardMxmlImports.add(NameFormatter.toDot(CLASS_CLASSFACTORY));
		standardMxmlImports.add(NameFormatter.toDot(CLASS_DEFERREDINSTANCEFROMCLASS));
		standardMxmlImports.add(NameFormatter.toDot(CLASS_DEFERREDINSTANCEFROMFUNCTION));
	}

	/**
	 * AS3 reserved words, illegal as var names
	 * NOTE: this list is hand-assembled from the constants in macromedia.asc.parser.Tokens and needs to be updated
	 * manually until/unless we develop an API for getting is-reserved-word directly from the ASC scanner.
	 * Note also that "get" and "set" do not appear, as they seem to be legal AS3 variable names.
	 */
	private static final Set as3ReservedWords = new HashSet();
	static
	{
		as3ReservedWords.add("as");
		as3ReservedWords.add("break");
		as3ReservedWords.add("case");
		as3ReservedWords.add("catch");
		as3ReservedWords.add("class");
		as3ReservedWords.add("continue");
		as3ReservedWords.add("default");
		as3ReservedWords.add("do");
		as3ReservedWords.add("else");
		as3ReservedWords.add("extends");
		as3ReservedWords.add("false");
		as3ReservedWords.add("final");
		as3ReservedWords.add("finally");
		as3ReservedWords.add("for");
		as3ReservedWords.add("function");
		as3ReservedWords.add("goto");
		as3ReservedWords.add("if");
		as3ReservedWords.add("implements");
		as3ReservedWords.add("import");
		as3ReservedWords.add("in");
		as3ReservedWords.add("include");
		as3ReservedWords.add("instanceof");
		as3ReservedWords.add("interface");
		as3ReservedWords.add("is");
		as3ReservedWords.add("namespace");
		as3ReservedWords.add("new");
		as3ReservedWords.add("null");
		as3ReservedWords.add("package");
		as3ReservedWords.add("private");
		as3ReservedWords.add("protected");
		as3ReservedWords.add("public");
		as3ReservedWords.add("return");
		as3ReservedWords.add("static");
		as3ReservedWords.add("super");
		as3ReservedWords.add("switch");
		as3ReservedWords.add("synchronized");
		as3ReservedWords.add("this");
		as3ReservedWords.add("throw");
		as3ReservedWords.add("transient");
		as3ReservedWords.add("true");
		as3ReservedWords.add("try");
		as3ReservedWords.add("typeof");
		as3ReservedWords.add("use");
		as3ReservedWords.add("var");
		as3ReservedWords.add("void");
		as3ReservedWords.add("volatile");
		as3ReservedWords.add("while");
		as3ReservedWords.add("with");
	}

	/**
	 * true iff passed string is a reserved word
	 */
	public static final boolean isReservedWord(String s)
	{
		return as3ReservedWords.contains(s);
	}

	/**
	 *
	 */
	private static final Set as3BuiltInTypeNames = new HashSet();
	static
	{
		as3BuiltInTypeNames.add("String");
		as3BuiltInTypeNames.add("Boolean");
		as3BuiltInTypeNames.add("Number");
		as3BuiltInTypeNames.add("int");
		as3BuiltInTypeNames.add("uint");
		as3BuiltInTypeNames.add("Function");
		as3BuiltInTypeNames.add("Class");
		as3BuiltInTypeNames.add("Array");
		as3BuiltInTypeNames.add("Object");
		as3BuiltInTypeNames.add("XML");
		as3BuiltInTypeNames.add("XMLList");
		as3BuiltInTypeNames.add("RegExp");
	}

	/**
	 * true iff passed string is the name of a built-in type
	 */
	public static final boolean isBuiltInTypeName(String s)
	{
		return as3BuiltInTypeNames.contains(s);
	}

	/**
	 * mappings from some MXML 1.5 tags to the corresponding MXML 2.0 vanilla faceless component tag names
	 * Note: here, instead of FrameworkDefs, because the target tag names could be mapped to any classes, in or out of our framework.
	 */
	private static final Map compatTagMappings = new HashMap();
	static
	{
		compatTagMappings.put(OperationNode.class, "WebServiceOperation");
		compatTagMappings.put(MethodNode.class, "RemoteObjectOperation");
	}

	/**
	 * returns converted tag name for nodes representing migrated MXML 1.5 tags, original node name otherwise
	 */
	public static final String getConvertedTagName(Node node)
	{
		String name = (String)compatTagMappings.get(node.getClass());
		return name != null ? name : node.getLocalPart();
	}

	/**
	 * TODO formalize the type relationship between IContainer and (I)Repeater
	 */
	public static boolean isContainer(Type type)
	{
		assert type != null;
		return type.isAssignableTo(INTERFACE_ICONTAINER) || type.isAssignableTo(CLASS_REPEATER);
	}

    public static boolean isApplication(Type type)
    {
        assert type != null;
        return type.isAssignableTo(CLASS_APPLICATION);
    }

    /**
	 *
	 */
	public static boolean isIUIComponent(Type type)
	{
		assert type != null;
		return type.isAssignableTo(INTERFACE_IUICOMPONENT);
	}

	/**
	 * Note that we trigger factory coercions only on IFactory type equality, *not* assignability
	 */
	public static boolean isIFactory(Type type)
	{
		assert type != null;
		return type.getName().equals(INTERFACE_IFACTORY);
	}

	/**
	 * Note that we trigger DI coercions only on IDeferredInstance type equality, *not* assignability
	 */
	public static boolean isIDeferredInstance(Type type)
	{
		assert type != null;
		return type.getName().equals(INTERFACE_IDEFERREDINSTANCE);
	}

	/**
	 *
	 */
	public static boolean isInstanceGenerator(Type type)
	{
		assert type != null;
		return isIFactory(type) || isIDeferredInstance(type);
	}

	/**
	 *
	 */
	public static boolean isRepeater(Type type)
    {
		assert type != null;
		return type.isAssignableTo(CLASS_REPEATER);
	}

	/**
	 * for use before MXML type load
	 * TODO once mxml core types have been factored out of frameworks.swc, ideally we can fail fast if they aren't available
	 */
	public static boolean isRepeater(String cls)
	{
	    return CLASS_REPEATER.equals(cls);
	}

	/**
	 *
	 */
	public static final String getXmlBackingClassName(boolean e4x)
	{
		return e4x ? CLASS_XML : CLASS_XMLNODE;
	}

	/**
	 * Note: the assert is because IDeferredInstantiationUIComponent is expected to go away in an upcoming framework
	 * scrub. The backstop is to avoid asserting when the core framework interfaces are entirely absent.
	 * TODO post-scrub, this should be modified to check whatever subinterface of IUIComponent defines id.
	 */
	public static final boolean isIUIComponentWithIdProperty(Type type)
	{
		assert (type.getTypeTable().getType(INTERFACE_IDEFERREDINSTANTIATIONUICOMPONENT) != null) ==
				(type.getTypeTable().getType(INTERFACE_IUICOMPONENT) != null)
				: "interface " + INTERFACE_IDEFERREDINSTANTIATIONUICOMPONENT + " not found in core framework interface set";

		return type.isAssignableTo(INTERFACE_IDEFERREDINSTANTIATIONUICOMPONENT);
	}
}
