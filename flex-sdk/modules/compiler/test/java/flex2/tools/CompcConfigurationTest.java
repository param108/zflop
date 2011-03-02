package flex2.tools;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.io.*;
import java.util.List;
import java.util.Map;

import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.CommandLineConfigurator;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.config.FileConfigurator;


public class CompcConfigurationTest extends TestCase {

    String boolVal = "false";
    String strVal = "foobar";
    String intVal = "1122";

    String someDir;
    String someFile;


    String[] CmdLnArgsLong; //value set in setup()

    StringReader FileCfgArgs;  //value set in setup()

    public CompcConfigurationTest() {
    }

    protected void setUp() throws Exception {
        someFile = new File(this.getClass().getResource("").getPath() + "somefile.xml").getAbsolutePath();
        someDir = new File(this.getClass().getResource("").getPath()).getAbsolutePath();

        CmdLnArgsLong = new String[]{
            "--load-config", strVal
            , "--file", strVal, strVal
            , "--file-specs", strVal
            //, "--help="+strVal
            , "--namespace-target", strVal
            , "--output", strVal
            , "--root", someDir
            , "--uri", strVal
        };

        FileCfgArgs = new StringReader("<fnord>"
                + "\n<config>" + strVal + "</config>"
                + "\n<file><name>" + strVal + "</name><path>" + someFile + "</path></file>"
                + "\n<file-specs><string>" + strVal + "</string></file-specs>"
               // + "\n<help>" + strVal + "</help>"
                + "\n<namespace-target>" + strVal + "</namespace-target>"
                + "\n<output>" + strVal + "</output>"
                + "\n<root>" + someDir + "</root>"
                + "\n<uri>" + strVal + "</uri>"
                + "\n</fnord>\n");

    }


    public static Test suite() {
        return new TestSuite(CompcConfigurationTest.class);
    }

    public ConfigurationBuffer getCommandLineConfiguratorBuffer(String[] args) {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CompcConfiguration.class);
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        return cfgbuf;
    }

    public ConfigurationBuffer getFileConfiguratorBuffer(StringReader args) {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CompcConfiguration.class);
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        return cfgbuf;
    }


    public void testCmdLineLongStr() {

        System.out.println(someDir);
        System.out.println(someFile);

        CompcConfiguration cfg = new CompcConfiguration();
        ConfigurationBuffer cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLong);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        //todo: how do I test config?
        //todo: how do I test help?

        //Can't get config - String config = cfg.getConfig();
        Map files = cfg.getFiles();
        List file_specs = cfg.getIncludeSources();
        // Can't get help - String help=cfg.getHelp();
        List namespace_target = cfg.getNamespaces();
        String output = cfg.getOutput();

        assertEquals(1, files.size());

        String path = (String)files.get(strVal);
        assertEquals(strVal, path);

        assertEquals(1, file_specs.size());
        String fs = file_specs.get(0).toString();

        assertEquals(strVal, fs);

        assertEquals(strVal, namespace_target);
        assertEquals(strVal, output);
    }


    public void testFileCfgStr() {

        CompcConfiguration cfg = new CompcConfiguration();
        ConfigurationBuffer cfgbuf = getFileConfiguratorBuffer(FileCfgArgs);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }


//        String usglong = CommandLineConfigurator.longUsage(cfgbuf, null);
//        System.out.println(usglong);

        //todo: how do I test config?
        //todo: how do I test help?

        //Can't get config - String config = cfg.getConfig();
        Map files = cfg.getFiles();
        List file_specs = cfg.getIncludeSources();
        // Can't get help - String help=cfg.getHelp();
        List namespace_target = cfg.getNamespaces();
        String output = cfg.getOutput();

        assertEquals(1, files.size());

        String f1 = files.get(strVal).toString();
        assertEquals(someFile, f1);

        assertEquals(1, file_specs.size());
        String fs = file_specs.get(0).toString();

        assertEquals(strVal, fs);

        assertEquals(strVal, namespace_target);
        assertEquals(strVal, output);
    }


    protected void tearDown
            () throws Exception {

    }
}