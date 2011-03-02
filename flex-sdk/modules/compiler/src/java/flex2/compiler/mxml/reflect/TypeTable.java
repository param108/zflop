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

package flex2.compiler.mxml.reflect;

import flex2.compiler.SymbolTable;
import flex2.compiler.abc.Attributes;
import flex2.compiler.abc.MetaData;
import flex2.compiler.abc.Method;
import flex2.compiler.abc.Variable;
import flex2.compiler.css.Styles;
import flex2.compiler.mxml.lang.StandardDefs;
import flex2.compiler.util.NameFormatter;
import flex2.compiler.util.NameMappings;
import flex2.compiler.util.QName;

import java.util.*;

import macromedia.asc.util.ContextStatics;

/**
 * Type and Property wrappers may be cached.
 *
 * Design Note: flex2.compiler.mxml.reflect.* interface with flex2.compiler.abc.*. That way, the MXML type system
 * is not tightly coupled with the player VM type system.
 *
 * @author Clement Wong
 */
public class TypeTable
{
	public TypeTable(SymbolTable symbolTable, NameMappings manifest)
	{
		this.symbolTable = symbolTable;
		this.manifest = manifest;
		nonRepeaters = new WeakHashMap();
		typeMap = new HashMap();

		noType          = getType(SymbolTable.NOTYPE);
		objectType      = getType(SymbolTable.OBJECT);
		stringType      = getType(SymbolTable.STRING);
		booleanType     = getType(SymbolTable.BOOLEAN);
		classType       = getType(SymbolTable.CLASS);
		functionType    = getType(SymbolTable.FUNCTION);
		numberType      = getType(SymbolTable.NUMBER);
		intType         = getType(SymbolTable.INT);
		uintType        = getType(SymbolTable.UINT);
		arrayType       = getType(SymbolTable.ARRAY);
		xmlType         = getType(SymbolTable.XML);
		xmlListType     = getType(SymbolTable.XML_LIST);
		regExpType      = getType(SymbolTable.REGEXP);
	}

	private SymbolTable symbolTable;
	private NameMappings manifest;
	public final Type noType, stringType, booleanType, classType, functionType, numberType, intType, uintType, arrayType, objectType, xmlType, xmlListType, regExpType;
	private Map typeMap, nonRepeaters;

	/**
	 * Use <namespaceURI:localPart> to lookup a component implementation
	 */
	public Type getType(String namespaceURI, String localPart)
	{
		// use manifest to lookup classname based on namespaceURI and localPart. classname is fully qualfied.
		String className = manifest.resolveClassName(namespaceURI, localPart);
		// C: should check the type visibility here...
		return getType(className);
	}

	public Type getType(QName qName)
	{
		return getType(qName.getNamespace(), qName.getLocalPart());
	}

	/**
	 * Use the specified fully-qualified class name to lookup a component implementation
	 */
	public Type getType(String className)
	{
		Type t = (Type) typeMap.get(className);

		if (t == null)
		{
			// use symbolTable to lookup Class.
			flex2.compiler.abc.Class classInfo = symbolTable.getClass(className);
			if (classInfo != null)
			{
				t = new TypeHelper(classInfo);
				typeMap.put(className, t);
			}
		}

		return t;
	}

	/**
	 * Look up a globally-defined style property
	 */
	public Style getStyle(String styleName)
	{
		MetaData md = symbolTable.getStyle(styleName);
		return md == null ? null : new StyleHelper(styleName,
												   md.getValue("type"),
												   md.getValue("enumeration"),
												   md.getValue("format"),
												   md.getValue("inherit"),
												   md.getValue(Deprecated.DEPRECATED_MESSAGE),
												   md.getValue(Deprecated.DEPRECATED_REPLACEMENT),
												   md.getValue(Deprecated.DEPRECATED_SINCE));
	}

    public Styles getStyles()
    {
        return symbolTable.getStyles();
    }

	public ContextStatics getPerCompileData()
	{
		return symbolTable.perCompileData;
	}

	// Helper classes

	private final class TypeHelper implements Type
	{
		private TypeHelper(flex2.compiler.abc.Class classInfo)
		{
			assert classInfo != null;
			this.classInfo = classInfo;
		}

		private flex2.compiler.abc.Class classInfo;
		private EventListHelper events;
		private List effects, styles;

