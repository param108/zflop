package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;

public class SimpleChildConfigTest extends TestCase {


    public SimpleChildConfigTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(SimpleChildConfigTest.class);
    }

    public void testCmdLineConfigurator() {
        String[] args = new String[]{"-one.string", "blap"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(MyConfig.class);
        MyConfig cfg = new MyConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals("blap", cfg.getOneConfiguration().getString());
    }

    public void testFileConfigurator() {
        StringReader args = new StringReader("<fnord>\n" +
                "  <one><string>blap</string></one>\n" +
                "</fnord>\n");


        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(MyConfig.class);
        MyConfig cfg = new MyConfig();
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals("blap", cfg.getOneConfiguration().getString());
    }


    public void testSysPropConfigurator() {
        System.setProperty("flex.one.string", "blap");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(MyConfig.class);
        MyConfig cfg = new MyConfig();
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        assertEquals("blap", cfg.getOneConfiguration().getString());
    }


    class MyConfig {

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

        public void cfgDefault(ConfigurationValue cv, List l) {

        }

    }

}



