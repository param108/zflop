package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.List;
import java.io.StringReader;

public class MergeBuffersTest extends TestCase {


    StringReader FileCfgArgs1 = new StringReader("<fnord>"
            + "\n<one-boolean>true</one-boolean>"
            + "\n<one-string>abc</one-string>"
            + "\n<one-int>1234</one-int>"
            + "\n<array-of-strings><string>kkk</string><string>mmm</string><string>qqq</string></array-of-strings>"
            + "\n<named-values><s>aaa</s><i>111</i><b>True</b></named-values>"
            + "\n<list-of-integers><int>1</int><int>3</int><int>5</int><int>7</int><int>11</int></list-of-integers>"
            + "\n<one><string>blip</string></one>"
            + "\n</fnord>\n");


    String[] CmdLnArgs1 = new String[]{"-one-boolean=false"
                                       , "-one-string", "def"
                                       , "-one-int", "5678"
                                       , "-array-of-strings", "rrr", "ggg"
                                       , "-named-values", "bbb", "999", "false"
                                       , "-list-of-integers", "2", "4", "6", "8", "10"
                                       , "-one.string", "blap"
    };


    StringReader FileCfgArgs2 = new StringReader("<fnord>"
            + "\n<one-boolean>false</one-boolean>"
            + "\n<one-string>def</one-string>"
            + "\n<one-int>5678</one-int>"
            + "\n<array-of-strings><string>rr</string><string>gg</string></array-of-strings>"
            + "\n<named-values><s>bbb</s><i>999</i><b>false</b></named-values>"
            + "\n<list-of-integers><int>2</int><int>4</int><int>6</int><int>8</int><int>10</int></list-of-integers>"
            + "\n<one><string>blap</string></one>"
            + "\n</fnord>\n");


    StringReader FileCfgArgs3 = new StringReader("<fnord>"
            + "\n<one><string>xxx</string></one>"
            + "\n</fnord>\n");


    public MergeBuffersTest() {
    }

    protected void setUp() throws Exception {

    }


    public static Test suite() {
        return new TestSuite(MergeBuffersTest.class);
    }

    public ConfigurationBuffer getSystemPropertyConfiguratorBuffer() {

        System.setProperty("flex.one-boolean", "false");
        System.setProperty("flex.one-string", "foo");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        return cfgbuf;
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
    /////////////////////

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

        ConfigurationBuffer sysCfgbuff = getSystemPropertyConfiguratorBuffer();
        ConfigurationBuffer fileCfgbuff = getFileConfiguratorBuffer(FileCfgArgs1);

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);

