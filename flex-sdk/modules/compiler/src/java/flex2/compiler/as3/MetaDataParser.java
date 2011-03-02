////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2006 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.as3;

import flex2.compiler.Source;
import flex2.compiler.as3.reflect.MetaData;
import flex2.compiler.util.CompilerMessage;
import flex2.compiler.util.ThreadLocalToolkit;
import macromedia.asc.parser.MetaDataNode;
import macromedia.asc.parser.Parser;
import macromedia.asc.util.Context;
import macromedia.asc.util.ContextStatics;

/**
 * Metadata parsing. This keeps other subsystems from using macromedia.asc classes directly.
 *
 * @author Paul Reilly
 */
public class MetaDataParser
{
	/**
	 * @param file where the metadata string is located.
	 * @param md
	 */
	public static MetaData parse(ContextStatics perCompileData, Source file, final int beginLine, String md)
	{
		StringBuffer stringBuffer = new StringBuffer();

		stringBuffer.append("[");
		stringBuffer.append(md);
		stringBuffer.append("]");

		// C: Is it possible to find out the encoding of the string from the source?
		/*InputStream inputStream = null;
		try
		{
			inputStream = new ByteArrayInputStream(stringBuffer.toString().getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException ex)
		{
			// not possible...
		}*/

		Context context = new Context((ContextStatics) perCompileData);
		context.setPath(file.getParent());
		context.setScriptName(file.getName());
		context.setHandler(new flex2.compiler.as3.Compiler.CompilerHandler()
		{
			public void error2(String filename, int ln, int col, Object msg, String source)
			{
				ThreadLocalToolkit.log(new InvalidMetadataFormatError(), filename, beginLine);
			}

			public void warning2(String filename, int ln, int col, Object msg, String source)
			{
				ThreadLocalToolkit.log(new InvalidMetadataFormatWarning(), filename, beginLine);
			}

			public void error(String filename, int ln, int col, String msg, String source, int errorCode)
			{
				ThreadLocalToolkit.log(new InvalidMetadataFormatError(), filename, beginLine);				
			}
			
			public void error(String filename, int ln, int col, String msg, String source)
			{
				ThreadLocalToolkit.log(new InvalidMetadataFormatError(), filename, beginLine);
			}

			public void warning(String filename, int ln, int col, String msg, String source, int errorCode)
			{	
				ThreadLocalToolkit.log(new InvalidMetadataFormatWarning(), filename, beginLine);
			}

			public void warning(String filename, int ln, int col, String msg, String source)
			{
				ThreadLocalToolkit.log(new InvalidMetadataFormatWarning(), filename, beginLine);
			}

			public FileInclude findFileInclude(String parentPath, String filespec)
			{
				return null;
			}			
		});
		((ContextStatics) perCompileData).handler = context.getHandler();

		Parser parser = new Parser(context, stringBuffer.toString(), file.getName());
		MetaDataNode metaDataNode = parser.parseMetaData();
		macromedia.asc.parser.MetaDataEvaluator metaDataEvaluator = new macromedia.asc.parser.MetaDataEvaluator();
		metaDataNode.evaluate(context, metaDataEvaluator);

		return context.errorCount() > 0 ? null : new MetaData(metaDataNode);
	}

	// error messages

	public static class InvalidMetadataFormatError extends CompilerMessage.CompilerError
	{
		public InvalidMetadataFormatError()
		{
			super();
		}
	}

	public static class InvalidMetadataFormatWarning extends CompilerMessage.CompilerWarning
	{
		public InvalidMetadataFormatWarning()
		{
			super();
		}
	}
}
