package flex2.compiler.config;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringReader;

public class SimpleStringTest extends TestCase {


    public SimpleStringTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(SimpleStringTest.class);
    }

    public void testSystemPropertyConfigurator() {

        System.setProperty("flex.one-string", "foo");           //a1
        System.setProperty("flex.onestring", "bar");            //a2
        System.setProperty("flex.1string", "zip");              //a3
        System.setProperty("flex.Onestring", "zap");            //a4
        System.setProperty("flex.OneString", "blip");           //a5
        System.setProperty("flex.one.string", "blap");          //a6
        System.setProperty("flex.One.string", "yip");           //a7
        System.setProperty("flex.One.String", "yap");           //a8
        System.setProperty("flex.One-String", "dip");           //a9
        System.setProperty("flex.One-string", "dap");           //a10

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String msg = "Testing Configurator Type: " + "SystemPropertyConfigurator";

        String a1 = cfg.getOneString();
        String a2 = cfg.getOnestring();
        String a2n = cfg.getonestring();
        String a3 = cfg.get1string();
        String a4 = cfg.getOnestring();
        String a5 = cfg.getOnestring();
        String a5n = cfg.getOneString();

        String a6 = cfg.getOneConfiguration().getString();
        String a7 = cfg.getOneConfiguration().getString();
        String a8 = cfg.getOneConfiguration().getString();

        String a9 = cfg.getOneString();
        String a10 = cfg.getOneString();
        String a10n = cfg.getOnestring();

        assertEquals(msg, "foo", a1);
        assertEquals(msg, "bar", a2);
        assertNull(msg, a2n);
        assertEquals(msg, "zip", a3);
        assertEquals(msg, "bar", a4);
        assertEquals(msg, "bar", a5);
        assertEquals(msg, "foo", a5n);

        assertEquals(msg, "blap", a6);
        assertFalse(msg, "yip" == a7);
        assertFalse(msg, "yap" == a8);

        assertEquals(msg, "foo", a9);
        assertEquals(msg, "foo", a10);
        assertEquals(msg, "bar", a10n);
    }

    public void testCommandLineConfigurator() {

        String[] args = new String[]{"-one-string", "foo"
                                     , "-onestring", "bar"
                                     , "-1string", "zip"
                                     //   , "-Onestring", "zap"
                                     //   , "-OneString", "blip"
                                     , "-one.string", "blap"
                                     //   , "-One.string", "yip"
                                     //   , "-One.String", "yap"
                                     //   , "-One-String", "dip"
                                     //   , "-One-string", "dap"
        };

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String msg = "Testing Configurator Type: " + "CommandLineConfigurator";

        String a1 = cfg.getOneString();
        String a2 = cfg.getOnestring();
        String a2n = cfg.getonestring();
        String a3 = cfg.get1string();
        String a4 = cfg.getOnestring();
        String a5 = cfg.getOnestring();
        String a5n = cfg.getOneString();

        String a6 = cfg.getOneConfiguration().getString();
        String a7 = cfg.getOneConfiguration().getString();
        String a8 = cfg.getOneConfiguration().getString();

        String a9 = cfg.getOneString();
        String a10 = cfg.getOneString();
        String a10n = cfg.getOnestring();

        assertEquals(msg, "foo", a1);
        assertEquals(msg, "bar", a2);
        assertNull(msg, a2n);
        assertEquals(msg, "zip", a3);
        assertEquals(msg, "bar", a4);
        assertEquals(msg, "bar", a5);
        assertEquals(msg, "foo", a5n);

        assertEquals(msg, "blap", a6);
        assertFalse(msg, "yip" == a7);
        assertFalse(msg, "yap" == a8);

        assertEquals(msg, "foo", a9);
        assertEquals(msg, "foo", a10);
        assertEquals(msg, "bar", a10n);
    }

    public void testCommandLineConfiguratorNeg1() {

        String[] args = new String[]{"-Onestring", "zap"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorNeg2() {

        String[] args = new String[]{"-OneString", "blip"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable );
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorNeg3() {

        String[] args = new String[]{"-One.string", "yip"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorNeg4() {

        String[] args = new String[]{"-One.String", "yap"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorNeg5() {

        String[] args = new String[]{"-One-String", "dip"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorNeg6() {

        String[] args = new String[]{"-One-string", "dap"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorJapanese() {

        String[] args = new String[]{"-one-string", ""

        };



        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }


    }


    public void testFileConfigurator() {
        StringReader args = new StringReader("<fnord>"
                + "\n<one-string>foo</one-string>"
                + "\n<onestring>bar</onestring>"
                //+ "\n<1string>zip</1string>"
                // + "\n<Onestring>zap</Onestring>"
                // + "\n<OneString>blip</OneString>"
                + "\n<one><string>blap</string></one>"
                // + "\n<One><string>yip</string></One>"
                // + "\n<One><String>yap</String></One>"
                // + "\n<One-String>dip</One-String>"
                // + "\n<One-string>dap</One-string>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String msg = "Testing Configurator Type: " + "FileConfigurator";

        String a1 = cfg.getOneString();
        String a2 = cfg.getOnestring();
        String a2n = cfg.getonestring();
        String a3 = cfg.get1string();
        String a4 = cfg.getOnestring();
        String a5 = cfg.getOnestring();
        String a5n = cfg.getOneString();

        String a6 = cfg.getOneConfiguration().getString();
        String a7 = cfg.getOneConfiguration().getString();
        String a8 = cfg.getOneConfiguration().getString();

        String a9 = cfg.getOneString();
        String a10 = cfg.getOneString();
        String a10n = cfg.getOnestring();

        assertEquals(msg, "foo", a1);
        assertEquals(msg, "bar", a2);
        assertNull(msg, a2n);
        assertNull(msg, a3);
        assertEquals(msg, "bar", a4);
        assertEquals(msg, "bar", a5);
        assertEquals(msg, "foo", a5n);

        assertEquals(msg, "blap", a6);
        assertFalse(msg, "yip" == a7);
        assertFalse(msg, "yap" == a8);

        assertEquals(msg, "foo", a9);
        assertEquals(msg, "foo", a10);
        assertEquals(msg, "bar", a10n);

    }

    public void testFileConfiguratorNeg1() {
        StringReader args = new StringReader("<fnord>"
                + "\n<1string>zip</1string>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.OtherThrowable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorNeg2() {
        StringReader args = new StringReader("<fnord>"
                + "\n<Onestring>zap</Onestring>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorNeg3() {
        StringReader args = new StringReader("<fnord>"
                + "\n<OneString>blip</OneString>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorNeg4() {
        StringReader args = new StringReader("<fnord>"
                + "\n<One><string>yip</string></One>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorNeg5() {
        StringReader args = new StringReader("<fnord>"
                + "\n<One><String>yap</String></One>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorNeg6() {
        StringReader args = new StringReader("<fnord>"
                + "\n<One-String>dip</One-String>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }


    public void testFileConfiguratorNeg7() {
        StringReader args = new StringReader("<fnord>"
                + "\n<One-string>dap</One-string>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.UnknownVariable);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    protected void tearDown
            () throws Exception {

    }


    class TestConfig {

        private String s1;

        public void cfgOneString(ConfigurationValue cv, String s) {
            this.s1 = s;
        }

        public String getOneString() {
            return s1;
        }

        private String s2;

        public void cfgonestring(ConfigurationValue cv, String s) {
            this.s2 = s;
        }

        public String getonestring() {
            return s2;
        }

        private String s3;

        public void cfg1string(ConfigurationValue cv, String s) {
            this.s3 = s;
        }

        public String get1string() {
            return s3;
        }

        private String s4;

        public void cfgOnestring(ConfigurationValue cv, String s) {
            this.s4 = s;
        }

        public String getOnestring() {
            return s4;
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

    class OneConfig {
        String s;

        public void cfgString(ConfigurationValue cv, String s) {
            this.s = s;
        }

        public String getString() {
            return s;
        }

    }

}

