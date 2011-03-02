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

package flex2.compiler.config;

import flash.util.Trace;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.io.UnsupportedEncodingException;

import flex2.compiler.io.VirtualFile;

/**
 * @author Roger Gonzalez
 *
 * The basic idea here is to let you keep all your configuration knowledge in your configuration object,
 * and to automate as much as possible.  Reflection is used to convert public fields and setters on your
 * configuration object into settable vars.  There are a few key concepts:
 *
 * - You should be able to configure absolutely any object.
 * - Child configuration variables in your config become a dotted hierarchy of varnames
 * - All sources of configuration data are buffered and merged (as string var/vals) before
 *   committing to the final configuration.  This class acts as the buffer.
 * - Hyphenated variables (i.e. "some-var") are automatically configured by calling your matching setter (i.e. setSomeVar)
 * - Implementing an getSomeVarInfo() method on your class lets you set up more complicated config objects
 * - You can make variables depend on other variables having been set first.  This lets you set a
 *   root directory in one var and then use its value in another.
 * - Per-variable validation can be performed in setters.  Overall validation should take place
 *   as a post-process step.
 * - You can keep ConfigurationBuffers around and merge multiple buffers together before committing.
 *   Most recent definitions always win.
 *
 * The contract with your configuration class:
 * - You must provide a method with the signature "void setYourVar(ConfigurationValue val)" to set your config var.
 *   Your setter method should accept either a single arg of type List or String[], or else an arglist of
 *   simple types.  For example "void myvar(int a, boolean b, String c")".
 * - You can implement a function with the signature "int yourvar_argcount()" to require a different number
 *   of arguments.  This limit will be enforced by configurators (command line, file, etc.)
 * - If you provide a setter and explicit parameters (i.e. not List or String[]) the number of arguments
 *   will be automatically determined.
 * - Each argument to your configuration variable is assumed to have a (potentially non-unique) name.  The default is
 *   the simple type of the argument (boolean, int, string).  If the var takes an undetermined number of args via
 *   List or String[], the argname defaults to string.
 * - You can implement a function with the signature "String yourvar_argnames(int)" to provide names
 *   for each of the parameters.  The integer passed in is the argument number.  Return the same name
 *   (i.e. "item") for infinite lists.
 * - You can implement a function with the signature "String[] yourvar_deps()" to provide a list
 *   of other prerequisites for this var.  You will be guaranteed that the deps are committed before
 *   your var, or else a configurationexception will be thrown if a prerequsite was unset.  (Note that
 *   infinite cycles are not checked, so be careful.)
 *
 */
public final class ConfigurationBuffer
{
    public ConfigurationBuffer( Class configClass )
    {
        this( configClass, new HashMap() );
    }

    public ConfigurationBuffer( Class configClass, Map aliases )
    {
    	this(configClass, aliases, null);
    }

    /**
     * Create a configuration buffer with an optional filter. The filter can be used
     * to remove unwanted options from a super class.
     *  
     * @param filter if null there is no filter, otherwise the set of configuration options
     * 				 is filtered.
	 */
    public ConfigurationBuffer( Class configClass, Map aliases, ConfigurationFilter filter )
    {
        this.configClass = configClass;
        this.varMap = new HashMap(); // var to args
        this.committed = new HashSet(); // vars

        loadCache( configClass, null, filter );
        assert ( varCache.size() > 0 ) : "coding error: nothing was configurable in the provided object!";
        for (Iterator it = aliases.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry e = (Map.Entry) it.next();
            addAlias( (String) e.getKey(), (String) e.getValue() );
        }
    }

    public ConfigurationBuffer( ConfigurationBuffer copyFrom, boolean copyCommitted )
    {
        this.configClass = copyFrom.configClass;
        this.varMap = new HashMap( copyFrom.varMap );
        this.committed = copyCommitted? new HashSet( copyFrom.committed ) : new HashSet();
        this.varCache = copyFrom.varCache;     // doesn't change after creation
        this.childCache = copyFrom.childCache; // doesn't change after creation;
        this.varList = copyFrom.varList;       // doesn't change after creation
        this.tokens = new HashMap( copyFrom.tokens );
    }

    public void setVar( String var, String val, String source, int line ) throws ConfigurationException
    {
        List list = new LinkedList();
        list.add( val );
        setVar( var, list, source, line, null, false );
    }

    public void setVar( String var, List vals, String source, int line ) throws ConfigurationException
    {
        setVar( var, vals, source, line, null, false );
    }

    public void setVar( String avar, List vals, String source, int line, String contextPath, boolean append ) throws ConfigurationException
    {
        String var = unalias( avar );
        if (!isValidVar( var ))
            throw new ConfigurationException.UnknownVariable( var, source, line );

        int argCount = getVarArgCount( var );

        // -1 means unspecified length, its up to the receiving setter to validate.
        if (argCount != -1)
        {
            if (vals.size() != argCount)
            {
                throw new ConfigurationException.IncorrectArgumentCount( argCount, // expected
                                                                         vals.size(), //passed
                                                                         var, source, line );
            }
        }

        ConfigurationValue val = new ConfigurationValue( this, var,
                                                         vals, //processValues( var, vals, source, line ),
                                                         source, line, contextPath );
        storeValue( var, val, append );
        committed.remove( var );
    }

