package flex.tools.dependencychecker;
import java.io.IOException;
import java.io.Writer;


public class Utilities {
	private static String newline = System.getProperty("line.separator");
	
	public static void outputNewLine(Writer out) throws IOException
	{
		out.write(newline);
	}
}
