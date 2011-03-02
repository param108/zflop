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

package flex2.compiler.util;

import flex2.compiler.ILocalizableMessage;
import flex2.compiler.CompilerException;
import flash.localization.LocalizationManager;

/**
 * @author Roger Gonzalez
 */
public class CompilerMessage extends CompilerException implements ILocalizableMessage
{
    public CompilerMessage( String level, String path, int line, int col )
    {
        this.level = level;
        this.path = path;
        this.line = line;
        this.column = col;
        isPathAvailable = true;
    }

    public String getLevel()
    {
        return level;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
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
        return column;
    }

    public void setColumn(int column)
    {
        this.column = column;
    }

    public Exception getExceptionDetail()
    {
        return null;
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

	public boolean isPathAvailable()
	{
		return isPathAvailable;
	}

	protected void noPath()
	{
		isPathAvailable = false;
	}
	
    public String level;
    public String path;
    public int line;
    public int column;
    private boolean isPathAvailable;


    // TODO - add ctors to these as needed

    public static class CompilerError extends CompilerMessage
    {
        public CompilerError()
        {
            super( ERROR, null, -1, -1 );
        }
    }

    public static class CompilerWarning extends CompilerMessage
    {
        public CompilerWarning()
        {
            super( WARNING, null, -1, -1 );
        }
    }

    public static class CompilerInfo extends CompilerMessage
    {
        public CompilerInfo()
        {
            super( INFO, null, -1, -1 );
        }
    }
}