    public void clearVar( String avar, String source, int line ) throws ConfigurationException
    {
        String var = unalias( avar );
        if (!isValidVar( var ))
            throw new ConfigurationException.UnknownVariable( var, source, line );
        varMap.remove( var );
        committed.remove( var );
    }

    public void clearSourceVars( String source )
    {
        List remove = new LinkedList();
        for (Iterator it = varMap.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();
            String var = (String) e.getKey();
            List vals = (List) e.getValue();

            List newvals = new LinkedList();
            for (Iterator vi = vals.iterator(); vi.hasNext();)
            {
                ConfigurationValue val = (ConfigurationValue) vi.next();

                if (!val.getSource().equals( source ))
                {
                    newvals.add( val );
                }
            }
            if (newvals.size() > 0)
                varMap.put( var, newvals );
            else
                remove.add( var );
        }
        for (Iterator it = remove.iterator(); it.hasNext();)
        {
            varMap.remove( it.next() );
        }
    }

    public List processValues( String var, List args, String source, int line ) throws ConfigurationException
    {
        List newArgs = new LinkedList();
        for (Iterator it = args.iterator(); it.hasNext();)
        {
            String arg = (String) it.next();

            int depth = 100;
            while (depth-- > 0)
            {
                int o = arg.indexOf( "${" );
                if (o == -1)
                    break;

                int c = arg.indexOf( "}", o );

                if (c == -1)
                {
                    throw new ConfigurationException.Token(ConfigurationException.Token.MISSING_DELIMITER,
                                                           null, var, source, line );
                }
                String token = arg.substring( o + 2, c );
                String value = getToken( token );

                if (value == null)
                {
                    if (false && isValidVar( token ))
                    {
                        if (varMap.containsKey( token ))
                        {
                            List vals = (LinkedList) varMap.get( token );
                            assert ( vals.size() > 0 );
                            if (vals.size() > 1)
                            {
                                throw new ConfigurationException.Token( ConfigurationException.Token.MULTIPLE_VALUES,
                                                                        token, var, source, line );
                            }
                            ConfigurationValue first = (ConfigurationValue) vals.get( 0 );
                            if (first.getArgs().size() != 1)
                            {
                                throw new ConfigurationException.Token( ConfigurationException.Token.MULTIPLE_VALUES,
                                                                        token, var, source, line );

                            }
                            value = (String) first.getArgs().get( 0 );
                        }
                    }
                    if (value == null)

                    {
                        throw new ConfigurationException.Token( ConfigurationException.Token.UNKNOWN_TOKEN,
                                                                token, var, source, line );
                    }

                }
                arg = arg.substring( 0, o ) + value + arg.substring( c + 1 );

            }
            if (depth == 0)
            {
                throw new ConfigurationException.Token( ConfigurationException.Token.RECURSION_LIMIT,
                                                        null, var, source, line );
            }

            newArgs.add( arg );
        }
        return newArgs;
    }

    public void setToken( String token, String value )
    {
        tokens.put( token, value );
    }

    public String getToken( String token )
    {
        if (tokens.containsKey( token ))
            return (String) tokens.get( token );
        else 
        {
            try
            {
                return System.getProperty( token );
            }
            catch (SecurityException se)         
            {
                return null;
            }
        }
    }

    private void storeValue( String avar, ConfigurationValue val, boolean append ) throws ConfigurationException
    {
        String var = unalias( avar );
        ConfigurationInfo info = getInfo( var );

        List vals;
        if (varMap.containsKey( var ))
        {
            vals = (LinkedList) varMap.get( var );
            assert ( vals.size() > 0 );
            ConfigurationValue first = (ConfigurationValue) vals.get( 0 );
            if (!append && !first.getSource().equals( val.getSource() ))
                vals.clear();
            else if (!info.allowMultiple())
                throw new ConfigurationException.IllegalMultipleSet(
                                                  var,
                                                  val.getSource(), val.getLine() );
        }
        else
        {
            vals = new LinkedList();
            varMap.put( var, vals );
        }
        vals.add( val );
    }

    public List getVar( String avar )
    {
        String var = unalias( avar );
        return (List) varMap.get( var );
    }

    public Iterator getVarIterator()
    {
        return varCache.keySet().iterator();
    }

    private Iterator getSetVarIterator()
    {
        return varMap.keySet().iterator();
    }

    public void merge( ConfigurationBuffer other )
    {
        assert ( configClass == other.configClass );
        varMap.putAll( other.varMap );
        committed.addAll( other.committed );
    }

