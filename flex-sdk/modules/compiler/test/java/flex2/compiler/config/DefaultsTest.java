package flex2.compiler.config;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.List;

public class DefaultsTest extends TestCase {


    public DefaultsTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(DefaultsTest.class);
    }

    public void testCommandLineConfiguratorListOfStrings() {
        String[] args = new String[]{"-list-of-strings", "xyz", "pdq", "zzz", "kkkkkkk", "mmmmmm", "qq", "333333333334"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, "defaults", args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String msg = "Testing Configurator Type: " + "CommandLineConfigurator";

        List l = cfg.getListOfStrings();
        assertEquals(3, l.size());
        assertEquals("xyz", l.get(0));
        assertEquals("pdq", l.get(1));
        assertEquals("zzz", l.get(2));

        List d = cfg.getDefaults();
        assertEquals(4, d.size());
        assertEquals("kkkkkkk", d.get(0));
        assertEquals("mmmmmm", d.get(1));
        assertEquals("qq", d.get(2));
        assertEquals("333333333334", d.get(3));
    }

    protected void tearDown
            () throws Exception {
    }

    static class TestConfig {

        private List sl;

        public void cfgListOfStrings(ConfigurationValue cv, List l) {
            this.sl = l;
        }

        public List getListOfStrings() {
            return sl;
        }

        public static ConfigurationInfo getListOfStringsInfo() {
            return new ConfigurationInfo(3);
        }


        public List _defaults;      // default var...

        public void cfgDefaults(ConfigurationValue cv, List args) {
            _defaults = args;
        }

        public List getDefaults() {
            return _defaults;
        }
    }

}

