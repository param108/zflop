package flex2.tools;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;
import java.util.List;

import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.CommandLineConfigurator;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.config.FileConfigurator;

public class CompilerConfiigurationTest extends TestCase {

    String boolVal = "false";
    String strVal = "foobar";
    String intVal = "1122";

    String someDir;
    String someFile;


    String[] CmdLnArgsLong; //value set in setup()

    StringReader FileCfgArgs;  //value set in setup()

    public CompilerConfiigurationTest() {
    }

    protected void setUp() throws Exception {

        CmdLnArgsLong = new String[]{
            "--load-config", strVal
            , "--dump-config="+ boolVal
            , "--file-specs", strVal
            , "--help="+boolVal
            , "--long-help="+boolVal

        };

        FileCfgArgs = new StringReader("<fnord>"
                + "\n<config>" + strVal + "</config>"
                + "\n<dump-config>" + boolVal + "</dump-config>"
                + "\n<file-specs>" + strVal + "</file-specs>"
                + "\n<help>" + boolVal + "</help>"
                + "\n<long-help>" + boolVal + "</long-help>"
                + "\n</fnord>\n");

    }


    public static Test suite() {
        return new TestSuite(CompilerConfiigurationTest.class);
    }

    public ConfigurationBuffer getCommandLineConfiguratorBuffer(String[] args) {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CommandLineConfiguration.class);
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        return cfgbuf;
    }

    public ConfigurationBuffer getFileConfiguratorBuffer(StringReader args) {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(CommandLineConfiguration.class);
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

        CommandLineConfiguration cfg = new CommandLineConfiguration();
        ConfigurationBuffer cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgsLong);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        //todo: target file is last file in filespecs, so should pass more to filespecs
        //todo: how do I test config?
        //todo: how do I test help?
        //todo: how do I test long-help?

        //String config=cfg.getConfig();
        String dump_config=cfg.getDumpConfig();
        List file_specs=cfg.getFileSpecs();
        String targetFile=cfg.getTargetFile();
        //Boolean help=cfg.getHelp();
        //Boolean long_help=cfg.getLongHelp();

        assertEquals(boolVal,dump_config);
        assertEquals(1,file_specs.size());
        assertEquals(strVal,file_specs.get(0).toString());
        assertEquals(strVal,targetFile);

    }


    public void testFileCfgStr() {

        CommandLineConfiguration cfg = new CommandLineConfiguration();
        ConfigurationBuffer cfgbuf = getFileConfiguratorBuffer(FileCfgArgs);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        //todo: target file is last file in filespecs, so I should test passing more args to file-specs
        //todo: how do I test config?
        //todo: how do I test help?
        //todo: how do I test long-help?

        //String config=cfg.getConfig();
        String dump_config=cfg.getDumpConfig();
        List file_specs=cfg.getFileSpecs();
        String targetFile=cfg.getTargetFile();
        //Boolean help=cfg.getHelp();
        //Boolean long_help=cfg.getLongHelp();

        assertEquals(boolVal,dump_config);
        assertEquals(1,file_specs.size());
        assertEquals(strVal,file_specs.get(0).toString());
        assertEquals(strVal,targetFile);

    }


    protected void tearDown
            () throws Exception {

    }
}