    public void mergeChild( String prefix, ConfigurationBuffer child )
    {
        assert isChildConfig( prefix ) : "coding error: " + prefix + " is not a child configuration object.";

        for (Iterator it = child.varMap.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();

            varMap.put( prefix + "." + e.getKey(), e.getValue() );
        }
        for (Iterator it = child.committed.iterator(); it.hasNext();)
        {
            String var = (String) it.next();

            committed.add( prefix + "." + var );
        }
    }

    private final Map varMap;                       // list of vars that have been set
    private final Set committed;                    // set of vars committed to backing config
    private final Class configClass;                // configuration class
    private Map varCache = new HashMap();           // info cache
    private List requiredList = new LinkedList();   // required vars
    private List varList = new LinkedList();        // list of vars in order they should be set
    private Map childCache = new HashMap();         // child configuration objects
    private Map aliases = new HashMap();            // variable name aliases
    private Map tokens = new HashMap();             // tokens for replacement
    private List positions = new ArrayList();

    private static final String SET_PREFIX = "cfg";
    private static final String GET_PREFIX = "get";
    private static final String CONFIGURATION_SUFFIX = "Configuration";
    private static final String INFO_SUFFIX = "Info";

    //-----------------------------------------------
    //

    /**
     * convert StudlyCaps or camelCase to hyphenated
     * @param camel someVar or SomeVar
     * @return hyphen some-var
     */
    protected static String c2h( String camel )
    {
        StringBuffer b = new StringBuffer(camel.length() + 5);
        for (int i = 0; i < camel.length(); ++i)
        {
            char c = camel.charAt(i);
            if (Character.isUpperCase( c ))
            {
                if (i != 0)
                    b.append( '-' );
                b.append( Character.toLowerCase( c ) );
            }
            else
            {
                b.append( camel.charAt(i) );
            }
        }
        return b.toString();
    }

    /**
     * convert hyphenated to StudlyCaps or camelCase
     * @param hyphenated some-var
     * @return result
     */
    protected static String h2c( String hyphenated, boolean studly )
    {
        StringBuffer b = new StringBuffer( hyphenated.length() );
        boolean capNext = studly;
        for (int i = 0; i < hyphenated.length(); ++i)
        {
            char c = hyphenated.charAt(i);
            if (c == '-')
                capNext = true;
            else
            {
                b.append( capNext? Character.toUpperCase( c ) : c );
                capNext = false;
            }
        }
        return b.toString();
    }

    private static String varname( String membername, String basename )
    {
        return ((basename == null)? membername : (basename + "." + membername));
    }

    private static ConfigurationInfo createInfo( Method setterMethod )
    {
        ConfigurationInfo info = null;

        String infoMethodName = GET_PREFIX + setterMethod.getName().substring( SET_PREFIX.length() ) + INFO_SUFFIX;
	    String getterMethodName = GET_PREFIX + setterMethod.getName().substring( SET_PREFIX.length() );
        Class cfgClass = setterMethod.getDeclaringClass();

        Method infoMethod = null, getterMethod = null;
        try
        {
            infoMethod = cfgClass.getMethod( infoMethodName, null);

            if (!Modifier.isStatic( infoMethod.getModifiers() ) )
            {
                assert false : ( "coding error: " + cfgClass.getName() + "." + infoMethodName + " needs to be static!" );
                infoMethod = null;
            }

            info = (ConfigurationInfo) infoMethod.invoke( null, null );

	        getterMethod = cfgClass.getMethod( getterMethodName, null);
        }
        catch (Exception e)
        {}

        if (info == null)
        {
            info = new ConfigurationInfo();
        }
        info.setSetterMethod( setterMethod );
	    info.setGetterMethod( getterMethod );

        return info;
    }

    private static ConfigurationInfo createChildInfo( Method childGetMethod )
    {
        ConfigurationInfo info = null;
        int cfgIndex = childGetMethod.getName().lastIndexOf( CONFIGURATION_SUFFIX );
        assert cfgIndex != -1;
        String infoMethodName = childGetMethod.getName().substring( 0, cfgIndex ) + INFO_SUFFIX;
        Class cfgClass = childGetMethod.getDeclaringClass();

        Method infoMethod = null;
        try
        {
            infoMethod = cfgClass.getMethod( infoMethodName, null);

            if (!Modifier.isStatic( infoMethod.getModifiers() ) )
            {
                assert false : ( "coding error: " + cfgClass.getName() + "." + infoMethodName + " needs to be static!" );
                infoMethod = null;
            }

            info = (ConfigurationInfo) infoMethod.invoke( null, null );

            info.setSetterMethod( null );

            assert info.getAliases() == null : "coding error: child configurations cannot have aliases.";
            assert info.getArgCount() == 0 : "coding error: child configurations do not have arguments";
            assert info.getArgName( 0 ) == null : "coding error: child configuraitons do not have argnames";

        }
        catch (Exception e)
        {
            return null;
        }

        if (info == null)
        {
            info = new ConfigurationInfo();
        }

        return info;

    }

