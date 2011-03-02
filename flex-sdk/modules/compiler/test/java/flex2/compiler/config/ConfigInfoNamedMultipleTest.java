package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;

public class ConfigInfoNamedMultipleTest extends TestCase {


    public ConfigInfoNamedMultipleTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(ConfigInfoNamedMultipleTest.class);
    }

    public void testCommandLineConfigurator() {

        String[] args = new String[]{"-named-values", "somestring1", "somestring2", "somestring3", "1234", "True"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        String s1 = cfg.getS1();
        String s2 = cfg.getS2();
        String s3 = cfg.getS3();
        int i = cfg.getI();
        boolean b = cfg.getB();

        assertEquals("somestring1", s1);
        assertEquals("somestring2", s2);
        assertEquals("somestring3", s3);
        assertEquals(1234, i);
        assertTrue(b);
    }

    public void testCommandLineConfiguratorMisordered() {

        String[] args = new String[]{"-named-values", "1234", "false", "somestring1", "somestring2", "somestring3"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            // System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.TypeMismatch);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfigurator2Few() {

        String[] args = new String[]{"-named-values", "somestring1", "1234"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfigurator2Many() {

        String[] args = new String[]{"-named-values", "somestring1", "somestring2", "somestring3", "1234", "True", "andanotherstring", "6667"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            //assertEquals(ConfigurationException.BAD_DEFAULTS, e.getCode());
            assertTrue(false); // fixme
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }


    /////////////////////

    public void testFileConfigurator() {

        StringReader args = new StringReader("<fnord>"
                + "\n<named-values>"
                + "\n<s>somestring1</s>"
                + "\n<s>somestring2</s>"
                + "\n<s>somestring3</s>"
                + "\n<i>1234</i>"
                + "\n<b>True</b>"
                + "\n</named-values>"
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

        String s1 = cfg.getS1();
        String s2 = cfg.getS2();
        String s3 = cfg.getS3();

        int i = cfg.getI();
        boolean b = cfg.getB();

        assertEquals("somestring1", s1);
        assertEquals("somestring2", s2);
        assertEquals("somestring3", s3);
        assertEquals(1234, i);
        assertTrue(b);
    }

    public void BBtestFileConfiguratorWrongDuplicates() {
        //@todo: bug? this seems like a bug or at least the error message could be better indicating that there are dups
        // --rg it basically punted on letting you have arbitrary order, so it went into "by sequence" mode, and
        // then you weren't matching what it was expecting.  any better suggestions for a message?
        StringReader args = new StringReader("<fnord>"
                + "\n<named-values>"
                + "\n<s>somestring1</s>"
                + "\n<s>somestring2</s>"
                + "\n<s>somestring3</s>"
                + "\n<i>1234</i>"
                + "\n<b>True</b>"
                + "\n<s>12345678</s>"
                + "\n</named-values>"
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

        String s1 = cfg.getS1();
        String s2 = cfg.getS2();
        String s3 = cfg.getS3();
        int i = cfg.getI();
        boolean b = cfg.getB();

        assertEquals("somestring1", s1);
        assertEquals("somestring2", s2);
        assertEquals("somestring3", s3);
        assertEquals(1234, i);
        assertTrue(b);
    }

    public void testFileConfiguratorMisordered() {

        StringReader args = new StringReader("<fnord>"
                + "\n<named-values>"
                + "\n<b>true</b>"
                + "\n<s>somestring1</s>"
                + "\n<i>1234</i>"
                + "\n<s>somestring2</s>"
                + "\n<s>somestring3</s>"
                + "\n</named-values>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            assertTrue(e instanceof ConfigurationException.UnexpectedArgument);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorWrongTypes() {

        StringReader args = new StringReader("<fnord>"
                + "\n<named-values>"
                + "\n<s>1234</s>"
                + "\n<s>5678</s>"
                + "\n<s>0</s>"
                + "\n<i>false</i>"
                + "\n<b>somestring</b>"
                + "\n</named-values>"
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
            assertTrue(e instanceof ConfigurationException.TypeMismatch);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfigurator2Few() {
        StringReader args = new StringReader("<fnord>"
                + "\n<named-values>"
                + "\n<s>somestring</s>"
                + "\n<i>1234</i>"
                + "\n<b>true</b>"
                + "\n</named-values>"
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
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);
            errorWasThrown = true;
        } catch (AssertionError e) {
            errorWasThrown = true;
        }


        assertTrue(errorWasThrown);
    }

    public void testFileConfigurator2Many() {

        StringReader args = new StringReader("<fnord>"
                + "\n<named-values>"
                + "\n<s>somestring1</s>"
                + "\n<s>somestring2</s>"
                + "\n<s>somestring3</s>"
                + "\n<i>1234</i>"
                + "\n<b>false</b>"
                + "\n<foo>andanotherstring</foo>"
                + "\n<bar>6667</bar>"
                + "\n</named-values>"
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
            assertTrue(e instanceof ConfigurationException.UnexpectedArgument);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    protected void tearDown
            () throws Exception {

    }


    static class TestConfig {

        String s1;
        String s2;
        String s3;

        int i;
        boolean b;

        public void cfgNamedValues(ConfigurationValue cv, String s1, String s2, String s3, int i, boolean b) {
            this.s1 = s1;
            this.s2 = s2;
            this.s3 = s3;
            this.i = i;
            this.b = b;
        }

        public static ConfigurationInfo getNamedValuesInfo() {
            return new ConfigurationInfo(new String[]{"s", "s", "s", "i", "b"}) {
                public boolean allowMultiple() {
                    return true;
                }
            };
        }

        public String getS1() {
            return this.s1;
        }

        public String getS2() {
            return this.s2;
        }

        public String getS3() {
            return this.s3;
        }

        public int getI() {
            return this.i;
        }

        public boolean getB() {
            return this.b;
        }

    }


}
