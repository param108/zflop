package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.List;
import java.io.StringReader;

public class ConfigInfoNamedTest extends TestCase {


    public ConfigInfoNamedTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(ConfigInfoNamedTest.class);
    }

    public void testCommandLineConfigurator() {

        String[] args = new String[]{"-named-values", "somestring", "1234", "True"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        String s = cfg.getS();
        int i = cfg.getI();
        boolean b = cfg.getB();

        assertEquals("somestring", s);
        assertEquals(1234, i);
        assertTrue(b);
    }

    public void testCommandLineConfiguratorMisordered() {

        String[] args = new String[]{"-named-values", "1234", "false", "somestring"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.TypeMismatch);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfigurator2Few() {

        String[] args = new String[]{"-named-values", "somestring", "1234"};

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

        String[] args = new String[]{"-named-values", "somestring", "1234", "True", "andanotherstring", "6667"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException); // fixme - bad defaults
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }


    /////////////////////

    public void testFileConfigurator() {

        StringReader args = new StringReader("<fnord>"
                                             + "\n<named-values>"
                                             + "\n<s>somestring</s>"
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

        String s = cfg.getS();
        int i = cfg.getI();
        boolean b = cfg.getB();

        assertEquals("somestring", s);
        assertEquals(1234, i);
        assertTrue(b);
    }

    public void testFileConfiguratorDuplicates() {

        StringReader args = new StringReader("<fnord>"
                                             + "\n<named-values>"
                                             + "\n<s>somestring</s>"
                                             + "\n<i>1234</i>"
                                             + "\n<b>True</b>"
                                             + "\n<b>false</b>"
                                             + "\n<b>false</b>"
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
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfiguratorMisordered() {

        StringReader args = new StringReader("<fnord>"
                                             + "\n<named-values>"
                                             + "\n<b>true</b>"
                                             + "\n<s>somestring</s>"
                                             + "\n<i>1234</i>"
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

        String s = cfg.getS();
        int i = cfg.getI();
        boolean b = cfg.getB();

        assertEquals("somestring", s);
        assertEquals(1234, i);
        assertTrue(b);
    }

    public void testFileConfiguratorWrongTypes() {

        StringReader args = new StringReader("<fnord>"
                                             + "\n<named-values>"
                                             + "\n<s>1234</s>"
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
            //assertEquals(ConfigurationException.EXCEPTION,e.getCode());
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);
    }

    public void testFileConfigurator2Many() {

        StringReader args = new StringReader("<fnord>"
                                             + "\n<named-values>"
                                             + "\n<s>somestring</s>"
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
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    protected void tearDown
            () throws Exception {

    }


    static class TestConfig {

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


    }


}

