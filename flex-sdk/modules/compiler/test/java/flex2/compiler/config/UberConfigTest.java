package flex2.compiler.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;
import java.util.List;
import java.util.LinkedList;

public class UberConfigTest extends TestCase {


    public UberConfigTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(UberConfigTest.class);
    }

    public void testFoo() {
        try {
            ConfigurationBuffer cfgbuf1 = new ConfigurationBuffer(TestConfiguration.class);
            //SystemPropertyConfigurator.load( cfgbuf1, "flex" );
            FileConfigurator.load(cfgbuf1, new StringReader("<fnord>\n" +
                    "  <one-boolean>true</one-boolean>\n" +
                    "  <other-boolean>false</other-boolean>\n" +
                    "  <one-int>123</one-int>\n" +
                    "  <list-of-strings1><string>abc</string><string>def</string></list-of-strings1>\n" +
                    "  <array-of-strings1><string>xyz</string><string>pdq</string><string>zzz</string></array-of-strings1>\n" +
                    "  <named-values><s>hello, world</s><i>7</i><b>true</b></named-values>\n" +
                    "  <list-of-stuff><stuff>a</stuff><stuff>b</stuff><stuff>c</stuff></list-of-stuff>\n" +
                    "  <dep-test>2</dep-test>\n" +
                    "  <child><last>2</last><middle>3</middle><first>10</first></child>\n" +
                    "  <complex-thingie><some-integer>999</some-integer><some-string>hi hi</some-string><some-boolean>true</some-boolean></complex-thingie>\n" +
                    "  <position><x>1</x><y>1</y></position>\n" +
                    "  <position><x>2</x><y>2</y></position>\n" +
                    "  <position><y>3</y><x>3</x></position>\n" +
                    "  <defaults><string>hi</string><string>there</string></defaults>\n" +
                    "</fnord>\n"), "fakefile", "fnord");
            TestConfiguration cfg1 = new TestConfiguration();
            cfgbuf1.commit(cfg1);
            cfg1.validate1();

            ConfigurationBuffer cfgbuf2 = new ConfigurationBuffer(TestConfiguration.class);
            cfgbuf2.addAlias("florp", "position");

            CommandLineConfigurator.parse(cfgbuf2, "defaults",
                    new String[]{
                        "--one-boolean",
                        "--other-boolean=false",
                        "-one-int", "123",
                        "-list-of-strings1", "abc", "def",
                        "--array-of-strings1", "xyz", "pdq", "zzz",
                        "-named-values", "hello, world", "7", "true",
                        "-list-of-stuff", "a", "b", "c",
                        "-dep-test", "2",
                        "--child.last", "2",
                        "--middle", "3",
                        "--child.first", "10",
                        "--complex-thingie", "999", "hi hi", "true",
                        "--position=1,1",
                        "--florp", "2", "2",
                        "--position", "3", "3",
                        "hi", "there"
                    });
            TestConfiguration cfg2 = new TestConfiguration();
            cfgbuf2.commit(cfg2);
            cfg2.validate1();

            ConfigurationBuffer cfgbuf3 = new ConfigurationBuffer(TestConfiguration.class);
            FileConfigurator.load(cfgbuf3, new StringReader("<fnord>\n" +
                    "  <one-int>222</one-int>\n" +
                    "</fnord>\n"), "inlinefile", "fnord");
            CommandLineConfigurator.parse(cfgbuf3, "defaults", new String[]{"--one-int", "333"});

            TestConfiguration cfg3 = new TestConfiguration();
            cfgbuf3.commit(cfg3);
            cfg3.validate2();

        } catch (ConfigurationException e) {
            System.err.println(e.source + ":" + e.line + " " + e.getMessage());
            System.exit(1);
        }

    }


    protected void tearDown
            () throws Exception {

    }


    static class TestConfiguration {
        public void validate1() {
            assertTrue(getOneBoolean());
            assertTrue(!getOtherBoolean());
            assertEquals(123, getOneInt());
            assertTrue(getListOfStrings1().size() == 2);
            assertTrue(getListOfStrings1().get(1).equals("def"));
            assertTrue(getArrayOfStrings1().length == 3);
            assertTrue(getArrayOfStrings1()[1].equals("pdq"));
            assertTrue(getNamed().equals("world"));
            assertTrue(getListOfStuff().size() == 3);
            assertTrue(getListOfStuff().get(1).equals("b"));
            assertTrue(child.x == 26);
            assertTrue(deptest == 52);
            assertTrue(getDefaults().size() == 2);
            assertTrue(getDefaults().get(1).equals("there"));
            assertTrue(getComplexThingie().someBoolean);
            assertTrue(getComplexThingie().someInteger == 999);
            assertTrue(getComplexThingie().someString.equals("hi hi"));
            assertTrue(getPositionList().size() == 3);
            assertTrue(((Position) getPositionList().get(2)).x == 3);
        }

        public void validate2() {
            assertTrue(getOneInt() == 333);
        }

        /////////////////////////////////////////////////////////////////////////////
        private boolean b = false;

        public void cfgOneBoolean(ConfigurationValue cv, boolean b) {
            this.b = b;
        }

        public boolean getOneBoolean() {
            return b;
        }

        /////////////////////////////////////////////////////////////////////////////
        private boolean b1 = true;

        public void cfgOtherBoolean(ConfigurationValue cv, boolean b) {
            this.b1 = b;
        }

        public boolean getOtherBoolean() {
            return b1;
        }


        private String s;

        public void cfgOneString(ConfigurationValue cv, String s) {
            this.s = s;
        }

        public String getOneString() {
            return s;
        }

        /////////////////////////////////////////////////////////////////////////////
        private int i;

        public void cfgOneInt(ConfigurationValue cv, int i) {
            this.i = i;
        }

        public int getOneInt() {
            return i;
        }

        /////////////////////////////////////////////////////////////////////////////
        private List l;

        public void cfgListOfStrings1(ConfigurationValue cv, List l) {
            this.l = l;
        }

        public List getListOfStrings1() {
            return l;
        }

        private String[] sa;

        public void cfgArrayOfStrings1(ConfigurationValue cv, String[] a) {
            this.sa = a;
        }

        public String[] getArrayOfStrings1() {
            return sa;
        }

        String named;

        public void cfgNamedValues(ConfigurationValue cv, String s, int x, boolean b) {
            named = b ? s.substring(x) : s;
        }

        public static ConfigurationInfo getNamedValuesInfo() {
            return new ConfigurationInfo(new String[]{"s", "i", "b"});
        }

        public String getNamed() {
            return named;
        }

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

        int deptest = -1;

        public void cfgDepTest(ConfigurationValue cv, int mul) {
            deptest = child.x * mul;
        }

        public static ConfigurationInfo getDepTestInfo() {
            return new ConfigurationInfo() {
                public String[] getPrerequisites() {
                    return new String[]{"child.last"};
                }
            };
        }

        public List _defaults;      // default var...

        public void cfgDefaults(ConfigurationValue cv, List args) {
            _defaults = args;
        }

        public List getDefaults() {
            return _defaults;
        }

        public ChildConfiguration child = new ChildConfiguration();
        // this is a nested config, everything will be prefixed with "child."

        public ChildConfiguration getChildConfiguration() {
            return child;
        }

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

    static class ChildConfiguration {
        int x = 1;

        public void cfgLast(ConfigurationValue cv, int last) {
            x *= last;
        }

        public static ConfigurationInfo getLastInfo() {
            return new ConfigurationInfo() {
                public String[] getPrerequisites() {
                    return new String[]{"middle"};
                }
            };
        }

        public void cfgFirst(ConfigurationValue cv, int first) {
            x = first;
        }

        public void cfgMiddle(ConfigurationValue cv, int middle) {
            x += middle;
        }

        public static ConfigurationInfo getMiddleInfo() {
            return new ConfigurationInfo() {
                public String[] getPrerequisites() {
                    return new String[]{"first"};
                }
            };
        }

    }

}

