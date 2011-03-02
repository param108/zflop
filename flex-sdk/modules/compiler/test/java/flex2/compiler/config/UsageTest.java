package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;

public class UsageTest extends TestCase {

    String[] CmdLnArgs1 = new String[]{"-one-boolean=false"
                                       , "-one-string", "def"
                                       , "-one-int", "5678"
                                       , "-array-of-strings", "rr", "gg"
                                       , "-named-values", "bbb", "999", "false"
                                       , "-list-of-integers", "2", "4", "6", "8", "10"
                                       , "-one.string", "blap"
    };

    StringReader FileCfgArgs1 = new StringReader("<fnord>"
            + "\n<one-boolean>true</one-boolean>"
            + "\n<one-string>abc</one-string>"
            + "\n<one-int>1234</one-int>"
            + "\n<array-of-strings><string>kkk</string><string>mmm</string><string>qqq</string></array-of-strings>"
            + "\n<named-values><s>aaa</s><i>111</i><b>True</b></named-values>"
            + "\n<list-of-integers><int>1</int><int>3</int><int>5</int><int>7</int><int>11</int></list-of-integers>"
            + "\n<one><string>blip</string></one>"
            + "\n</fnord>\n");


    StringReader FileCfgArgs2 = new StringReader("<fnord>"
            + "\n<one-boolean>false</one-boolean>"
            + "\n<one-string>def</one-string>"
            + "\n<one-int>5678</one-int>"
            + "\n<array-of-strings><string>rr</string><string>gg</string></array-of-strings>"
            + "\n<named-values><s>bbb</s><i>999</i><b>false</b></named-values>"
            + "\n<list-of-integers><int>2</int><int>4</int><int>6</int><int>8</int><int>10</int></list-of-integers>"
            + "\n<one><string>blap</string></one>"
            + "\n</fnord>\n");

    public UsageTest() {
    }

    protected void setUp() throws Exception {

    }


    public static Test suite() {
        return new TestSuite(UsageTest.class);
    }

    public ConfigurationBuffer getCommandLineConfiguratorBuffer(String[] args) {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        return cfgbuf;
    }

    public ConfigurationBuffer getFileConfiguratorBuffer(StringReader args) {

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        return cfgbuf;
    }

    public void testOne() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer cfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgs1);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        /*
        FIXME
        String expected_usg = "\t[-array-of-strings [string][...]]\n" +
                "\t[-default [string][...]]\n" +
                "\t[-list-of-integers [int][...]]\n" +
                "\t[-named-values <s> <i> <b>]\n" +
                "\t[-one-boolean]\n" +
                "\t[-one-configuration <string>]\n" +
                "\t[-one-int <int>]\n" +
                "\t[-one-string <string>]\n" +
                "\t[-string <string>]";
        String usg = CommandLineConfigurator.usage(cfgbuf, null, false);
        //System.out.println(usg);
        assertEquals(expected_usg.trim(), usg.trim());
        */

		/*
		FIXME
		String expected_usg_long = "-array-of-strings [string][...]\n" +
				"-list-of-integers [int][...]\n" +
				"-named-values <s> <i> <b>\n" +
				"-one-boolean\n" +
				"-one-configuration <string>\n" +
				"-one-int <int>\n" +
				"-one-string <string>\n" +
				"-one.default (-default) [string][...]\n" +
				"-one.string (-string) <string>";
        String usglong = CommandLineConfigurator.longUsage(cfgbuf, null);
        //System.out.println(usglong);
        assertEquals(expected_usg_long.trim(), usglong.trim());
        */

    }


    public void testTwo() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer fileCfgbuf = getFileConfiguratorBuffer(FileCfgArgs2);

        ConfigurationBuffer cmdlineCfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgs1);

        try {
            fileCfgbuf.commit(cfg);
            cmdlineCfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String formattedFile = FileConfigurator.formatBuffer(fileCfgbuf, "fnord");
        String formattedCmdline = FileConfigurator.formatBuffer(cmdlineCfgbuf, "fnord");
        //System.out.println(formattedFile);
        //System.out.println(formattedCmdline);

        assertEquals(formattedFile.trim(), formattedCmdline.trim());


    }

    public void testThree() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer fileCfgbuf = getFileConfiguratorBuffer(FileCfgArgs2);

        ConfigurationBuffer cmdlineCfgbuf = getCommandLineConfiguratorBuffer(CmdLnArgs1);

        try {
            fileCfgbuf.commit(cfg);
            cmdlineCfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String formattedFile = FileConfigurator.formatBuffer(fileCfgbuf, "fnord");
        String formattedCmdline = FileConfigurator.formatBuffer(cmdlineCfgbuf, "fnord");
        //System.out.println(formattedFile);
        //System.out.println(formattedCmdline);

        assertEquals(formattedFile.trim(), formattedCmdline.trim());

        /*
        FIXME
        String usgFile = CommandLineConfigurator.usage(fileCfgbuf, null, false);
        String usgCmdLine = CommandLineConfigurator.usage(cmdlineCfgbuf, null, false);

        assertEquals(usgFile.trim(), usgCmdLine.trim());

        String usgLFile = CommandLineConfigurator.longUsage(fileCfgbuf, null);
        String usgLCmdLine = CommandLineConfigurator.longUsage(cmdlineCfgbuf, null);

        assertEquals(usgLFile.trim(), usgLCmdLine.trim());
        */


    }


    protected void tearDown
            () throws Exception {

    }

    static class TestConfig {
        /////////////////////////////////////////////////////////////////////////////
        private boolean b1 = false;

        public void cfgOneBoolean(ConfigurationValue cv, boolean b) {
            this.b1 = b;
        }

        public boolean getOneBoolean() {
            return b1;
        }


        /////////////////////////////////////////////////////////////////////////////
        private String os;

        public void cfgOneString(ConfigurationValue cv, String s) {
            this.os = s;
        }

        public String getOneString() {
            return os;
        }

        /////////////////////////////////////////////////////////////////////////////
        private int i1;

        public void cfgOneInt(ConfigurationValue cv, int i) {
            this.i1 = i;
        }

        public int getOneInt() {
            return i1;
        }


        private String[] sa;

        public void cfgArrayOfStrings(ConfigurationValue cv, String[] a) {
            this.sa = a;
        }

        public String[] getArrayOfStrings() {
            return sa;
        }

        private List il;

        public void cfgListOfIntegers(ConfigurationValue cv, List l) {
            this.il = l;
        }

        public List getListOfIntegers() {
            return il;
        }

        public static ConfigurationInfo getListOfIntegersInfo() {
            return new ConfigurationInfo(-1, "int");
        }

        String s;
        int i;
        boolean b;

        public void cfgNamedValues(ConfigurationValue cv, String s, int i, boolean b) {
            this.s = s;
            this.i = i;
            this.b = b;
        }

        public static ConfigurationInfo getNamedValuesInfo() {
            return new ConfigurationInfo(new String[]{"s", "i", "b"});
        }

        public String getS() {
            return this.s;
        }

        public int getI() {
            return this.i;
        }

        public boolean getB() {
            return this.b;
        }

        public OneConfig one = new OneConfig();
        // this is a nested config, everything will be prefixed with "one."

        public OneConfig getOneConfiguration() {
            return one;
        }

        public void cfgOneConfiguration(ConfigurationValue cv, String s) {
            this.one.cfgString(cv, s);
        }
    }


    static class OneConfig {
        String s;

        public void cfgString(ConfigurationValue cv, String s) {
            this.s = s;
        }

        public String getString() {
            return s;
        }

        public void cfgDefault(ConfigurationValue cv, List l) {

        }

    }

}