    /**
     * load - prefetch all the interesting names into a dictionary so that we can find them
     * again more easily.  At the end of this call, we will have a list of every variable
     * and their associated method.
     * 
     * @param filter if null there is no filter, otherwise the set of configuration options
     * 				 is filtered.
     */
    private boolean loadCache( Class cfg, String basename, ConfigurationFilter filter)
    {
        int count = 0;

        // First, find all vars at this level.
        Method methods[] = cfg.getMethods();
        for (int m = 0; m < methods.length; ++m)
        {
            Method method = methods[m];

            if (method.getName().startsWith( SET_PREFIX ))
            {
                Class[] pt = method.getParameterTypes();

                if ((pt.length > 1) && (pt[0] == ConfigurationValue.class))
                {
                    // This is an autoconfiguration setter!

                    ConfigurationInfo info = createInfo( method );

                    String leafname = c2h( method.getName().substring( SET_PREFIX.length() ) );
                    String name = varname( leafname, basename );

                    if (filter == null || filter.select(name))
                    {
                        varCache.put( name, info );
                        varList.add( name );
                        if (info.isRequired())
                        {
                            requiredList.add( name );
                        }
                        ++count;                    	
                    }
                }
            }
        }

        // Now find all children.
        for (int m = 0; m < methods.length; ++m)
        {
            Method method = methods[m];

            if (method.getName().startsWith( GET_PREFIX )
                    && method.getName().endsWith( CONFIGURATION_SUFFIX ))
            {
                String leafname = c2h( method.getName().substring( GET_PREFIX.length(),
                                                                   method.getName().length() - CONFIGURATION_SUFFIX.length()));
                String fullname = varname( leafname, basename );

                if (loadCache( method.getReturnType(), fullname, filter ))
                {
                    childCache.put( fullname, method.getReturnType() );
                    ++count;
                }
            }
            else
            {
                continue;
            }
        }

        assert ( count > 0 || filter != null) : "coding error: config class " + cfg.getName() + " did not define any setters or child configs";
        return (count > 0);
    }


    String classToArgName( Class c )
    {
        // we only support builtin classnames!

        String className = c.getName();
        if (className.startsWith( "java.lang." ))
            className = className.substring( "java.lang.".length() );

        return className.toLowerCase();
    }

    public ConfigurationInfo getInfo( String avar )
    {
        String var = unalias( avar );
        return (ConfigurationInfo) varCache.get( var );
    }

    String getVarArgName( String avar, int argnum )
    {
        String var = unalias( avar );
        ConfigurationInfo info = getInfo( var );

        if (info == null)
        {
            assert false : ( "must call isValid to check vars!" );
        }

        return info.getArgName( argnum );
    }

    public boolean isValidVar( String avar )
    {
        String var = unalias( avar );
        ConfigurationInfo info = getInfo( var );
        return (info != null);
    }

    public boolean isChildConfig( String var )
    {
        return childCache.keySet().contains( var );
    }

    public Class getChildConfigClass( String var )
    {
        return (Class) childCache.get( var );
    }

    int getVarArgCount( String avar )
    {
        ConfigurationInfo info = getInfo( avar );
        assert ( info != null );
        return info.getArgCount();
    }

    /**
     * commit - bake the resolved map to the configuration
     */
    public void commit( Object config ) throws ConfigurationException
    {
        assert ( config.getClass() == configClass ) : ( "coding error: configuration " + config.getClass() + " != template " + configClass );
        Set done = new HashSet();

        for (Iterator vars = varList.iterator(); vars.hasNext(); )
        {
            String var = (String) vars.next();
            if (varMap.containsKey( var ))
            {
                commitVariable( config, var, done );
            }
        }

        for (Iterator reqs = requiredList.iterator(); reqs.hasNext();)
        {
            String req = (String) reqs.next();

            if (!committed.contains( req ))
            {
                throw new ConfigurationException.MissingRequirement( req, null, null, -1 );
            }
        }
    }

    /**
     * commitVariable - copy a variable out of a state into the final config.
     * This should only be called on variables that are known to exist in the state!
     *
     * @param var variable name to lookup
     * @param done set of variable names that have been completed so far (for recursion)
     */
    private void commitVariable( Object config, String var, Set done ) throws ConfigurationException
    {
        ConfigurationInfo info = getInfo( var );

		setPrerequisites(info.getPrerequisites(), var, done, config, true);
		setPrerequisites(info.getSoftPrerequisites(), var, done, config, false);

		if (committed.contains( var ))
			return;

        committed.add( var );
        done.add( var );

        assert ( varMap.containsKey( var ) );
        List vals = (List) varMap.get( var );

        if (vals.size() > 1)
        {
            assert ( info.allowMultiple() );   // assumed to have been previously checked
        }
        for (Iterator valit = vals.iterator(); valit.hasNext();)
        {
            ConfigurationValue val = (ConfigurationValue) valit.next();

            try
            {
                Object targetconfig = getParentConfiguration( config, var );
                Object[] args = buildArgList( info, val );

                info.getSetterMethod().invoke( targetconfig, args );

	            calculateChecksum(targetconfig, info, var, args);
            }
            catch (Exception e)
            {
                Throwable t = e;

                if (e instanceof InvocationTargetException)
                {
                    t = ((InvocationTargetException)e).getTargetException();
                }

                if (Trace.error)
                    t.printStackTrace();

                if (t instanceof ConfigurationException)
                {
                    throw (ConfigurationException)t;
                }
                else
                {
                    throw new ConfigurationException.OtherThrowable(t, var, val.getSource(), val.getLine() );
                }
            }
        }

    }

