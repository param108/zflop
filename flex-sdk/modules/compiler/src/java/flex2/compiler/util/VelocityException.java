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

/**
 * @author Paul Reilly
 */
public class VelocityException
{
    public static class TemplateNotFound extends CompilerMessage.CompilerError
    {
        public String template;

        public TemplateNotFound(String template)
        {
            this.template = template;
            noPath();
        }
    }

    public static class GenerateException extends CompilerMessage.CompilerError
    {
        public String message;
        public String template;

        public GenerateException(String template, String message)
        {
            this.template = template;
            this.message = message;
        }
    }

    public static class UnableToWriteGeneratedFile extends CompilerMessage.CompilerWarning
    {
        public String fileName;
        public String message;

        public UnableToWriteGeneratedFile(String fileName, String message)
        {
            this.fileName = fileName;
	        this.message = message;
	        noPath();
        }
    }
}