		public boolean equals(Object obj)
		{
			if (obj == this)
			{
				return true;
			}
			else if (obj instanceof Type)
			{
				return getName().equals(((Type) obj).getName());
			}
			else
			{
				return false;
			}
		}

		public TypeTable getTypeTable()
		{
			return TypeTable.this;
		}

		/**
		 * Type name. AS3-compatible fully-qualified class name.
		 */
		public String getName()
		{
			return classInfo.getName();
		}

		/**
		 * Super type
		 */
		public Type getSuperType()
		{
			return classInfo.getSuperTypeName() != null ? getType(classInfo.getSuperTypeName()) : null;
		}

		/**
		 * Interfaces
		 */
		public Type[] getInterfaces()
		{
			String[] ifaces = classInfo.getInterfaceNames();

			if (ifaces != null)
			{
				Type[] types = new Type[ifaces.length];
				for (int i = 0, length = types.length; i < length; i++)
				{
					types[i] = getType(ifaces[i]);
				}
				return types;
			}
			else
			{
				return null;
			}
		}

		/**
		 * Property. variables, getters, setters, etc.
		 * Searches SymbolTable.VISIBILITY_NAMESPACES: public protected internal private
		 */
		public Property getProperty(String name)
		{
			return getProperty(SymbolTable.VISIBILITY_NAMESPACES, name);
		}

		/**
		 * Property. variables, getters, setters, etc.
		 * Searches specified namespace
		 */
		public Property getProperty(String namespace, String name)
		{
			return getProperty(new String[]{namespace}, name);
		}

		/**
		 * Property. variables, getters, setters, etc.
		 * Searches specified namespaces
		 */
		public Property getProperty(String[] namespaces, String name)
		{
			flex2.compiler.abc.Class cls = classInfo, superClass = null;

			// walk the superclass chain for the specified property...
			while (cls != null)
			{
				Variable var = cls.getVariable(namespaces, name, false);
				if (var != null)
				{
					Attributes attrs = var.getAttributes();
					if (attrs == null || !attrs.hasStatic())
					{
						// found the property as a variable...
						return new PropertyHelper(var);
					}
					else
					{
						superClass = symbolTable.getClass(cls.getSuperTypeName());
					}
				}
				else
				{
					Method setter = cls.getSetter(namespaces, name, false);
					Method getter = cls.getGetter(namespaces, name, false);
					if (setter != null && getter != null)
					{
						// found the property as a pair of getter and setter...
						return new PropertyHelper(setter, getter);
					}

					superClass = symbolTable.getClass(cls.getSuperTypeName());

					if (setter != null && superClass != null)
					{
						// search for a superclass getter before creating PropertyHelper.
						getter = findGetter(superClass, setter);
						return new PropertyHelper(setter, getter);
					}
					else if (getter != null && superClass != null)
					{
						// search for a superclass setter before creating PropertyHelper.
						setter = findSetter(superClass, getter);
						return new PropertyHelper(setter, getter);
					}
				}
				cls = superClass;
			}

			return null;
		}

		/**
		 *
		 */
		public boolean hasStaticMember(String name)
		{
			flex2.compiler.abc.Class cls = classInfo;

			// walk the superclass chain for the specified property...
			while (cls != null)
			{
				Variable var = cls.getVariable(SymbolTable.VISIBILITY_NAMESPACES, name, false);
				if (var != null)
				{
					Attributes attrs = var.getAttributes();
					if (attrs != null && attrs.hasStatic())
					{
						return true;
					}
				}
				else
				{
					Method method = cls.getMethod(new String[] {SymbolTable.publicNamespace}, name, false);
					if (method != null)
					{
						Attributes attrs = method.getAttributes();
						if (attrs != null && attrs.hasStatic())
						{
							return true;
						}
					}
				}

				cls = symbolTable.getClass(cls.getSuperTypeName());
			}

			return false;
		}

		/**
		 * [Event]
		 * NOTE a) for now, we assume that Event's type attribute (if specified) is either fully qualified,
		 * 	***in internal format***, or is in flash.core (!)
		 * NOTE b) for now, we assume that Event's class can be found within current type set (!)
		 * NOTE c) for now, we silently revert to flash.core.Event if (a) or (b) are false (!)
		 * TODO fix (a), (b), (c) above, following ASC rearchitecture. EventExtension should a) try resolving unqualified
		 * type against current imports; b) add import if implied by qualified type; c) logError if a/b fail
		 */
		public Event getEvent(String name)
		{
			if (events == null)
			{
				events = new EventListHelper(classInfo.getMetaData("Event", false));
			}

			Event e = events.getEvent(name);

			if (e != null)
			{
				return e;
			}
			else
			{
				Type st = getSuperType();
				return (st != null) ? st.getEvent(name) : null;
			}
		}