	private void setPrerequisites(String[] prerequisites, String var, Set done, Object config, boolean required)
			throws ConfigurationException
	{
		if (prerequisites != null)
		{
			for (int p = 0; p < prerequisites.length; ++p)
			{
				String depvar = prerequisites[p];

				// Dependencies can only go downward.
				int dot = var.lastIndexOf( '.' );

				if (dot >= 0)
				{
					String car = var.substring( 0, dot );
					//String cdr = var.substring( dot + 1 );

					depvar = car + "." + depvar;
				}

				if (!done.contains( depvar ))
				{
					if (!isValidVar( depvar ))
					{
						assert false : ( "invalid " + var + " dependency " + depvar );
						continue;
					}
					if (varMap.containsKey( depvar ))
					{
						commitVariable( config, depvar, done );
					}
					else if (required && !committed.contains( depvar ))
					{
                        // fixme - can we get source/line for this?
                        throw new ConfigurationException.MissingRequirement(depvar, var, null, -1);
					}
				}
			}
		}
	}

	Object getParentConfiguration( Object config, String varname )
	{
		int dot = varname.indexOf( '.' );   // FIXME? should be lastIndexOf? --rg

		String getConfigName;
		if (dot < 0)
		{
			// varname is in current config.

			return config;
		}
		else
		{
			String car = varname.substring( 0, dot );
			String cdr = varname.substring( dot + 1 );

			getConfigName = GET_PREFIX + h2c( car, true ) + CONFIGURATION_SUFFIX;

			try
			{
				Method getCfgMethod = config.getClass().getMethod( getConfigName, null );

				Object child = getCfgMethod.invoke( config, null );

				return getParentConfiguration( child, cdr );
			}
			catch (NoSuchMethodException e)
			{
				assert false : ( "impossible: should have already confirmed this!" );
			}
			catch (InvocationTargetException e)
			{
				assert false : ( "coding error: bad child config getter" );
			}
			catch (IllegalAccessException e)
			{
				assert false : ( "coding error: bad child config getter" );
			}
			return null;
		}
	}

    private String[] constructStringArray( List args )
    {
        String[] sa = new String[args.size()];

        int i = 0;
        for (Iterator it = args.iterator(); it.hasNext();)
            sa[i++] = (String) it.next();

        return sa;
    }

    private Object constructValueObject( ConfigurationInfo info, ConfigurationValue cv ) throws ConfigurationException
    {
        try
        {
            Class[] pt = info.getSetterMethod().getParameterTypes();
            assert ( pt.length == 2 ); // assumed to be checked upstream

            Object o = pt[1].newInstance();

            Field[] fields = pt[1].getFields();

            assert ( fields.length == cv.getArgs().size() );   // assumed to be checked upstream

            Iterator argsit = cv.getArgs().iterator();
            for (int f = 0; f < fields.length; ++f)
            {
                String val = (String) argsit.next();
                Object valobj = null;
                Class fc = fields[f].getType();

                assert ( info.getArgType( f ) == fc );
                assert ( info.getArgName( f ).equals( ConfigurationBuffer.c2h( fields[f].getName() )) );

                if (fc == String.class)
                {
                    valobj = val;
                }
                else if ((fc == Boolean.class) || (fc == boolean.class))
                {
                    // todo - Boolean.valueOf is pretty lax.  Maybe we should restrict to true/false?
                    valobj = Boolean.valueOf( val );
                }
                else if ((fc == Integer.class) || (fc == int.class))
                {
                    valobj = Integer.decode( val );
                }
                else if ((fc == Long.class) || (fc == long.class))
                {
                    valobj = Long.decode( val );
                }
                else
                {
                    assert false;  // should have checked any other condition upstream!
                }
                fields[f].set( o, valobj );
            }

            return o;
        }
        catch (InstantiationException e)
        {
            assert false : ( "coding error: unable to instantiate value object when trying to set var " + cv.getVar() );
            throw new ConfigurationException.OtherThrowable( e, cv.getVar(), cv.getSource(), cv.getLine() );

        }
        catch (IllegalAccessException e)
        {
            assert false : ( "coding error: " + e + " when trying to set var " + cv.getVar() );
            throw new ConfigurationException.OtherThrowable( e, cv.getVar(), cv.getSource(), cv.getLine() );
        }
    }

