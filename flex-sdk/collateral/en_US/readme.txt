Adobe Flex SDK 2.0 Beta 3 Readme

This file describes the contents of the Flex Software 
Development Kit (SDK) and where to get more information 
about using the command-line compilers.

*****************************
*          Contents         *
*****************************

The Flex SDK consists of the following directories:

../bin		
	Contains command-line tools such as mxmlc, compc and fdb.
 You use these tools to compile and debug Flex applications and 
components.

../frameworks
	Contains the framework.swc file, frameworks.swc 
source code and other helper files that are used to compile Flex
 applications.

../players
	Contains debug versions of the Flash Player.

../lib
	Contains JAR files used by the compilers.

../samples
	Contains source code for sample applications. Run 
build-samples.bat (Windows) or build-samples.sh (Mac and Unix)
to build the SWFs.  The scripts assume that the bin/ and 
/samples directories are at the same level. Once the samples 
are built open explorer.html and flexstore.html in a browser
to view them.


Note: You can store this directory structure anywhere, 
but the /bin and /lib directories must be at the same level.


*****************************
*     Compiler Resources    *
*****************************

The /frameworks/flex-config.xml file includes the default 
compiler options.

The /bin/jvm.config contains Java VM settings.


*****************************
*      More Information     *
*****************************

For information on using the command-line compilers, see 
"Using the Flex Compilers" in Building and Deploying Flex 
Applications, provided with the Beta documentation.


