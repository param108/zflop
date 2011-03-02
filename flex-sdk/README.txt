This version of Flex is officially built in Cygwin on Windows XP Pro SP2.

It requires the following software that is not under source control:

    J2SDK 1.4.2_14 (see http://java.sun.com/products/archive/j2se/1.4.2_14/index.html)
    
    Ant 1.6.2 (see http://archive.apache.org/dist/ant/binaries/)
    
The following environment variables must be set:

    JAVA_HOME
    ANT_HOME

The PATH must include

    bin directory of Flex SDK
    bin directory of Ant
    bin directory of Java

For testing, the Flash Player's mm.cfg file must have the following entries

    ErrorReportingEnable=1
    TraceOutputFileEnable=1

and a FlashPlayerTrust file must allow local SWFs to access local files.

To build and test the SDK, execute

    ant main checkintests