		/**
		 * [Effect]
		 */
		public Effect getEffect(String name)
		{
			if (effects == null)
			{
				effects = classInfo.getMetaData("Effect", true);
			}

			for (int i = 0, length = effects.size(); i < length; i++)
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) effects.get(i);
				if (name.equals(md.getValue(0)))
				{
					return new EffectHelper(name,
											md.getValue("event"),
											md.getValue(Deprecated.DEPRECATED_MESSAGE),
											md.getValue(Deprecated.DEPRECATED_REPLACEMENT),
											md.getValue(Deprecated.DEPRECATED_SINCE));
				}
			}

			return null;
		}

		/**
		 * [Style]
		 */
		public Style getStyle(String name)
		{
			if (styles == null)
			{
				styles = classInfo.getMetaData("Style", true);
			}

			for (int i = 0, length = styles.size(); i < length; i++)
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) styles.get(i);
				if (name.equals(md.getValue("name")))
				{
					return new StyleHelper(name,
										   md.getValue("type"),
										   md.getValue("enumeration"),
										   md.getValue("format"),
										   md.getValue("inherit"),
										   md.getValue(Deprecated.DEPRECATED_MESSAGE),
										   md.getValue(Deprecated.DEPRECATED_REPLACEMENT),
										   md.getValue(Deprecated.DEPRECATED_SINCE));
				}
			}

			return null;
		}

		/**
		 * [DefaultProperty]
		 * Note: returns name as given in metadata - may or may not correctly specify a public property
		 * TODO validate: should error when [DefaultProperty] is defined, but doesn't yield a default property
		 */
		public Property getDefaultProperty()
		{
			List metadata = classInfo.getMetaData("DefaultProperty", true);
			if (metadata.size() > 0)
			{
				String defaultPropertyName = ((flex2.compiler.abc.MetaData) metadata.get(0)).getValue(0);
				return defaultPropertyName != null ? getProperty(defaultPropertyName) : null;
			}
			else
			{
				return null;
			}
		}

		/**
		 * [Obsolete]
		 */
		public boolean hasObsolete(String name)
		{
			List metadata = classInfo.getMetaData("Obsolete", false);
			for (int i = 0, length = metadata.size(); i < length; i++)
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) metadata.get(i);
				if (name.equals(md.getValue(0)))
				{
					return true;
				}
			}

			return false;
		}

		public int getMaxChildren()
		{
			List metadata = classInfo.getMetaData("MaxChildren", true);
			if (!metadata.isEmpty())
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) metadata.get(0);
				return Integer.parseInt(md.getValue(0));
			}

			return 0;
		}

        public String getLoaderClass()
        {
            List metadata = classInfo.getMetaData("Frame", true);
            if (!metadata.isEmpty())
            {
                flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) metadata.get(0);

                return md.getValue( "factoryClass" );
            }
            return null;
        }

		/**
		 * Dynamic type
		 */
		public boolean hasDynamic()
		{
			return (classInfo.getAttributes() != null) && classInfo.getAttributes().hasDynamic();
		}

		/**
		 * Is this type a subclass of baseType?
		 *
		 * C: Note that this implementation of isAssignableTo *might* run into infinite recursion if there is a
		 *    circular reference in the supertype/interface hierarchies. The type table is not expecting stuff
		 *    registered in SymbolTable to have compile problems.
		 *
		 *    If we want to stop infinite recursion in this code, we just need to pass a HashSet down the recursion,
		 *    detect when the code is processing something that already exists in the HashSet.
		 */
		public boolean isSubclassOf(Type baseType)
		{
			if (baseType == this)
			{
				return true;
			}
			else if (baseType == noType)
			{
				return false;
			}
			else if (baseType != null)
			{
				return isSubclassOf(baseType.getName());
			}
			else
			{
				return false;
			}
		}

		public boolean isSubclassOf(String baseName)
		{
			if (SymbolTable.NOTYPE.equals(baseName))
			{
				return false;
			}
			else
			{
				return isAssignableTo(baseName);
			}
		}

		public boolean isAssignableTo(Type baseType)
		{
			if (baseType == this || baseType == noType)
			{
				return true;
			}
			else if (baseType != null)
			{
				return isAssignableTo(baseType.getName());
			}
			else
			{
				return false;
			}
		}

		public boolean isAssignableTo(String baseName)
		{
			String name = getName();

			// C: if this type is not assignable to Repeater, return false immediately.
			if (StandardDefs.CLASS_REPEATER.equals(baseName) && TypeTable.this.nonRepeaters.containsKey(name))
			{
				return false;
			}

			if (SymbolTable.NOTYPE.equals(baseName) || name.equals(baseName))
			{
				return true;
			}

			Type superType = getSuperType();

			if (superType != null && superType.getName().equals(baseName))
			{
				return true;
			}

			Type[] interfaces = getInterfaces();

			for (int i = 0, length = (interfaces == null) ? 0 : interfaces.length; i < length; i++)
			{
				if (baseName != null && interfaces[i] != null && baseName.equals(interfaces[i].getName()))
				{
					return true;
				}
			}

			if (superType != null && superType.isAssignableTo(baseName))
			{
				return true;
			}

			for (int i = 0, length = (interfaces == null) ? 0 : interfaces.length; i < length; i++)
			{
				if (interfaces[i] != null && interfaces[i].isAssignableTo(baseName))
				{
					return true;
				}
			}

			// C: if this type is not assignable to Repeater, remember it.
			if (StandardDefs.CLASS_REPEATER.equals(baseName))
			{
				TypeTable.this.nonRepeaters.put(name, name);
			}

			return false;
		}

		// lookup superclass chain until it finds a matching getter...

		private Method findGetter(flex2.compiler.abc.Class cls, Method setter)
		{
			while (cls != null)
			{
				Method getter = cls.getGetter(new String[] {SymbolTable.publicNamespace}, setter.getName(), true);
				if (getter != null)
				{
					return getter;
				}
				else
				{
					cls = symbolTable.getClass(cls.getSuperTypeName());
				}
			}
			return null;
		}

		private Method findSetter(flex2.compiler.abc.Class cls, Method getter)
		{
			while (cls != null)
			{
				Method setter = cls.getSetter(new String[] {SymbolTable.publicNamespace}, getter.getName(), true);
				if (setter != null)
				{
					return setter;
				}
				else
				{
					cls = symbolTable.getClass(cls.getSuperTypeName());
				}
			}
			return null;
		}
	}

	private final class EventListHelper
	{
		private EventListHelper(List events)
		{
			if (events.size() == 0) return;

			eventTypes = new HashMap(events.size());

			for (int i = 0, length = events.size(); i < length; i++)
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) events.get(i);

				String name = md.getValue("name");
				String typeName = md.getValue("type");
				
				if (name != null)
				{
					if (typeName == null)
					{
						// [Event(name="...")]
						typeName = SymbolTable.EVENT;
					}
					else
					{
						// [Event(name="...",type="...")]
						typeName = NameFormatter.toColon(typeName);
					}
				}
				else
				{
					// [Event("name")]
					name = md.getValue(0);
					typeName = SymbolTable.EVENT;
				}

				if (typeName != null)
				{
					eventTypes.put(name, new EventHelper(name, typeName,
														 md.getValue(Deprecated.DEPRECATED_MESSAGE),
														 md.getValue(Deprecated.DEPRECATED_REPLACEMENT),
														 md.getValue(Deprecated.DEPRECATED_SINCE)));
				}
			}
		}

		private Map eventTypes;

		Event getEvent(String name)
		{
			return (eventTypes == null) ? null : (Event) eventTypes.get(name);
		}
	}

	private final class PropertyHelper implements Property
	{
		private PropertyHelper(Variable var)
		{
			this.var = var;

			readOnly = getAttributes() != null && getAttributes().hasConst();
			writeOnly = false;
		}

		PropertyHelper(Method setter, Method getter)
		{
			this.setter = setter;
			this.getter = getter;

			readOnly = setter == null && getter != null;
			writeOnly = setter != null && getter == null;
		}

		private boolean readOnly;
		private boolean writeOnly;

		private Variable var;
		private Method setter, getter;

		public boolean equals(Object obj)
		{
			if (obj instanceof Property)
			{
				Property p = (Property) obj;
				// FIXME
				return getName().equals(p.getName());
			}
			else
			{
				return false;
			}
		}

		/**
		 * Property name
		 */
		public String getName()
		{
			if (var != null)
			{
				return var.getName();
			}
			else if (setter != null)
			{
				return setter.getName();
			}
			else // if (getter != null)
			{
				return getter.getName();
			}
		}

		/**
		 * Type.
		 *
		 * If this is a getter, the returned value is the getter's return type.
		 * If this is a setter, the returned value is the type of the input argument of the setter.
		 */
		public Type getType()
		{
			String className;

			if (var != null)
			{
				className = var.getTypeName();
			}
			else if (setter != null)
			{
				className = setter.getParameterTypeNames()[0];
			}
			else // if (getter != null)
			{
				className = getter.getReturnTypeName();
			}

			return TypeTable.this.getType(className);
		}

		/**
		 * Is this read only?
		 */
		public boolean readOnly()
		{
			return readOnly;
		}

		/**
		 * Is this write only?
		 */
		public boolean writeOnly()
		{
			return writeOnly;
		}

		/**
		 *
		 */
		public boolean hasStatic()
		{
			Attributes attrs = getAttributes();
			return attrs != null && attrs.hasStatic();
		}

		/**
		 * Does this property override supertype's property?
		 */
		public boolean hasOverride()
		{
			Attributes attrs = getAttributes();
			return attrs != null && attrs.hasOverride();
		}

		/**
		 *
		 */
		public boolean hasPrivate()
		{
			Attributes attrs = getAttributes();
			return attrs != null && attrs.hasPrivate();
		}

		/**
		 *
		 */
		public boolean hasPublic()
		{
			Attributes attrs = getAttributes();
			return attrs != null && attrs.hasPublic();
		}

		/**
		 *
		 */
		public boolean hasProtected()
		{
			Attributes attrs = getAttributes();
			return attrs != null && attrs.hasProtected();
		}

		/**
		 * Note that having no visibility specifiers at all, which is (currently) defined as implying the default
		 * internal namespace, produces attrs == null in the ASC data structure. This seems fragile.
		 */
		public boolean hasInternal()
		{
			Attributes attrs = getAttributes();
			return attrs == null || attrs.hasInternal();
		}

		/**
		 *
		 */
		public boolean hasNamespace(String nsValue)
		{
			Attributes attrs = getAttributes();
			return attrs != null && attrs.hasNamespace(nsValue);
		}

		/**
		 *
		 */
		private Attributes getAttributes()
		{
			return var != null ? var.getAttributes() :
				setter != null ? setter.getAttributes() :
				getter.getAttributes();
		}

		/**
		 *
		 */
		public Inspectable getInspectable()
		{
			List mdList = getMetaDataList(Inspectable.INSPECTABLE);
			if (mdList != null)
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) mdList.get(0);

				return new InspectableHelper(md.getValue(Inspectable.ENUMERATION),
											 md.getValue(Inspectable.DEFAULT_VALUE),
											 md.getValue(Inspectable.IS_DEFAULT),
											 md.getValue(Inspectable.CATEGORY),
											 md.getValue(Inspectable.IS_VERBOSE),
											 md.getValue(Inspectable.TYPE),
											 md.getValue(Inspectable.OBJECT_TYPE),
											 md.getValue(Inspectable.ARRAY_TYPE),
											 md.getValue(Inspectable.ENVIRONMENT),
											 md.getValue(Inspectable.FORMAT));
			}
			else
			{
				return null;
			}
		}

		public Deprecated getDeprecated()
		{
			List mdList = getMetaDataList(Deprecated.DEPRECATED);
			if (mdList != null)
			{
				flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) mdList.get(0);

				String replacement = md.getValue(Deprecated.REPLACEMENT);
				String message     = md.getValue(Deprecated.MESSAGE);
				String since	   = md.getValue(Deprecated.SINCE);

                // grab whatever string /was/ provided: [Deprecated("foo")]
				if ((replacement == null) &&
                        (message == null) &&
                          (since == null) && (md.count() > 0))
				{
					message = md.getValue(0);
				}
                
				return new DeprecatedHelper(replacement, message, since);
			}
			else
			{
				return null;
			}
		}

		/**
		 * [ChangeEvent]
		 * TODO why just on var? should it be returned for getter/setter props?
		 */
		public boolean hasChangeEvent(String name)
		{
			if (var != null)
			{
				List mdList = var.getMetaData(StandardDefs.MD_CHANGEEVENT);
				if (mdList != null)
				{
					for (int i = 0, size = mdList.size(); i < size; i++)
					{
						flex2.compiler.abc.MetaData md = (flex2.compiler.abc.MetaData) mdList.get(i);
						if (name.equals(md.getValue(0)))
						{
							return true;
						}
					}
				}
			}

			return false;
		}

		/**
		 * [ArrayElementType]
		 */
		public String getArrayElementType()
		{
			List mdList = getMetaDataList(StandardDefs.MD_ARRAYELEMENTTYPE);
			if (mdList != null)
			{
				MetaData metaData = (MetaData) mdList.get(0);
				if (metaData.count() > 0)
				{
					String typeName = metaData.getValue(0);
					return typeName == null ? null : NameFormatter.toColon(typeName);
				}
			}

			return null;
		}

		/**
		 * [PercentProxy]
		 */
		public String getPercentProxy()
		{
			List metaDataList = getMetaDataList(StandardDefs.MD_PERCENTPROXY);
			if (metaDataList != null && !metaDataList.isEmpty())
			{
				MetaData metaData = (MetaData) metaDataList.get(0);
				if (metaData.count() > 0)
				{
					return metaData.getValue(0);
				}
			}

			return null;
		}

		/**
		 * [InstanceType]
		 */
		public String getInstanceType()
		{
			List mdList = getMetaDataList(StandardDefs.MD_INSTANCETYPE);
			if (mdList != null)
			{
				MetaData metaData = (MetaData) mdList.get(0);
				if (metaData.count() > 0)
				{
					return NameFormatter.toColon(metaData.getValue(0));
				}
			}

			return null;
		}

		/**
		 * [CollapseWhiteSpace]
		 */
		public boolean collapseWhiteSpace()
		{
			return getMetaDataList(StandardDefs.MD_COLLAPSEWHITESPACE) != null;
		}

		/**
		 * property metadata lookup: we will have either a non-null var, or <getter, setter> with one or both non-null.
		 * Return value is guaranteed non-empty if non-null.
		 * Note: validation should ensure that each metadata name occurs at most once on the latter pair.
		 */
		private List getMetaDataList(String name)
		{
			List mdList = null;

			if (var != null)
			{
				mdList = var.getMetaData(name);
			}
			else
			{
				if (getter != null)
				{
					mdList = getter.getMetaData(name);
				}

				if (mdList == null && setter != null)
				{
					mdList = setter.getMetaData(name);
				}
			}

			return mdList != null && mdList.size() > 0 ? mdList : null;
		}
	}

	private final class EventHelper implements Event
	{
		private final String name;
		private final String typeName;
		private Type type;
		private final String message;
		private final String replacement;
		private final String since;

		private EventHelper(String name, String typeName, String message, String replacement, String since)
		{
			this.name = name;
			this.typeName = typeName;
			this.message = message;
			this.replacement = replacement;
			this.since = since;
		}

		public String getName()
		{
			return name;
		}

		public String getTypeName()
		{
			return typeName;
		}

		public Type getType()
		{
			return type != null ? type : (type = TypeTable.this.getType(typeName));
		}
		
		public String getDeprecatedMessage()
		{
			return message;
		}

		public String getDeprecatedReplacement()
		{
			return replacement;
		}

		public String getDeprecatedSince()
		{
			return since;
		}
	}

	private final class StyleHelper implements Style
	{
		/**
		 * NOTE for now, we assume that specified type name (if specified) is either fully qualified,
		 * TODO fix this, following ASC rearchitecture. StyleExtension should a) try resolving unqualified
		 * type against current imports; b) add import if implied by qualified type; c) logError if a/b fail
		 * TODO make StyleHelper and EventHelper consistent w.r.t. type member
		 */
		private StyleHelper(String name, String type, String enumeration, String format, String inherit,
							String message, String replacement, String since)
		{
			if (type == null)
			{
				type = (enumeration == null) ? SymbolTable.OBJECT : SymbolTable.STRING;
			}
			else
			{
				//	HACK: for now, if declared type isn't found... (no more actionscript.lang)
				//	TODO: this should no longer be necessary given metadata scanning in as3.SyntaxTreeEvaluator, but
				//	leaving it in place for now out of cowardice.
				Type t = TypeTable.this.getType(NameFormatter.toColon(type));
				if (t == null)
				{
					type = SymbolTable.OBJECT;
				}
				//	/HACK
			}

			this.name = name;
			this.type = NameFormatter.toColon(type).intern();
			if (enumeration != null)
			{
				StringTokenizer t = new StringTokenizer(enumeration, ",");
				this.enumeration = new String[t.countTokens()];
				for (int i = 0; t.hasMoreTokens(); i++)
				{
					this.enumeration[i] = t.nextToken();
				}
			}
			this.format = format;
			isInherit = "yes".equalsIgnoreCase(inherit);
			
			this.message = message;
			this.replacement = replacement;
			this.since = since;
		}

		private String name;
		private String type;
		private String[] enumeration;
		private String format;
		private boolean isInherit;
		private String message;
		private String replacement;
		private String since;


		public String getName()
		{
			return name;
		}

		public Type getType()
		{
			return TypeTable.this.getType(type);
		}

		public String[] getEnumeration()
		{
			return enumeration;
		}

		public String getFormat()
		{
			return format;
		}

		public boolean isInherit()
		{
			return isInherit;
		}

		public String getDeprecatedMessage()
		{
			return message;
		}

		public String getDeprecatedReplacement()
		{
			return replacement;
		}

		public String getDeprecatedSince()
		{
			return since;
		}
	}

	private final class EffectHelper implements Effect
	{
		private final String name;
		private final String event;
		private final String message;
		private final String replacement;
		private final String since;

		private EffectHelper(String name, String event, String message, String replacement, String since)
		{
			this.name = name;
			this.event = event;
			this.message = message;
			this.replacement = replacement;
			this.since = since;
		}

		public String getName() { return name; }

		public String getEvent() { return event; }
		
		public String getDeprecatedMessage() { return message; }

		public String getDeprecatedReplacement() { return replacement; }

		public String getDeprecatedSince() { return since; }
	}

	private final class InspectableHelper implements Inspectable
	{
		private InspectableHelper(String enumeration,
								  String defaultValue,
								  String isDefault,
								  String category,
								  String isVerbose,
								  String type,
								  String objectType,
								  String arrayType,
								  String environment,
								  String format)
		{
			if (enumeration != null)
			{
				StringTokenizer t = new StringTokenizer(enumeration, ",");
				this.enumeration = new String[t.countTokens()];
				for (int i = 0; t.hasMoreTokens(); i++)
				{
					this.enumeration[i] = t.nextToken();
				}
			}

			this.defaultValue = defaultValue;
			this.isDefault = "yes".equalsIgnoreCase(isDefault);
			this.category = category;
			this.isVerbose = "yes".equalsIgnoreCase(isVerbose);
			this.type = type;
			this.objectType = objectType;
			this.arrayType = arrayType;
			this.environment = environment;
			this.format = format;
		}

		private String[] enumeration;
		private String defaultValue;
		private boolean isDefault;
		private String category;
		private boolean isVerbose;
		private String type;
		private String objectType;
		private String arrayType;
		private String environment;
		private String format;

		/**
		 * enumeration
		 */
		public String[] getEnumeration()
		{
			return enumeration;
		}

		/**
		 * default value
		 */
		public String getDefaultValue()
		{
			return defaultValue;
		}

		/**
		 * default?
		 */
		public boolean isDefault()
		{
			return isDefault;
		}

		/**
		 * category
		 */
		public String getCategory()
		{
			return category;
		}

		/**
		 * verbose?
		 */
		public boolean isVerbose()
		{
			return isVerbose;
		}

		/**
		 * type
		 */
		public String getType()
		{
			return type;
		}

		/**
		 * object type
		 */
		public String getObjectType()
		{
			return objectType;
		}

		/**
		 * array type
		 */
		public String getArrayType()
		{
			return arrayType;
		}

		/**
		 * environment
		 */
		public String getEnvironment()
		{
			return environment;
		}

		/**
		 * format
		 */
		public String getFormat()
		{
			return format;
		}
	}

	private final class DeprecatedHelper implements Deprecated
	{
		private DeprecatedHelper(String replacement, String message, String since)
		{
			this.replacement = replacement;
			this.message = message;
			this.since = since;
		}

		private String replacement;
		private String message;
		private String since;

		public String getReplacement()
		{
			return replacement;
		}

		public String getMessage()
		{
			return message;
		}
		
		public String getSince()
		{
			return since;
		}
	}

}