    protected static boolean isSupportedSimpleType( Class c )
    {
        return ((c == String.class)
                || (c == Integer.class) || (c == int.class)
                || (c == Long.class) || (c == long.class)
                || (c == Boolean.class) || (c == boolean.class));
    }

    protected static boolean isSupportedListType( Class c )
    {
        return ((c == List.class) || (c == String[].class));
    }
    protected static boolean isSupportedValueType( Class c )
    {
        if (isSupportedSimpleType( c ))
            return false;

        Field[] fields = c.getFields();

        for (int f = 0; f < fields.length; ++f)
        {
            if (!isSupportedSimpleType( fields[f].getType() ))
                return false;
        }
        return true;
    }

    private Object[] buildArgList( ConfigurationInfo info, ConfigurationValue val ) throws ConfigurationException
    {
        Method setter = info.getSetterMethod();

        Class[] pt = setter.getParameterTypes();

        List args = processValues( val.getVar(), val.getArgs(), val.getSource(), val.getLine() );

        if (info.getArgCount() == -1)
        {
            if (pt.length != 2)
            {
                assert false : ( "coding error: unlimited length setter " + val.getVar() + " must take a single argument of type List or String[]" );
                return null;
            }
            else if (List.class.isAssignableFrom( pt[1] ))
            {
                return new Object[] { val, args };
            }
            else if (String[].class.isAssignableFrom( pt[1] ))
            {
                return new Object[] {val, constructStringArray( args )};
            }
            else
            {
                assert false : ( "coding error: unlimited length setter " + val.getVar() + " must take a single argument of type List or String[]" );
                return null;
            }
        }
        else
        {
            assert ( pt.length > 1 ) : ( "coding error: config setter " + val.getVar() + " must accept at least one argument" );
            // ok, we first check to see if the signature of their setter accepts a list.


            if (pt.length == 2)
            {
                // a variety of specialty setters here...

                if (List.class.isAssignableFrom( pt[1] ))
                {
                    return new Object[] { val, args };
                }
                else if (String[].class == pt[1])
                {
                    return new Object[] { val, constructStringArray( args ) };
                }
                else if (isSupportedValueType( pt[1] ))
                {
                    return new Object[] { val, constructValueObject( info, val ) };
                }
            }

            // otherwise, they must have a matching size parm list as the number of args passed in.

            assert ( pt.length == (args.size() + 1) ) : ( "coding error: config setter " + val.getVar() + " does not have " + args.size() + " parameters!" );

            Object[] pa = new Object[pt.length];

            pa[0] = val;

            for (int p = 1; p < pt.length; ++p)
            {
                String arg = (String) args.get(p-1);
                if (pt[p].isAssignableFrom( String.class ))
                {
                    pa[p] = arg;
                }
                else if ((pt[p] == int.class) || (pt[p] == Integer.class))
                {
                    try
                    {
                        pa[p] = Integer.decode( arg );

                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException.TypeMismatch( ConfigurationException.TypeMismatch.INTEGER,
                                                                       arg, val.getVar(), val.getSource(), val.getLine() );
                    }
                }
                else if ((pt[p] == long.class) || (pt[p] == Long.class))
                {
                    try
                    {
                        pa[p] = Long.decode( arg );

                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException.TypeMismatch(
                                ConfigurationException.TypeMismatch.LONG,
                                arg, val.getVar(), val.getSource(), val.getLine() );
                    }
                }
                else if ((pt[p] == boolean.class) || (pt[p] == Boolean.class))
                {
                    try
                    {
                        arg = arg.trim().toLowerCase();
                        if ( arg.equals( "true" ) || arg.equals( "false" ) )
                        {
                            pa[p] = Boolean.valueOf( arg );
                        }
                        else
                        {
                            throw new ConfigurationException.TypeMismatch(
                                    ConfigurationException.TypeMismatch.BOOLEAN, arg, val.getVar(), val.getSource(), val.getLine() );
                        }
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException.TypeMismatch(
                                ConfigurationException.TypeMismatch.BOOLEAN, arg, val.getVar(), val.getSource(), val.getLine() );
                    }
                }
                else
                {
                    assert false : ( "coding error: " + val.getVar() + " setter argument " + p + " is not a supported type" );
                }
            }

            return pa;
        }
    }

    public void addAlias( String alias, String var )
    {
        if (!isValidVar( var ))
        {
            assert false : ( "coding error: can't bind alias " + alias + " to nonexistent var " + var );
            return;
        }
        if (aliases.containsKey( alias ))
        {
            assert false : ( "coding error: alias " + alias + " already defined as " + aliases.get( alias ));
            return;
        }
        if (varCache.containsKey( alias ))
        {
            assert false : ( "coding error: can't define alias " + alias + ", it already exists as a var" );
            return;
        }

        aliases.put( alias, var );
    }
    public Map getAliases()
    {
        return aliases;
    }

