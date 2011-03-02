package flex2.compiler.common;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;

import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.CommandLineConfigurator;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.config.FileConfigurator;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.util.ThreadLocalToolkit;

public class ConfigurationTest extends TestCase {

    boolean bool = false;
    String boolVal = String.valueOf(bool);
    String strVal = "foobar";
    String intVal = "1122";

    String someDir;
    String someFile;

    String[] CmdLnArgsLongBool = new String[]{

        "--compiler.accessible=" + boolVal
        , "--compiler.show-coach-warnings=" + boolVal
        , "--compiler.debug=" + boolVal
        , "--compiler.keep-generated-actionscript=" + boolVal
        , "--compiler.show-deprecation-warnings=" + boolVal
        , "--compiler.strict=" + boolVal
        , "--use-network=" + boolVal
        , "--dependency-info=" + boolVal
        , "--verbose=" + boolVal

    };

    String[] CmdLnArgsLongInt21 = new String[]{
        "--default-script-limits", intVal
    };
    String[] CmdLnArgsLongInt23 = new String[]{
        "--default-script-limits", intVal, intVal, intVal
    };

    String[] CmdLnArgsLongInt31 = new String[]{
        "--default-size", intVal
    };
    String[] CmdLnArgsLongInt33 = new String[]{
        "--default-size", intVal, intVal, intVal
    };

    String[] CmdLnArgsLongInt = new String[]{

        "--default-background-color=" + intVal
        , "--default-frame-rate=" + intVal
        , "--default-script-limits", intVal, intVal
        , "--default-size", intVal, intVal
    };


    String[] CmdLnArgsLongStr; //value set in setup()
    String[] CmdLnArgsLongStr2; //value set in setup()
    String[] CmdLnArgsLongStr3; //value set in setup()
    String[] CmdLnArgsLongStr4; //value set in setup()
    String[] CmdLnArgsLongStr5; //value set in setup()


    StringReader xFileCfgArgs1 = new StringReader("<fnord>"
            + "\n<one-boolean>true</one-boolean>"
            + "\n<one-string>abc</one-string>"
            + "\n<one-int>1234</one-int>"
            + "\n<array-of-strings><string>kkk</string><string>mmm</string><string>qqq</string></array-of-strings>"
            + "\n<named-values><s>aaa</s><i>111</i><b>True</b></named-values>"
            + "\n<list-of-integers><int>1</int><int>3</int><int>5</int><int>7</int><int>11</int></list-of-integers>"
            + "\n<one><string>blip</string></one>"
            + "\n</fnord>\n");

    StringReader FileCfgArgsBool = new StringReader("<fnord>"
            + "\n<compiler>"
            + "\n   <accessible>" + boolVal + "</accessible>"
            + "\n   <show-coach-warnings>" + boolVal + "</show-coach-warnings>"
            + "\n   <debug>" + boolVal + "</debug>"
            + "\n   <keep-generated-actionscript>" + boolVal + "</keep-generated-actionscript>"
            + "\n   <show-deprecation-warnings>" + boolVal + "</show-deprecation-warnings>"
            + "\n   <strict>" + boolVal + "</strict>"
            + "\n</compiler>"
            + "\n<use-network>" + boolVal + "</use-network>"
            + "\n<dependency-info>" + boolVal + "</dependency-info>"
            + "\n<verbose>" + boolVal + "</verbose>"
            + "\n</fnord>\n");


    StringReader FileCfgArgsInt = new StringReader("<fnord>"
            + "\n<default-background-color>" + intVal + "</default-background-color>"
            + "\n<default-frame-rate>" + intVal + "</default-frame-rate>"
            + "\n<default-script-limits>"
            + "\n    <max-recursion-depth>" + intVal + "</max-recursion-depth>"
            + "\n    <max-execution-time>" + intVal + "</max-execution-time>"
            + "\n</default-script-limits>"
            + "\n<default-size>"
            + "\n   <width>" + intVal + "</width>"
            + "\n   <height>" + intVal + "</height>"
            + "\n</default-size>"
            + "\n</fnord>\n");

    StringReader FileCfgArgsStr;  //value set in setup()
    StringReader FileCfgArgsStr2;  //value set in setup()
    StringReader FileCfgArgsStr3;  //value set in setup()

    public ConfigurationTest() {
    }

