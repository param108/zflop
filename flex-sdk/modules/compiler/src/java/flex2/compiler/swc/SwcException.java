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

package flex2.compiler.swc;

import flash.localization.LocalizationManager;
import flex2.compiler.ILocalizableMessage;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.ThreadLocalToolkit;

/**
 * General exception for SWC problems
 *
 * @author Brian Deitte
 */
public class SwcException extends RuntimeException implements ILocalizableMessage
{
	Exception detailEx;

	public String getLevel()
	{
	    return ERROR;
	}

	public String getPath()
	{
	    return null;
	}

    public void setPath(String path)
    {
    }

	public int getLine()
	{
	    return -1;
	}

    public void setLine(int line)
    {
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
		return detailEx;
	}

	public boolean isPathAvailable()
	{
		return true;
	}

	public String getMessage()
	{
		String msg = super.getMessage();
		if (msg != null)
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

	/**
	* Start of SwcExceptions, the main message classes for swc exceptions.  Only SwcException classes
	* should be logged and thrown out of the SWC package.
	**/

	public static class SwcNotLoaded extends SwcException
	{
		public SwcNotLoaded(String location, Exception exception)
		{
			this.location = location;
			this.detailEx = exception;
		}
		public String location;
	}

	public static class SwcNotExported extends SwcException
	{
		public SwcNotExported(String location, Exception exception)
		{
			this.location = location;
			this.detailEx = exception;
		}
		public String location;
	}

	public static class CouldNotFindSource extends SwcException
	{
		public CouldNotFindSource(String className)
		{
			this.className = className;
		}
		public String className;
	}

	public static class CouldNotFindFileSource extends SwcException
	{
		public CouldNotFindFileSource(String className)
		{
			this.className = className;
		}
		public String className;
	}

	public static class NoResourceBundleSource extends SwcException
	{
	    public NoResourceBundleSource( String className )
	    {
	        this.className = className;
	    }
	    public String className;
	}

	public static class NoSourceForClass extends SwcException
	{
	    public NoSourceForClass( String className, String nsTarget )
	    {
	        this.className = className;
		    this.nsTarget = nsTarget;
	    }
	    public String className;
		public String nsTarget;
	}

	public static class MissingIconFile extends SwcException
    {
        public MissingIconFile( String icon, String source )
        {
            this.icon = icon;
            this.source = source;
        }
        public String icon;
        public String source;
    }
	
	public static class MissingFile extends SwcException
	{
		public MissingFile(String file)
		{
			this.file = file;
		}
		public String file;
	}

	public static class NullCatalogStream extends SwcException
	{
	}

	public static class UseClassName extends SwcException
	{
	}

	public static class DuplicateDefinition extends SwcException
	{
		public DuplicateDefinition(String definition, String script, String source)
		{
			this.definition = definition;
			this.script = script;
			this.source = source;
		}

		public String definition;
		public String script;
		public String source;
	}

	public static class SwcNotFound extends SwcException
	{
		public SwcNotFound(String location)
		{
			this.location = location;
		}
		public String location;
	}

	public static class NotASwcDirectory extends SwcException
	{
		public NotASwcDirectory(String directory)
		{
			this.directory = directory;
		}
		public String directory;
	}

	public static class SwcLocation extends SwcException
	{
		public SwcLocation(String location)
		{
			this.location = location;
		}

		public String location;
	}

	public static class FileNotWritten extends SwcException
	{
		public FileNotWritten(String file, String message)
		{
			this.file = file;
			this.message = message;
		}
		public String file;
		public String message;
	}

	public static class FilesNotRead extends SwcException
	{
		public FilesNotRead(String message)
		{
			this.message = message;
		}
		public String message;
	}

	public static class NotADirectory extends SwcException
	{
		public NotADirectory(String directory)
		{
			this.directory = directory;
		}
		public String directory;
	}

	public static class DirectoryNotCreated extends SwcException
	{
		public DirectoryNotCreated(String directory)
		{
			this.directory = directory;
		}
		public String directory;
	}

	public static class SwcNotRenamed extends SwcException
	{
		public SwcNotRenamed(String oldName, String newName)
		{
			this.oldName = oldName;
			this.newName = newName;
		}
		public String oldName;
		public String newName;
	}

	public static class CatalogNotFound extends SwcException
	{
	}

	public static class UnsupportedOperation extends SwcException
	{
		public UnsupportedOperation(String operation, String className)
		{
			this.operation = operation;
			this.className = className;
		}
		public String operation;
		public String className;
	}

	public static class EmptyNamespace extends SwcException
	{
		public EmptyNamespace(String name)
		{
			this.name = name;
		}
		public String name;
	}

	public static class ComponentDefinedTwice extends SwcException
	{
		public ComponentDefinedTwice(String name, String className1, String className2)
		{
			this.name = name;
			this.className1 = className1;
			this.className2 = className2;
		}
		public String name;
		public String className1;
		public String className2;
	}

	public static class UnknownElementInCatalog extends SwcException
	{
		public UnknownElementInCatalog(String element, String section)
		{
			this.element = element;
			this.section = section;
		}
		public String element;
		public String section;
	}

	public static class UnsupportedFeature extends SwcException
	{
		public UnsupportedFeature(String name)
		{
			this.name = name;
		}
		public String name;
	}

	public static class NoElementValue extends SwcException
	{
		public NoElementValue(String name)
		{
			this.name = name;
		}
		public String name;
	}

	public static class ScriptUsedMultipleTimes extends SwcException
	{
		public ScriptUsedMultipleTimes(String scriptName)
		{
			this.scriptName = scriptName;
		}
		public String scriptName;
	}

	public static class NoElementValueFound extends SwcException
	{
		public NoElementValueFound(String name, String className)
		{
			this.name = name;
			this.className = className;
		}
		public String name;
		public String className;
	}

	public static class BadCRC extends SwcException
	{
		public BadCRC(String givenChecksum, String realChecksum)
		{
			this.givenChecksum = givenChecksum;
			this.realChecksum = realChecksum;
		}
		public String givenChecksum;
		public String realChecksum;
	}

	public static class UnknownZipFormat extends SwcException
	{
		public UnknownZipFormat(String start)
		{
			this.start = start;
		}
		public String start;
	}

	public static class NotAResource extends SwcException
	{
		public NotAResource(String className)
		{
			this.className = className;
		}
		public String className;
	}

	public static class CouldNotSetZipSize extends SwcException
	{
		public CouldNotSetZipSize(String entry, String message)
		{
			this.entry = entry;
			this.message = message;
		}
		public String entry;
		public String message;
	}

	public static class UnsupportedZipCompression extends SwcException
	{
		public UnsupportedZipCompression(String method)
		{
			this.method = method;
		}
		public String method;
	}

	public static class BadZipSize extends SwcException
	{
		public BadZipSize(String entry, String expected, String found)
		{
			this.entry = entry;
			this.expected = expected;
			this.found = found;
		}
		public String entry;
		public String expected;
		public String found;
	}
	
	public static class NotASwcFile extends SwcException
	{
		public NotASwcFile(String location)
		{
			this.location = location;
		}
		public String location;
	}

	public static class MetadataNotWritten extends CompilerMessage.CompilerWarning
	{
	    public MetadataNotWritten()
	    {
	    }
	}


	public static class DigestsNotWritten extends CompilerMessage.CompilerWarning
	{
	    public DigestsNotWritten()
	    {
	    }
	}

}
