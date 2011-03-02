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

import flash.localization.LocalizationManager;
import flex2.compiler.ILocalizableMessage;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 * @author Roger Gonzalez
 */
public class ConfigurationException extends Exception implements ILocalizableMessage
{
    public ConfigurationException( String msg )
    {
        super( msg );
    }

    public ConfigurationException( String var, String source, int line )
    {
        this.var = var;
        this.source = source;
        this.line = line;
    }

    public String getLevel()
    {
        return ERROR;
    }

    public String getPath()
    {
        return source;
    }

    public void setPath(String path)
    {
        source = path;
    }

    public int getLine()
    {
        return line;
    }

    public void setLine(int line)
    {
        this.line = line;
    }

    public int getColumn()
    {
        return -1;
    }

    public void setColumn(int column)
    {
    }

	public Exception getExceptionDetail()
	{
		return null;
	}

	public boolean isPathAvailable()
	{
		return true;
	}
	
    public static class UnknownVariable extends ConfigurationException
    {
        public UnknownVariable( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class IllegalMultipleSet extends ConfigurationException
    {
        public IllegalMultipleSet( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class UnexpectedDefaults extends ConfigurationException
    {
        public UnexpectedDefaults( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    public static class InterspersedDefaults extends ConfigurationException
    {
        public InterspersedDefaults( String var, String source, int line )
        {
            super( var, source, line );
        }
    }



    public String var = null;
    public String source = null;
    public int line = -1;

    public static class AmbiguousParse extends ConfigurationException
    {
        public AmbiguousParse( String defaultvar, String var, String source, int line )
        {
            super( var, source, line );
            this.defaultvar = defaultvar;
        }
        public String defaultvar;
    }


    public static class Token extends ConfigurationException
    {
        public static final String MISSING_DELIMITER = "MissingDelimiter";
        public static final String MULTIPLE_VALUES = "MultipleValues";
        public static final String UNKNOWN_TOKEN = "UnknownToken";
        public static final String RECURSION_LIMIT = "RecursionLimit";
        public static final String INSUFFICIENT_ARGS = "InsufficientArgs";
        public Token( String id, String token, String var, String source, int line )
        {
            super( var, source, line );
            this.token = token;
            this.id = id;
        }
        public String id;
        public String token;
    }
    public static class IncorrectArgumentCount extends ConfigurationException
    {
        public IncorrectArgumentCount( int expected, int passed, String var, String source, int line )
        {
            super( var, source, line );
            this.expected = expected;
            this.passed = passed;
        }
        public int expected;
        public int passed;
    }

    public static class VariableMissingRequirement extends ConfigurationException
    {
        public VariableMissingRequirement( String required, String var, String source, int line )
        {
            super( var, source, line );
            this.required = required;
        }
        public String required;
    }

    public static class MissingRequirement extends ConfigurationException
    {
        public MissingRequirement( String required, String var, String source, int line )
        {
            super( null, source, line );
            this.required = required;
        }
        public String required;
    }

    public static class OtherThrowable extends ConfigurationException
    {
        public OtherThrowable( Throwable t, String var, String source, int line )
        {
            super( var, source, line );
            this.throwable = t;
        }
        public Throwable throwable;
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

    public static class TypeMismatch extends BadValue
    {
        public static final String BOOLEAN = "Boolean";
        public static final String INTEGER = "Integer";
        public static final String LONG = "Long";
        public TypeMismatch( String type, String value, String var, String source, int line )
        {
            super( value, var, source, line );
            this.id = type;
        }
        public String id;   // named id in order to act as a subkey for L10N mgr
    }

    public static class ConfigurationIOError extends ConfigurationException
    {
        public ConfigurationIOError( String path, String var, String source, int line )
        {
            super( var, source, line );
            this.path = path;
        }
        public String path;
    }
    public static class IOError extends ConfigurationIOError
    {
        public IOError( String path )
        {
            super( path, null, null, -1 );
        }
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


    public static class NotAFile extends ConfigurationException
    {
        public NotAFile( String path, String var, String source, int line )
        {
            super( var, source, line );
            this.path = path;
        }
        public String path;
    }



    public static class UnexpectedElement extends ConfigurationException
    {
        public UnexpectedElement( String found, String source, int line )
        {
            super( null, source, line );
            this.found = found;
        }
        public String found;
    }
    public static class IncorrectElement extends ConfigurationException
    {
        public IncorrectElement( String expected, String found, String source, int line )
        {
            super( null, source, line );
            this.expected = expected;
            this.found = found;
        }
        public String expected;
        public String found;
    }




    public static class UnexpectedCDATA extends ConfigurationException
    {
        public UnexpectedCDATA( String source, int line )
        {
            super( null, source, line );
        }
    }

    // "I came here for an argument!"
    // "No you didn't."
    public static class MissingArgument extends ConfigurationException
    {
        public MissingArgument( String argument, String var, String source, int line )
        {
            super( var, source, line );
            this.argument = argument;
        }
        public String argument;
    }

    public static class UnexpectedArgument extends ConfigurationException
    {
        public UnexpectedArgument( String expected, String argument, String var, String source, int line )
        {
            super( var, source, line );
            this.expected = expected;
            this.argument = argument;
        }
        public String argument;
        public String expected;
    }
    public static class BadAppendValue extends ConfigurationException
    {
        public BadAppendValue( String var, String source, int line )
        {
            super( var, source, line );
        }
    }
    
    public static class BadVersion extends ConfigurationException
    {
        public BadVersion( String version, String var)
        {
            super( var, null, -1);
            this.version = version;
        }
        
        public String version;
    }

    
    public static class FileTooBig extends ConfigurationException
    {
        public FileTooBig( String path, String var, String source, int line )
        {
            super( var, source, line );
            this.path = path;
        }
        public String path;
    }
    
    public static class BadDefinition extends ConfigurationException
    {
        public BadDefinition( String argument, String var, String source, int line )
        {
            super( var, source, line );
            this.argument = argument;
        }
        public String argument;
    }

    
    
	public String getMessage()
	{
		String msg = super.getMessage();
		if (msg != null && msg.length() > 0)
		{
			return msg;
		}
		else
		{
			LocalizationManager l10n = ThreadLocalToolkit.getLocalizationManager();
			if (l10n == null)
			{
				return null;
			}
			else
			{
				return l10n.getLocalizedTextString(this);
			}
		}
	}
	
	public String toString()
	{
		return getMessage();
	}
}
