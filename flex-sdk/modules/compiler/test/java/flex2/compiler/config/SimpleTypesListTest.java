package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;


public class SimpleTypesListTest extends TestCase {


    public SimpleTypesListTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(SimpleTypesListTest.class);
    }

    public void testCommandLineConfiguratorListOfStrings() {
        String[] args = new String[]{"-list-of-strings", "xyz", "pdq", "zzz"};

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

        List l = cfg.getListOfStrings();
        assertEquals(args.length - 1, l.size());
        for (int i = 0; i < l.size(); i++) {
            assertEquals(msg + " String at position [" + i + "]", args[i + 1], l.get(i));
        }

    }

    public void testCommandLineConfiguratorListOfIntegers() {
        String[] args = new String[]{"-list-of-integers", "10", "100", "100005", "0", "99"};

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

        List l = cfg.getListOfIntegers();

        assertEquals(args.length - 1, l.size());

        for (int i = 0; i < l.size(); i++) {
            assertEquals(msg + " Int at position [" + i + "]", Integer.parseInt(args[i + 1]), Integer.parseInt(l.get(i).toString()));
        }
    }

    public void testCommandLineConfiguratorListOfFloats() {
        String[] args = new String[]{"-list-of-floats", "1.1", String.valueOf(1 / 3), String.valueOf(9 / 2.13), "0.000"};

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

        List l = cfg.getListOfFloats();

        assertEquals(args.length - 1, l.size());

        for (int i = 0; i < l.size(); i++) {
            assertEquals(msg + " Float at position [" + i + "]", (args[i + 1]), l.get(i).toString());
        }
    }

    public void testCommandLineConfiguratorListOfBooleans() {
        String[] args = new String[]{"-list-of-booleans", "true", "TRUE", "tRue", "false", "FaLSe"};

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

        List l = cfg.getListOfBooleans();

        assertEquals(args.length - 1, l.size());

        for (int i = 0; i < l.size(); i++) {
            assertEquals(msg + " Boolean at position [" + i + "]", new Boolean(args[i + 1]).booleanValue(), new Boolean(l.get(i).toString()).booleanValue());
        }
    }

    public void testFileConfiguratorListOfStrings() {
        StringReader args = new StringReader("<fnord>"
                + "\n<list-of-strings>"
                + "\n<string>xyz</string>"
                + "\n<string>pdq</string>"
                + "\n<string>zzz</string>"
                + "\n</list-of-strings>"
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

        String msg = "Testing Configurator Type: " + "CommandLineConfigurator";

        List l = cfg.getListOfStrings();
        assertEquals(3, l.size());
        assertEquals("xyz", l.get(0));
        assertEquals("pdq", l.get(1));
        assertEquals("zzz", l.get(2));
    }

    public void testFileConfiguratorListOfIntegers() {
        StringReader args = new StringReader("<fnord>"
                + "\n<list-of-integers>"
                + "\n<int>10</int>"
                + "\n<int>100</int>"
                + "\n<int>100005</int>"
                + "\n<int>0</int>"
                + "\n<int>-99</int>"
                + "\n</list-of-integers>"
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

        List l = cfg.getListOfIntegers();
        assertEquals(5, l.size());
        assertEquals(10, Integer.parseInt(l.get(0).toString()));
        assertEquals(100, Integer.parseInt(l.get(1).toString()));
        assertEquals(100005, Integer.parseInt(l.get(2).toString()));
        assertEquals(0, Integer.parseInt(l.get(3).toString()));
        assertEquals(-99, Integer.parseInt(l.get(4).toString()));
    }


    public void testFileConfiguratorListOfFloats() {
        StringReader args = new StringReader("<fnord>"
                + "\n<list-of-floats>"
                + "\n<float>1.1</float>"
                + "\n<float>" + String.valueOf(1 / 3) + "</float>"
                + "\n<float>" + String.valueOf(9 / 2.13) + "</float>"
                + "\n<float>0</float>"
                + "\n<float>0.000</float>"
                + "\n</list-of-floats>"
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

        List l = cfg.getListOfFloats();
        assertEquals(5, l.size());
        assertEquals(String.valueOf(1.1), l.get(0).toString());
        assertEquals(String.valueOf(1 / 3), l.get(1).toString());
        assertEquals(String.valueOf(9 / 2.13), l.get(2).toString());
        assertEquals(String.valueOf(0), l.get(3).toString());

    }


    public void testFileConfiguratorListOfBooleans() {
        StringReader args = new StringReader("<fnord>"
                + "\n<list-of-booleans>"
                + "\n<boolean>true</boolean>"
                + "\n<boolean>TRUE</boolean>"
                + "\n<boolean>tRue</boolean>"
                + "\n<boolean>false</boolean>"
                + "\n<boolean>FaLSe</boolean>"
                + "\n</list-of-booleans>"
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

        List l = cfg.getListOfBooleans();
        assertEquals(5, l.size());
        assertTrue((new Boolean(l.get(0).toString())).booleanValue());
        assertTrue(new Boolean(l.get(1).toString()).booleanValue());
        assertTrue(new Boolean(l.get(2).toString()).booleanValue());
        assertFalse(new Boolean(l.get(3).toString()).booleanValue());
        assertFalse(new Boolean(l.get(4).toString()).booleanValue());

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

        private List bl;

        public void cfgListOfBooleans(ConfigurationValue cv, List l) {
            this.bl = l;
        }

        public List getListOfBooleans() {
            return bl;
        }

        public static ConfigurationInfo getListOfBooleansInfo() {
            return new ConfigurationInfo(-1, "boolean");
        }

        private List fl;

        public void cfgListOfFloats(ConfigurationValue cv, List l) {
            this.fl = l;
        }

        public List getListOfFloats() {
            return fl;
        }

        public static ConfigurationInfo getListOfFloatsInfo() {
            return new ConfigurationInfo(-1, "float");
        }

    }

}

