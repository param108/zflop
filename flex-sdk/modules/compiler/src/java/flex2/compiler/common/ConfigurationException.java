////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.common;

/**
 * @author Roger Gonzalez
 */
public class ConfigurationException extends flex2.compiler.config.ConfigurationException
{
    public ConfigurationException( String var, String source, int line )
    {
        super( var, source, line );
    }
    public static class BadFrameParameters extends ConfigurationException
    {
        public BadFrameParameters( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class GreaterThanZero extends ConfigurationException
    {
        public GreaterThanZero( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class MustSpecifyTarget extends ConfigurationException
    {
        public MustSpecifyTarget( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class NoSwcInputs extends ConfigurationException
    {
        public NoSwcInputs( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class OnlyOneSource extends ConfigurationException
    {
        public OnlyOneSource( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class CouldNotCreateConfig extends ConfigurationException
    {
        public CouldNotCreateConfig( String var, String source, int line )
        {
            super( var, source, line );
        }
    }

    public static class BadValue extends ConfigurationException
    {
        public BadValue( String value, String var, String source, int line )
        {
            super( var, source, line );
            this.value = value;
        }
        public String value;
    }

    public static class BadMetadataCombo extends ConfigurationException
    {
        public BadMetadataCombo( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class IllegalDimensions extends ConfigurationException
    {
        public IllegalDimensions( int width, int height, String var, String source, int line )
        {
            super( var, source, line );
            this.width = width;
            this.height = height;
        }
        public int width;
        public int height;
    }
    public static class CannotOpen extends ConfigurationException
    {
        public CannotOpen( String path, String var, String source, int line )
        {
            super(var, source, line );
            this.path = path;
        }
        public String path;
    }
    public static class UnknownNamespace extends ConfigurationException
    {
        public UnknownNamespace( String ns, String var, String source, int line )
        {
            super( var, source, line );
            this.namespace = ns;
        }
        public String namespace;
    }
    public static class NotDirectory extends ConfigurationException
    {
        public NotDirectory( String path, String var, String source, int line )
        {
            super( var, source, line );
            this.path = path;
        }
        public String path;
    }

    public static class DirectoryNotEmpty extends ConfigurationException
    {
        public DirectoryNotEmpty( String path, String var, String source, int line )
        {
            super( var, source, line );
            this.path = path;
        }
        public String path;
    }

    public static class RedundantFile extends ConfigurationException
    {
        public RedundantFile( String path, String var, String source, int line )
        {
            super( var, source, line );
            this.path = path;
        }
        public String path;
    }



    public static class ObsoleteVariable extends ConfigurationException
    {
        public ObsoleteVariable( String replacement, String var, String source, int line )
        {
            super( var, source, line );
            this.replacement = replacement;
        }
        public String replacement;
    }

	public static class NoASDocInputs extends ConfigurationException
	{
	    public NoASDocInputs()
	    {
	        super( null, null, -1 );
	    }
	}

	public static class BadExcludeDependencies extends ConfigurationException
	{
	    public BadExcludeDependencies()
	    {
	        super( null, null, -1 );
	    }
	}
}