    protected void setUp() throws Exception {
        ThreadLocalToolkit.setPathResolver(new PathResolver());

        someFile = new File(this.getClass().getResource("").getPath() + "somefile.xml").getAbsolutePath();
        someDir = new File(this.getClass().getResource("").getPath()).getAbsolutePath();

        //write out somefile.xml to someDir that contains:
        //<?xml version="1.0" encoding="UTF-8"?>
        //<foo/>

        File f = new File(someFile);
        String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo/>";
        PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)));
        out.write(text);
        out.close();

        //todo: bug?? doesn't seem to matter what -flexlib is, just that it exists???
        CmdLnArgsLongStr = new String[]{

            "--flexlib", ""
            , "--compiler.source-path", someDir
            //, "--compiler.enabled-warnings=" + someFile
            , "--compiler.global-css-url=" + someFile
            , "--compiler.library-path=" + someDir
            , "--compiler.namespaces.namespace", strVal, someFile
            , "--compiler.services=" + someFile
            , "--debug-password=" + strVal
            , "--global=" + someFile         //depends on --flexlib
            , "--mxml-manifest=" + someFile    //depends on --flexlib
            , "--playerglobal=" + someFile       //depends on --flexlib

        };
        //no flexlib
        CmdLnArgsLongStr2 = new String[]{
            "--compiler.source-path", someDir
            //, "--compiler.enabled-warnings=" + someFile
            , "--compiler.global-css-url=" + someFile
            , "--compiler.library-path=" + someDir
            , "--compiler.namespaces.namespace", strVal, someFile
            , "--compiler.services=" + someFile
            , "--debug-password=" + strVal
            , "--global=" + someFile         //depends on --flexlib
            , "--mxml-manifest=" + someFile    //depends on --flexlib
            , "--playerglobal=" + someFile       //depends on --flexlib

        };

        CmdLnArgsLongStr3 = new String[]{
            "--flexlib", ""
            , "--mxml-manifest=" + strVal    //depends on --flexlib


        };

         CmdLnArgsLongStr5 = new String[]{

            "--flexlib", ""
            ,"--compiler.source-path", strVal
        };

        FileCfgArgsStr = new StringReader("<fnord>"
                + "\n<flexlib>" + someDir + "</flexlib>"
                + "\n<compiler>"
                + "\n   <source-path><path-element>" + someDir + "</path-element></source-path>"
                //+ "\n   <enabled-warnings>" + someFile + "</enabled-warnings>"
                + "\n   <global-css-url>" + someFile + "</global-css-url>"
                + "\n   <library-path><path-element>" + someDir + "</path-element></library-path>"
                + "\n   <namespaces>"
                + "\n       <namespace>"
                + "\n           <uri>" + strVal + "</uri>"
                + "\n           <manifest>" + someFile + "</manifest>"
                + "\n       </namespace>"
                + "\n   </namespaces>"
                + "\n   <services>" + someFile + "</services>"
                + "\n</compiler>"
                + "\n<debug-password>" + strVal + "</debug-password>"
                + "\n<global>" + someFile + "</global>"         //depends on --flexlib
                + "\n<mxml-manifest>" + someFile + "</mxml-manifest>"    //depends on --flexlib
                + "\n<playerglobal>" + someFile + "</playerglobal>"      //depends on --flexlib
                + "\n</fnord>\n");

        FileCfgArgsStr2 = new StringReader("<fnord>"
                + "\n<flexlib>" + someDir + "</flexlib>"
                + "\n<compiler>"
                + "\n   <namespaces>"
                + "\n       <namespace>"
                + "\n           <uri>" + strVal + "</uri>"
                + "\n       </namespace>"
                + "\n   </namespaces>"
                + "\n</compiler>"
                + "\n</fnord>\n");

        FileCfgArgsStr3 = new StringReader("<fnord>"
                + "\n<flexlib>" + someDir + "</flexlib>"
                + "\n<compiler>"
                + "\n   <namespaces>"
                + "\n       <namespace>"
                + "\n           <manifest>" + strVal + "</manifest>"
                + "\n       </namespace>"
                + "\n   </namespaces>"
                + "\n</compiler>"
                + "\n</fnord>\n");


    }


    public static Test suite() {
        return new TestSuite(ConfigurationTest.class);
    }

    public ConfigurationBuffer getCommandLineConfiguratorBuffer(String[] args) throws ConfigurationException {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(Configuration.class);
        CommandLineConfigurator.parse(cfgbuf, null, args);

        return cfgbuf;
    }

    public ConfigurationBuffer getFileConfiguratorBuffer(StringReader args) throws ConfigurationException {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(Configuration.class);
        FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
        return cfgbuf;
    }

    public void testCmdLineLongBool() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;

        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongBool);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals(bool, cfg.getCompilerConfiguration().accessible());
        //assertEquals(bool, cfg.getCompilerConfiguration().coach());
        assertEquals(bool, cfg.getCompilerConfiguration().debug());
        assertEquals(bool, cfg.getCompilerConfiguration().keepGeneratedActionScript());
        assertEquals(bool, cfg.getCompilerConfiguration().showDeprecationWarnings());
        assertEquals(bool, cfg.getCompilerConfiguration().strict());
        assertEquals(bool, cfg.useNetwork());

        String usg = CommandLineConfigurator.usage("foo", "defaultVar", cfgbuf, null,
                                                   ThreadLocalToolkit.getLocalizationManager(), "");
        System.out.println(usg);

        String f = FileConfigurator.formatBuffer(cfgbuf, "foo");

        System.out.println(f);
    }


    public void testCmdLineLongInt() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;

        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongInt);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals(Integer.parseInt(intVal), cfg.backgroundColor());
        assertEquals(Integer.parseInt(intVal), cfg.getFrameRate());
        assertEquals(Integer.parseInt(intVal), cfg.getScriptRecursionLimit());
        assertEquals(Integer.parseInt(intVal), cfg.getFrameRate());
        assertEquals(intVal, cfg.height());
        assertEquals(intVal, cfg.width());

    }

    public void testCmdLineLongIntNeg1() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongInt21);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testCmdLineLongIntNeg2() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongInt23);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.UnexpectedDefaults);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testCmdLineLongIntNeg3() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongInt31);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testCmdLineLongIntNeg4() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongInt33);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //FIXMEassertEquals(ConfigurationException.BAD_DEFAULTS, e.getCode());
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testCmdLineNegMissingLibpath() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongStr2);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.MissingRequirement);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testCmdLineNegBadPath1() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongStr3);
             // this whole flexlib thing seems kludgey - kq
            String flib = cfgbuf.getVar("flexlib").get(0).toString();

            ConfigurationPathResolver configResolver = new ConfigurationPathResolver();
            configResolver.setRoot(flib);
            cfg.setConfigPathResolver(configResolver);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.IOError);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

     public void testCmdLineNegBadPath3() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        boolean errorWasThrown = false;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongStr5);
             // this whole flexlib thing seems kludgey - kq
            String flib = cfgbuf.getVar("flexlib").get(0).toString();

            ConfigurationPathResolver configResolver = new ConfigurationPathResolver();
            configResolver.setRoot(flib);
            cfg.setConfigPathResolver(configResolver);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.IOError);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }


    public void testCmdLineLongStr() {

        System.out.println(someDir);
        System.out.println(someFile);

        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        try {
            cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLongStr);

            // this whole flexlib thing seems kludgey - kq
            String flib = cfgbuf.getVar("flexlib").get(0).toString();

            ConfigurationPathResolver configResolver = new ConfigurationPathResolver();
            configResolver.setRoot(flib);
            cfg.setConfigPathResolver(configResolver);

            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String sourcePath = cfg.getCompilerConfiguration().getSourcePath()[0].getName();
        System.out.println("source-path = " + sourcePath);

        //String EnabledWarnings = cfg.getCompilerConfiguration().getEnabledWarnings().getName();
        //System.out.println("EnabledWarnings = " + EnabledWarnings);

        String LibraryPath = cfg.getCompilerConfiguration().getLibraryPath()[0].getName();
        System.out.println("LibraryPath = " + LibraryPath);

        String Manifest = ((VirtualFile) cfg.getCompilerConfiguration().getNamespacesConfiguration().getManifestMappings().get(strVal)).getName();
        System.out.println("Manifest = " + Manifest);

        String debugPassword = cfg.debugPassword();
        System.out.println("debugPassword = " + debugPassword);

        //String Services = String.valueOf(cfg.getCompilerConfiguration().getServices().getServices().size());
        //System.out.println("Services = " + Services);

        assertEquals(someDir, sourcePath);
        //assertEquals(someFile, EnabledWarnings);
        assertEquals(someDir, LibraryPath);
        assertEquals(someFile, Manifest);
        assertEquals(strVal, debugPassword);
        //assertEquals("0", Services);
    }

    /////////////////

    public void testFileCfgBool() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;

        try {
            cfgbuf = getFileConfiguratorBuffer(FileCfgArgsBool);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals(bool, cfg.getCompilerConfiguration().accessible());
        //assertEquals(bool, cfg.getCompilerConfiguration().coach());
        assertEquals(bool, cfg.getCompilerConfiguration().debug());
        assertEquals(bool, cfg.getCompilerConfiguration().keepGeneratedActionScript());
        assertEquals(bool, cfg.getCompilerConfiguration().showDeprecationWarnings());
        assertEquals(bool, cfg.getCompilerConfiguration().strict());
        assertEquals(bool, cfg.useNetwork());
    }

    public void testFileCfgInt() {
        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;

        try {
            cfgbuf = getFileConfiguratorBuffer(FileCfgArgsInt);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals(Integer.parseInt(intVal), cfg.backgroundColor());
        assertEquals(Integer.parseInt(intVal), cfg.getFrameRate());
        assertEquals(Integer.parseInt(intVal), cfg.getScriptRecursionLimit());
        assertEquals(Integer.parseInt(intVal), cfg.getFrameRate());
        assertEquals(intVal, cfg.height());
        assertEquals(intVal, cfg.width());

    }

    public void testFileCfgStr() {

        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;
        try {
            cfgbuf = getFileConfiguratorBuffer(FileCfgArgsStr);

            // this whole flexlib thing seems kludgey - kq
            String flib = cfgbuf.getVar("flexlib").get(0).toString();

            ConfigurationPathResolver resolver = new ConfigurationPathResolver();
            resolver.setRoot(flib);
            cfg.setConfigPathResolver(resolver);

            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String sourcePath;
        try {
            sourcePath = cfg.getCompilerConfiguration().getSourcePath()[0].getName();
        } catch (Exception e) {
            sourcePath = "";
        }
        System.out.println("source-path = " + sourcePath);
        //String EnabledWarnings = cfg.getCompilerConfiguration().getEnabledWarnings().getName();
        //System.out.println("EnabledWarnings = " + EnabledWarnings);


        String LibraryPath;
        try {
            LibraryPath = cfg.getCompilerConfiguration().getLibraryPath()[0].getName();
        } catch (Exception e) {
            LibraryPath = "";
        }
        System.out.println("LibraryPath = " + LibraryPath);

        String Manifest = ((VirtualFile) cfg.getCompilerConfiguration().getNamespacesConfiguration().getManifestMappings().get(strVal)).getName();
        System.out.println("Manifest = " + Manifest);

        String debugPassword = cfg.debugPassword();
        System.out.println("debugPassword = " + debugPassword);

        //String Services = String.valueOf(cfg.getCompilerConfiguration().getServices().getServices().size());
        //System.out.println("Services = " + Services);

        assertEquals(someDir, sourcePath);
        //assertEquals(someFile, EnabledWarnings);
        assertEquals(someDir, LibraryPath);
        assertEquals(someFile, Manifest);
        assertEquals(strVal, debugPassword);
        //assertEquals("0", Services);

        //String f = FileConfigurator.formatBuffer(cfgbuf, "fnord");
        //System.out.println(f);
    }

    public void testFileCfgStrNeg1() {

        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;

        boolean errorWasThrown = false;
        try {
            cfgbuf = getFileConfiguratorBuffer(FileCfgArgsStr2);

            // this whole flexlib thing seems kludgey - kq
            String flib = cfgbuf.getVar("flexlib").get(0).toString();

            ConfigurationPathResolver resolver = new ConfigurationPathResolver();
            resolver.setRoot(flib);
            cfg.setConfigPathResolver(resolver);

            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.OtherThrowable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);


    }

    public void testFileCfgStrNeg2() {

        Configuration cfg = new Configuration();
        ConfigurationBuffer cfgbuf = null;

        boolean errorWasThrown = false;
        try {
            cfgbuf = getFileConfiguratorBuffer(FileCfgArgsStr3);

            // this whole flexlib thing seems kludgey - kq
            String flib = cfgbuf.getVar("flexlib").get(0).toString();

            ConfigurationPathResolver resolver = new ConfigurationPathResolver();
            resolver.setRoot(flib);
            cfg.setConfigPathResolver(resolver);

            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            assertTrue(e instanceof ConfigurationException.MissingArgument);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);


    }


    protected void tearDown
            () throws Exception {

        ThreadLocalToolkit.setPathResolver(null);
    }
}
