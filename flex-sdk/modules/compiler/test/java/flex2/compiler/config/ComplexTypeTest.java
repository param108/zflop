package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;
import java.util.LinkedList;


public class ComplexTypeTest extends TestCase {

    public ComplexTypeTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(ComplexTypeTest.class);
    }

    public void testCommandLineConfigurator() {

        String[] args = new String[]{
            "--complex-thingie", "1234", "some string", "true",
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
                + "\n<complex-thingie>"
                + "\n<some-integer>1234</some-integer>"
                + "\n<some-string>some string</some-string>"
                + "\n<some-boolean>True</some-boolean>"
                + "\n</complex-thingie>"
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

        String s = cfg.getComplexThingie().someString;
        int i = cfg.getComplexThingie().someInteger;
        boolean b = cfg.getComplexThingie().someBoolean;

        assertEquals("some string", s);
        assertEquals(1234, i);
        assertTrue(b);

    }

    public void testCommandLineConfiguratorMultiple() {

        String[] args = new String[]{
            "-position", "1", "1"
            , "-position", "2", "2"
            , "-position", "3", "3",
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

        List l = cfg.getPositionList();
        assertEquals(3, l.size());

        int x1 = ((TestConfig.Position) l.get(0)).x;
        int y1 = ((TestConfig.Position) l.get(0)).y;

        int x2 = ((TestConfig.Position) l.get(1)).x;
        int y2 = ((TestConfig.Position) l.get(1)).y;

        int x3 = ((TestConfig.Position) l.get(2)).x;
        int y3 = ((TestConfig.Position) l.get(2)).y;

        assertEquals(1, x1);
        assertEquals(1, y1);

        assertEquals(2, x2);
        assertEquals(2, y2);

        assertEquals(3, x3);
        assertEquals(3, y3);

    }

    public void testCommandLineConfiguratorMultiple2() {

        String[] args = new String[]{
            "-position=1,1"
            , "-position=2,2"
            , "-position=3,3",
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

        List l = cfg.getPositionList();
        assertEquals(3, l.size());

        int x1 = ((TestConfig.Position) l.get(0)).x;
        int y1 = ((TestConfig.Position) l.get(0)).y;

        int x2 = ((TestConfig.Position) l.get(1)).x;
        int y2 = ((TestConfig.Position) l.get(1)).y;

        int x3 = ((TestConfig.Position) l.get(2)).x;
        int y3 = ((TestConfig.Position) l.get(2)).y;

        assertEquals(1, x1);
        assertEquals(1, y1);

        assertEquals(2, x2);
        assertEquals(2, y2);

        assertEquals(3, x3);
        assertEquals(3, y3);

    }


    public void testFileConfiguratorMultiple() {

        StringReader args = new StringReader("<fnord>"
                + "\n<position><x>1</x><y>1</y></position>"
                + "\n<position><x>2</x><y>2</y></position>"
                + "\n<position><x>3</x><y>3</y></position>"
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

        List l = cfg.getPositionList();
        assertEquals(3, l.size());

        int x1 = ((TestConfig.Position) l.get(0)).x;
        int y1 = ((TestConfig.Position) l.get(0)).y;

        int x2 = ((TestConfig.Position) l.get(1)).x;
        int y2 = ((TestConfig.Position) l.get(1)).y;

        int x3 = ((TestConfig.Position) l.get(2)).x;
        int y3 = ((TestConfig.Position) l.get(2)).y;

        assertEquals(1, x1);
        assertEquals(1, y1);

        assertEquals(2, x2);
        assertEquals(2, y2);

        assertEquals(3, x3);
        assertEquals(3, y3);

    }

    protected void tearDown
            () throws Exception {

    }


    static class TestConfig {

        static class ComplexType {
            public int someInteger;
            public String someString;
            public boolean someBoolean;
        }

        private ComplexType ct;

        public void cfgComplexThingie(ConfigurationValue cv, ComplexType ct) {
            this.ct = ct;
        }

        public ComplexType getComplexThingie() {
            return ct;
        }

        static class Position {
            public int x;
            public int y;
        }

        List poslist = new LinkedList();

        public void cfgPosition(ConfigurationValue cv, Position p) {
            poslist.add(p);
        }

        public static ConfigurationInfo getPositionInfo() {
            return new ConfigurationInfo() {
                public boolean allowMultiple() {
                    return true;
                }
            };
        }

        List getPositionList() {
            return poslist;
        }


    }


}




