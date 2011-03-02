Adobe Flex Compiler API Readme

This file describes the contents of the Adobe Flex Compiler API and provides information for getting started.

*****************************
*          Contents         *
*****************************

The Adobe Flex Compiler API consists of the following files and directories:

/api
        This directory contains the Javadoc documentation for the classes in the flex2.tools.oem.* package.

/lib
        This directory contains the Adobe Flex Compiler API JAR file. Copy this JAR file to your lib directory.

Additionally, you can use the Flex 2 Compiler API User Guide which contains usage information for the Adobe Flex Compiler API. This manual is available from the same source as the flex_compiler_api.zip file.

*****************************
*      Getting Started      *
*****************************

The first step to using the Adobe Flex Compiler API is write a Java application that uses the API. For example:

  import flex2.tools.oem.Application;
  import java.io.*;

  public class Example
  {
      public static void main(String[] args)
      {
          try
          {
              Application application = new Application("example", new File("example.mxml"));
              Configuration c = application.getDefaultConfiguration();
              c.setLicense("compiler","0000-0000-0000-0000-0000-0000");
              application.setConfiguration(c);
              application.setOutput(new File("example.swf"));
              application.build();
          }
          catch (Exception ex)
          {
              ex.printStackTrace();
          }
      }
  }

Replace "0000-0000-0000-0000-0000-0000" with the actual serial number for your compiler license.

The next step is compile the Java code. For example:

  javac -classpath ${flex.dir}/lib/flex-compiler-oem.jar Example.java

Replace the ${flex.dir} token with the location of Flex.

The next step is write an MXML application.  For example:

  <?xml version="1.0"?>
  <mx:Application xmlns:mx="http://www.adobe.com/2006/mxml">
      <mx:Label text="Hello World!"/>
  </mx:Application>

The last step is run the Java code and compile the new MXML application. For example:

  java -classpath ${flex.dir}/lib/flex-compiler-oem.jar:. Example

*****************************
*  Note on Trial Licenses   *
*****************************

If you are using a time-limited trial license, activate it for the first time by running:

   java -jar flex-compiler-oem.jar

from within the directory where the Adobe Flex Compiler API JAR files are installed.

*****************************
*     More Information      *
*****************************

For more information please see the Javadoc documentation for the Adobe Flex Compiler API and the Flex 2 Compiler API User Guide.
