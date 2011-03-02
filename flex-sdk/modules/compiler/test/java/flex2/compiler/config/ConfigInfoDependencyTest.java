package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: kquevillon
 * Date: May 10, 2005
 * Time: 3:32:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigInfoDependencyTest extends TestCase {


    public ConfigInfoDependencyTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(ConfigInfoDependencyTest.class);
    }

    public void testSystemPropertyConfigurator() {

        System.setProperty("flex.first", "foo");
        System.setProperty("flex.second", "bar");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals("foo", cfg.getFirst());
        assertEquals("foobar", cfg.getSecond());

    }

    public void testSystemPropertyConfiguratorNeg1() {

        System.setProperty("flex.thoid", "bar");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfigDeuce.class);
        TestConfigDeuce cfg = new TestConfigDeuce();
        boolean errorWasThrown = false;
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.MissingRequirement);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }


    public void testCommandLineConfigurator() {

        String[] args = new String[]{"-first", "foo", "-second", "bar"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals("foo", cfg.getFirst());
        assertEquals("foobar", cfg.getSecond());
    }

    public void testCommandLineConfiguratorNeg1() {

        String[] args = new String[]{"-second", "bar"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.MissingRequirement);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testFileConfigurator() {

        StringReader args = new StringReader("<fnord>"
                + "\n<first>foo</first>"
                + "\n<second>bar</second>"
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


        assertEquals("foo", cfg.getFirst());
        assertEquals("foobar", cfg.getSecond());
    }

    public void testFileConfiguratorNeg1() {

        StringReader args = new StringReader("<fnord>"
                + "\n<second>bar</second>"
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
            assertTrue(e instanceof ConfigurationException.MissingRequirement);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    protected void tearDown
            () throws Exception {

    }


    static class TestConfig {

        private String second;

        public void cfgSecond(ConfigurationValue cv, String s) {
            this.second = s;
        }

        public String getSecond() {
            return this.first + this.second;
        }

        public static ConfigurationInfo getSecondInfo() {
            return new ConfigurationInfo() {
                public String[] getPrerequisites() {
                    return new String[]{"first"};
                }
            };
        }

        private String first;

        public void cfgFirst(ConfigurationValue cv, String s) {
            this.first = s;
        }

        public String getFirst() {
            return this.first;
        }

    }

    static class TestConfigDeuce {

        private String thoid;

        public void cfgThoid(ConfigurationValue cv, String s) {
            this.thoid = s;
        }

        public String getThoid() {
            return this.foist + this.thoid;
        }

        public static ConfigurationInfo getThoidInfo() {
            return new ConfigurationInfo() {
                public String[] getPrerequisites() {
                    return new String[]{"foist"};
                }
            };
        }

        private String foist;

        public void cfgFoist(ConfigurationValue cv, String s) {
            this.foist = s;
        }

        public String getFoist() {
            return this.foist;
        }

    }


}