        cfgbuf.merge(sysCfgbuff);
        cfgbuf.merge(fileCfgbuff);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        boolean b1 = cfg.getOneBoolean();
        String s1 = cfg.getOneString();
        assertEquals(true, b1);
        assertEquals("abc", s1);

    }

    public void testTwo() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer sysCfgbuff = getSystemPropertyConfiguratorBuffer();
        ConfigurationBuffer cmdlnCfgbuff = getCommandLineConfiguratorBuffer(CmdLnArgs1);

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);

        cfgbuf.merge(cmdlnCfgbuff);
        cfgbuf.merge(sysCfgbuff);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        boolean b1 = cfg.getOneBoolean();
        String s1 = cfg.getOneString();
        assertEquals(false, b1);
        assertEquals("foo", s1);

    }

    public void test3() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer fileCfgbuff = getFileConfiguratorBuffer(FileCfgArgs1);
        ConfigurationBuffer cmdlnCfgbuff = getCommandLineConfiguratorBuffer(CmdLnArgs1);

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);

        cfgbuf.merge(cmdlnCfgbuff);
        cfgbuf.merge(fileCfgbuff);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        boolean b1 = cfg.getOneBoolean();
        String s1 = cfg.getOneString();
        int i1 = cfg.getOneInt();
        assertEquals(true, b1);
        assertEquals("abc", s1);
        assertEquals(1234, i1);

        String[] a = cfg.getArrayOfStrings();
        assertEquals(3, a.length);
        assertEquals("kkk", a[0]);
        assertEquals("mmm", a[1]);
        assertEquals("qqq", a[2]);

        String ns = cfg.getS();
        int ni = cfg.getI();
        boolean nb = cfg.getB();

        assertEquals("aaa", ns);
        assertEquals(111, ni);
        assertEquals(true, nb);

        List li = cfg.getListOfIntegers();
        assertEquals(5, li.size());
        assertEquals(1, Integer.parseInt(li.get(0).toString()));
        assertEquals(3, Integer.parseInt(li.get(1).toString()));
        assertEquals(5, Integer.parseInt(li.get(2).toString()));
        assertEquals(7, Integer.parseInt(li.get(3).toString()));
        assertEquals(11, Integer.parseInt(li.get(4).toString()));

        String child = cfg.getOneConfiguration().getString();

        assertEquals("blip", child);

    }

    public void test4() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer fileCfgbuff = getFileConfiguratorBuffer(FileCfgArgs1);
        ConfigurationBuffer fileCfgbuff2 = getFileConfiguratorBuffer(FileCfgArgs2);


        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);

        cfgbuf.merge(fileCfgbuff);
        cfgbuf.merge(fileCfgbuff2);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        boolean b1 = cfg.getOneBoolean();
        String s1 = cfg.getOneString();
        int i1 = cfg.getOneInt();
        assertEquals(false, b1);
        assertEquals("def", s1);
        assertEquals(5678, i1);

        String[] a = cfg.getArrayOfStrings();
        assertEquals(2, a.length);
        assertEquals("rr", a[0]);
        assertEquals("gg", a[1]);

        String ns = cfg.getS();
        int ni = cfg.getI();
        boolean nb = cfg.getB();

        assertEquals("bbb", ns);
        assertEquals(999, ni);
        assertEquals(false, nb);

        List li = cfg.getListOfIntegers();
        assertEquals(5, li.size());
        assertEquals(2, Integer.parseInt(li.get(0).toString()));
        assertEquals(4, Integer.parseInt(li.get(1).toString()));
        assertEquals(6, Integer.parseInt(li.get(2).toString()));
        assertEquals(8, Integer.parseInt(li.get(3).toString()));
        assertEquals(10, Integer.parseInt(li.get(4).toString()));

        String child = cfg.getOneConfiguration().getString();

        assertEquals("blap", child);

    }

    /**
     * merge a default file buffer with a second file buffer and commit
     * then, a copy of the default file buffer with a different file buffer
     * expect that there is no evidence of the second file buffer settings
     */
    public void test5() {
        TestConfig cfg = new TestConfig();

        ConfigurationBuffer fileCfgbuff = getFileConfiguratorBuffer(FileCfgArgs1);
        ConfigurationBuffer fileCfgbuff2 = getFileConfiguratorBuffer(FileCfgArgs2);

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);

        cfgbuf.merge(fileCfgbuff);
        cfgbuf.merge(fileCfgbuff2);

        try {
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // expect the values from FileCfgArgs2 in both cases
        String s1 = cfg.getOneString();
        assertEquals("def", s1);
        String child = cfg.getOneConfiguration().getString();
        assertEquals("blap", child);

        // take a copy of the default with a different file buffer
        cfg = new TestConfig();
        ConfigurationBuffer fileCfgbuff3 = new ConfigurationBuffer(fileCfgbuff, false);
        ConfigurationBuffer fileCfgbuff4 = getFileConfiguratorBuffer(FileCfgArgs3);

        fileCfgbuff3.merge(fileCfgbuff4);

        try {
            fileCfgbuff3.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // expect the value from buffer 3 in the first case and buffer 4 in the second
        s1 = cfg.getOneString();
        assertEquals("abc", s1);
        child = cfg.getOneConfiguration().getString();
        assertEquals("xxx", child);
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