    public String unalias( String var )
    {
        String realvar = (String) aliases.get( var );
        return (realvar == null)? var : realvar;
    }

	public String peekSimpleConfigurationVar(String avar) throws ConfigurationException
	{
		String val = null;
		List valList = getVar(avar);
		if (valList != null)
		{
			ConfigurationValue cv = (ConfigurationValue) valList.get(0);
			List args = processValues(avar, cv.getArgs(), cv.getSource(), cv.getLine());
			val = (String) args.get(0);
		}
		return val;
	}

	public List peekConfigurationVar(String avar) throws ConfigurationException
	{
		List srcList = getVar(avar);
        if (srcList == null)
            return null;

        List dstList = new LinkedList();
        for (Iterator it = srcList.iterator(); it.hasNext();)
        {
			ConfigurationValue srcVal = (ConfigurationValue) it.next();
            List args = processValues(avar, srcVal.getArgs(), srcVal.getSource(), srcVal.getLine());

            ConfigurationValue dstVal = new ConfigurationValue( srcVal.getBuffer(), avar, args, srcVal.getSource(), srcVal.getLine(), srcVal.getContext());
            dstList.add( dstVal );
		}
		return dstList;
	}

	public void addPosition(String var, int iStart, int iEnd)
	{
		positions.add(new Object[] { var, new Integer(iStart), new Integer(iEnd) });
	}
	
	public List getPositions()
	{
		return positions;
	}
	
    // C: checksum calculation for Configuration. checksum() is based on config values. checksum_ts() is
	//    based on config values + timestamps from VirtualFile-based config values.

	public void setDefaultVar(String var)
	{
		defaultVar = var;
	}
	
	private String defaultVar;
	private StringBuffer compile_checksum = new StringBuffer();
	private StringBuffer compile_checksum_ts = new StringBuffer();
	private StringBuffer link_checksum = new StringBuffer();
	private StringBuffer link_checksum_ts = new StringBuffer();

	private void calculateChecksum(Object targetConfig, ConfigurationInfo info, String var, Object[] args)
		throws Exception
	{
		// C: don't use default var to calculate checksum...
		if (var != null && var.equals(defaultVar))
		{
			return;
		}
		
		// C: we always update link_checksum and link_checksum_ts.

		if (info.doChecksum())
			compile_checksum.append(var);
		link_checksum.append(var);

		for (int i = 1; i < args.length; i++)
		{
			if (info.getGetterMethod() != null)
			{
				Class retType = info.getGetterMethod().getReturnType();

				if (VirtualFile.class.isAssignableFrom(retType))
				{
					VirtualFile file = (VirtualFile) info.getGetterMethod().invoke(targetConfig, null);
					if (file != null)
					{
						if (info.doChecksum())
							compile_checksum.append(file.getName());
						link_checksum.append(file.getName());
					}
					continue;
				}
				else if (retType.isArray() && VirtualFile.class.isAssignableFrom(retType.getComponentType()))
				{
					VirtualFile[] files = (VirtualFile[]) info.getGetterMethod().invoke(targetConfig, null);
					for (int j = 0; files != null && j < files.length; j++)
					{
						if (files[j] != null)
						{
							if (info.doChecksum())
								compile_checksum.append(files[j].getName());
							link_checksum.append(files[j].getName());
						}
					}
					continue;
				}
			}

			if (args[i] instanceof Object[])
			{
				Object[] a = (Object[]) args[i];
				for (int j = 0; j < a.length; j++)
				{
					if (info.doChecksum())
						compile_checksum.append(a[j]);
					link_checksum.append(a[j]);
				}
			}
			else if (args[i] instanceof List)
			{
				List l = (List) args[i];
				for (int j = 0; j < l.size(); j++)
				{
					if (info.doChecksum())
						compile_checksum.append(l.get(j));
					link_checksum.append(l.get(j));
				}
			}
			else
			{
				if (info.doChecksum())
					compile_checksum.append(args[i]);
				link_checksum.append(args[i]);
			}
		}

		if (info.getGetterMethod() == null)
		{
			// C: need to make sure that all the VirtualFile-based config values should have getters.
			return;
		}

		Class retType = info.getGetterMethod().getReturnType();

		if (VirtualFile.class.isAssignableFrom(retType))
		{
			VirtualFile file = (VirtualFile) info.getGetterMethod().invoke(targetConfig, null);
			if (file != null && !file.isDirectory())
			{
				if (info.doChecksum())
					compile_checksum_ts.append(file.getLastModified());
				link_checksum_ts.append(file.getLastModified());
			}
		}
		else if (retType.isArray() && VirtualFile.class.isAssignableFrom(retType.getComponentType()))
		{
			VirtualFile[] files = (VirtualFile[]) info.getGetterMethod().invoke(targetConfig, null);
			for (int i = 0; files != null && i < files.length; i++)
			{
				if (files[i] != null && !files[i].isDirectory())
				{
					if (info.doChecksum())
						compile_checksum_ts.append(files[i].getLastModified());
					link_checksum_ts.append(files[i].getLastModified());
				}
			}
		}
	}

