package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;

public class SimpleTypesArrayTest extends TestCase {


    public SimpleTypesArrayTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(SimpleTypesArrayTest.class);
    }

    public void testCommandLineConfiguratorArrayOfStrings() {
        String[] args = new String[]{"-array-of-strings", "xyz", "pdq", "zzz"};

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

        String[] a = cfg.getArrayOfStrings();
        assertEquals(args.length - 1, a.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(msg + " String at position [" + i + "]", args[i + 1], a[i]);
        }

    }

    public void XXtestCommandLineConfiguratorArrayOfIntegers() {
        String[] args = new String[]{"-array-of-integers", "10", "100", "100005", "0", "-99"};

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

        int[] a = cfg.getArrayOfIntegers();

        assertEquals(args.length - 1, a.length);

        for (int i = 0; i < a.length; i++) {
            assertEquals(msg + " Int at position [" + i + "]", Integer.parseInt(args[i + 1]), a[i]);
        }
    }

    public void XXtestCommandLineConfiguratorArrayOfFloats() {
        String[] args = new String[]{"-array-of-floats", "1.1", String.valueOf(1 / 3), String.valueOf(-9 / 2.13), "0.000"};

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

        /*
        float[] a = cfg.getArrayOfFloats();

        assertEquals(args.length - 1, a.length);

        for (int i = 0; i < a.length; i++) {
            assertEquals(msg + " Float at position [" + i + "]", (args[i + 1]), Float.toString(a[i]));
        }
        */
    }

    public void XXtestCommandLineConfiguratorArrayOfBooleans() {
        String[] args = new String[]{"-array-of-booleans", "true", "TRUE", "tRue", "false", "FaLSe"};

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

        boolean[] a = cfg.getArrayOfBooleans();

        assertEquals(args.length - 1, a.length);

        for (int i = 0; i < a.length; i++) {
            assertEquals(msg + " Boolean at position [" + i + "]", new Boolean(args[i + 1]).booleanValue(), a[i]);
        }
    }

    public void testFileConfiguratorArrayOfStrings() {
        StringReader args = new StringReader("<fnord>"
                + "\n<array-of-strings>"
                + "\n<string>xyz</string>"
                + "\n<string>pdq</string>"
                + "\n<string>zzz</string>"
                + "\n</array-of-strings>"
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

        String[] a = cfg.getArrayOfStrings();
        assertEquals(3, a.length);
        assertEquals("xyz", a[0]);
        assertEquals("pdq", a[1]);
        assertEquals("zzz", a[2]);
    }
    //todo: bug: I logged a bug against this 126706
    public void testFileConfiguratorArrayOfStringsNeg1() {
        StringReader args = new StringReader("<fnord>"
                + "\n<array-of-strings>xyz"
                + "\n</array-of-strings>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }


    public void XXtestFileConfiguratorArrayOfIntegers() {
        StringReader args = new StringReader("<fnord>"
                + "\n<array-of-integers>"
                + "\n<int>10</int>"
                + "\n<int>100</int>"
                + "\n<int>100005</int>"
                + "\n<int>0</int>"
                + "\n<int>-99</int>"
                + "\n</array-of-integers>"
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

        int[] a = cfg.getArrayOfIntegers();
        assertEquals(5, a.length);
        assertEquals(10, a[0]);
        assertEquals(100, a[1]);
        assertEquals(10005, a[2]);
        assertEquals(0, a[3]);
        assertEquals(-99, a[4]);
    }


    public void XXtestFileConfiguratorArrayOfFloats() {
        StringReader args = new StringReader("<fnord>"
                + "\n<array-of-floats>"
                + "\n<float>1.1</float>"
                + "\n<float>" + String.valueOf(1 / 3) + "</float>"
                + "\n<float>" + String.valueOf(-9 / 2.13) + "</float>"
                + "\n<float>0</float>"
                + "\n<float>0.000</float>"
                + "\n</array-of-floats>"
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

        float[] a = cfg.getArrayOfFloats();
        assertEquals(4, a.length);
        assertEquals(String.valueOf(1.1), String.valueOf(a[0]));
        assertEquals(String.valueOf(1 / 3), String.valueOf(a[1]));
        assertEquals(String.valueOf(-9 / 2.13), String.valueOf(a[2]));
        assertEquals(String.valueOf(0.000), String.valueOf(a[3]));
    }


    public void XXtestFileConfiguratorArrayOfBooleans() {

        StringReader args = new StringReader("<fnord>"
                + "\n<array-of-booleans>"
                + "\n<boolean>true</boolean>"
                + "\n<boolean>TRUE</boolean>"
                + "\n<boolean>tRue</boolean>"
                + "\n<boolean>false</boolean>"
                + "\n<boolean>FaLSe</string>"
                + "\n</array-of-booleans>"
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

        boolean[] a = cfg.getArrayOfBooleans();
        assertEquals(5, a.length);
        assertTrue(a[0]);
        assertTrue(a[1]);
        assertTrue(a[2]);
        assertFalse(a[3]);
        assertFalse(a[4]);

    }

    protected void tearDown
            () throws Exception {
    }

    class TestConfig {

        private String[] sa;

        public void cfgArrayOfStrings(ConfigurationValue cv, String[] a) {
            this.sa = a;
        }

        public String[] getArrayOfStrings() {
            return sa;
        }

        private int[] ia;

        public void XXcfgArrayOfIntegers(ConfigurationValue cv, int[] a) {
            this.ia = a;
        }

        public int[] getArrayOfIntegers() {
            return ia;
        }

        private boolean[] ba;

        public void XXcfgArrayOfBooleans(ConfigurationValue cv, boolean[] a) {
            this.ba = a;
        }

        public boolean[] getArrayOfBooleans() {
            return ba;
        }

        private float[] fa;

        public void XXcfgArrayOfFloats(ConfigurationValue cv, float[] a) {
            this.fa = a;
        }

        public float[] getArrayOfFloats() {
            return fa;
        }

    }

}

