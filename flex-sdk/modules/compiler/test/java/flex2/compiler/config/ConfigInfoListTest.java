package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;


public class ConfigInfoListTest extends TestCase {


    public ConfigInfoListTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(ConfigInfoListTest.class);
    }

    public void testCommandLineConfigurator() {

        String[] args = new String[]{"-list-of-stuff", "a", "b", "c"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        List l = cfg.getListOfStuff();
        assertEquals(args.length - 1, l.size());
        for (int i = 0; i < l.size(); i++) {
            assertEquals("String at position [" + i + "]", args[i + 1], l.get(i));
        }

    }

    public void testCommandLineConfiguratorLimited2Few() {
        String[] args = new String[]{"-limited-list-of-stuff", "a", "b", "c"};

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


    public void testCommandLineConfiguratorLimitedJustRight() {

        String[] args = new String[]{"-limited-list-of-stuff", "a", "b", "c", "d", "e"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        List l = cfg.getLimitedListOfStuff();
        assertEquals(args.length - 1, l.size());
        for (int i = 0; i < l.size(); i++) {
            assertEquals("String at position [" + i + "]", args[i + 1], l.get(i));
        }

    }

    public void testCommandLineConfiguratorLimited2Many() {

        String[] args = new String[]{"-limited-list-of-stuff", "a", "b", "c", "d", "e", "F", "g", "hh"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);  // FIXME - this isn't sufficient, check id
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    public void testCommandLineConfiguratorTerminatedList() {
        String[] args = new String[]{"-list-of-string", "a", "b", "c", "d", "e", "F", "g", "hh", "--", "dfltrgs"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig2.class);
        TestConfig2 cfg = new TestConfig2();
        try {
            CommandLineConfigurator.parse(cfgbuf, "defaults", args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            //System.out.println(e.getCode());
        }

        assertEquals(8, cfg.getListOfString().size());
        assertEquals("a", cfg.getListOfString().get(0));
        assertEquals("b", cfg.getListOfString().get(1));
        assertEquals("c", cfg.getListOfString().get(2));
        assertEquals("d", cfg.getListOfString().get(3));
        assertEquals("e", cfg.getListOfString().get(4));
        assertEquals("F", cfg.getListOfString().get(5));
        assertEquals("g", cfg.getListOfString().get(6));
        assertEquals("hh", cfg.getListOfString().get(7));

        assertEquals("dfltrgs",cfg.getDefaults().get(0));


    }



/////////////////////

    public void testFileConfigurator() {
        StringReader args = new StringReader("<fnord>"
                + "\n<list-of-stuff>"
                + "\n<stuff>a</stuff>"
                + "\n<stuff>b</stuff>"
                + "\n<stuff>c</stuff>"
                + "\n</list-of-stuff>"
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

        List l = cfg.getListOfStuff();
        assertEquals(3, l.size());

        assertEquals("a", l.get(0));
        assertEquals("b", l.get(1));
        assertEquals("c", l.get(2));


    }

    public void testFileConfiguratorLimited2Few() {

        StringReader args = new StringReader("<fnord>"
                + "\n<limited-list-of-stuff>"
                + "\n<stuff>a</stuff>"
                + "\n<stuff>b</stuff>"
                + "\n<stuff>c</stuff>"
                + "\n</limited-list-of-stuff>"
                + "</fnord>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //fail(e.toString());
            assertTrue(e instanceof ConfigurationException.IncorrectArgumentCount);
			errorWasThrown = true;

        }
        assertTrue(errorWasThrown);


    }

    public void testFileConfiguratorLimitedJustRight() {

        StringReader args = new StringReader("<fnord>"
                + "\n<limited-list-of-stuff>"
                + "\n<stuff>a</stuff>"
                + "\n<stuff>b</stuff>"
                + "\n<stuff>c</stuff>"
                + "\n<stuff>d</stuff>"
                + "\n<stuff>e</stuff>"
                + "\n</limited-list-of-stuff>"
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
        List l = cfg.getLimitedListOfStuff();
        assertEquals(5, l.size());

        assertEquals("a", l.get(0));
        assertEquals("b", l.get(1));
        assertEquals("c", l.get(2));
        assertEquals("d", l.get(3));
        assertEquals("e", l.get(4));

    }

    public void testFileConfiguratorLimitedTooMany() {

        StringReader args = new StringReader("<fnord>"
                + "\n<limited-list-of-stuff>"
                + "\n<stuff>a</stuff>"
                + "\n<stuff>b</stuff>"
                + "\n<stuff>c</stuff>"
                + "\n<stuff>d</stuff>"
                + "\n<stuff>e</stuff>"
                + "\n<stuff>F</stuff>"
                + "\n<stuff>g</stuff>"
                + "\n<stuff>hh</stuff>"
                + "\n</limited-list-of-stuff>"
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
            assertTrue( e instanceof ConfigurationException.IncorrectArgumentCount);
            errorWasThrown = true;
        }

        assertTrue(errorWasThrown);

    }

    protected void tearDown
            () throws Exception {

    }


    static class TestConfig {

        List stuff;

        public void cfgListOfStuff(ConfigurationValue cv, List l) {
            stuff = l;
        }

        public List getListOfStuff() {
            return stuff;
        }

        public static ConfigurationInfo getListOfStuffInfo() {
            return new ConfigurationInfo(-1, "stuff");
        }

        List limited;

        public void cfgLimitedListOfStuff(ConfigurationValue cv, List l) {
            stuff = l;
        }

        public List getLimitedListOfStuff() {
            return stuff;
        }

        public static ConfigurationInfo getLimitedListOfStuffInfo() {
            return new ConfigurationInfo(5, "stuff");
        }

    }

    static class TestConfig2 {

        List stuff;

        public void cfgListOfString(ConfigurationValue cv, List l) {
            stuff = l;
        }

        public List getListOfString() {
            return stuff;
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