	// Compiler.processConfiguration() derives app-config.xml from the command-line target file argument.
	// That's why this is necessary...
	public void calculateChecksum(VirtualFile f)
	{		
		compile_checksum.append(f.getName());
		compile_checksum_ts.append(f.getLastModified());
	}

	// The web tier can use this to provide the timestamps of the dependent files (e.g. remoting-service.xml)
	// referenced by the service config file.
	public void calculateChecksum(String name, Long lastModified)
	{		
		compile_checksum.append(name);
		compile_checksum_ts.append(lastModified);
	}

	private int calculateChecksum(String str)
	{
		byte[] b = null;

		try
		{
			b = str.getBytes("UTF8");
		}
		catch (UnsupportedEncodingException ex)
		{
			b = str.getBytes();
		}

		int checksum = 0;

		// C: There are better algorithms to calculate checksums than this. Let's worry about it later.
		for (int i = 0; i < b.length; i++)
		{
			checksum += b[i];
		}

		return checksum;
	}

	/**
	 * This value is good for naming the cache file.
	 */
	public int checksum()
	{
		return calculateChecksum(compile_checksum.toString());
	}

	/**
	 * This value takes timestamps into account and is the actual value embedded in the cache file.
	 */
	public int checksum_ts()
	{
		return calculateChecksum(compile_checksum.toString() + compile_checksum_ts.toString());
	}
	
	public int link_checksum_ts()
	{
		return calculateChecksum(link_checksum.toString() + link_checksum_ts.toString());
	}

    static public List formatText( String input, int columns )
    {
        ArrayList lines = new ArrayList();

        if ((input == null) || (input.length() == 0))
            return lines;

        int current = 0;
        int lineStart = -1;
        int lineEnd = -1;
        int wordStart = -1;
        int wordEnd = -1;
        boolean start = true;
        boolean preserve = true;

        while (true)
        {
            if (current < input.length())
            {
                boolean newline = input.charAt( current ) == '\n';
                boolean printable = (preserve && !newline) || !Character.isWhitespace( input.charAt( current ) );


                if (start)  // find a word
                {
                    if (printable)
                    {
                        if (lineStart == -1)
                        {
                            lineStart = current;
                        }
                        wordStart = current;
                        start = false;
                    }
                    else
                    {
                        if (newline && lineStart != -1)
                        {
                            lines.add( input.substring( lineStart, current ));
                            lineStart = -1;
                        }
                        else if (newline)
                        {
                            lines.add( "" );
                        }
                        ++current;
                    }
                }
                else    // have a word
                {
                    preserve = false;
                    if (printable)
                    {
                        ++current;
                    }
                    else
                    {
                        wordEnd = current;
                        if (lineEnd == -1)
                        {
                            lineEnd = current;
                        }

                        // two possibilities; if the new word fits in the current line length
                        // without being too many columns, leave on current line.
                        // otherwise, set it as the start of a new line.

                        if (wordEnd - lineStart < columns)
                        {
                            if (newline)
                            {
                                lines.add( input.substring( lineStart, current ));
                                lineStart = -1;
                                lineEnd = -1;
                                wordStart = -1;
                                start = true;
                                preserve = true;
                                ++current;
                            }
                            else
                            {
                                // we have room to add the current word to this line, find new word
                                start = true;
                                lineEnd = current;
                            }
                        }
                        else
                        {
                            // current word pushes things beyond the requested column limit,
                            // dump current text
                            lines.add( input.substring( lineStart, lineEnd ) );
                            lineStart = wordStart;
                            lineEnd = -1;
                            wordStart = -1;
                            start = true;
                            if (newline)
                                preserve = true;
                        }
                    }
                }
            }
            else    // we're done
            {
                // a) no line yet, so don't do anything
                // b) have line and new word would push over edge, need two lines
                // c) have line and current word fits, need one line
                // d) only one word and its too long anyway, need one line

                if (lineStart != -1)    // we have a line in progress
                {
                    wordEnd = current;
                    if (lineEnd == -1)
                        lineEnd = current;

                    if (((wordEnd - lineStart) < columns) // current word fits
                            || (wordEnd == lineEnd))      // or one long word
                    {
                        lineEnd = wordEnd;
                        lines.add( input.substring( lineStart, wordEnd ));
                    }
                    else // didn't fit, multiple words
                    {
                        lines.add( input.substring( lineStart, lineEnd ));
                        lines.add( input.substring( wordStart, wordEnd ));
                    }
                }
                break;
            }
        }
        return lines;
    }
}

