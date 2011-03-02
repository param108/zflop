package flex2.compiler.config;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringReader;

public class SimpleTypesTest extends TestCase {

    public SimpleTypesTest() {
    }

    protected void setUp() throws Exception {

    }

    public static Test suite() {
        return new TestSuite(SimpleTypesTest.class);
    }


    public void testSystemPropertyConfigurator() {

        System.setProperty("flex.one-string", "foo");
        System.setProperty("flex.one-boolean", "true");
        System.setProperty("flex.two-boolean", "TRUE");
        System.setProperty("flex.three-boolean", "True");
        System.setProperty("flex.four-boolean", "trUe");
        System.setProperty("flex.five-boolean", "False");
        System.setProperty("flex.six-boolean", "false");
        System.setProperty("flex.one-int", "22");
        System.setProperty("flex.two-int", "-56");
        //System.setProperty("flex.three-int", "");
        //System.setProperty("flex.four-int", "garbage");
        //System.setProperty("flex.five-int", "6.67");


        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        String msg = "Testing Configurator Type: " + "SystemPropertyConfigurator";

        String s1 = cfg.getOneString();
        boolean b1 = cfg.getOneBoolean();
        boolean b2 = cfg.getTwoBoolean();
        boolean b3 = cfg.getThreeBoolean();
        boolean b4 = cfg.getFourBoolean();
        boolean b5 = cfg.getFiveBoolean();
        boolean b6 = cfg.getSixBoolean();
        int i1 = cfg.getOneInt();
        int i2 = cfg.getTwoInt();
        int i3 = cfg.getThreeInt();
        int i4 = cfg.getFourInt();
        int i5 = cfg.getFiveInt();


        assertEquals(msg, "foo", s1);
        assertEquals(msg, true, b1);
        assertEquals(msg, true, b2);
        assertEquals(msg, true, b3);
        assertEquals(msg, true, b4);
        assertEquals(msg, false, b5);
        assertEquals(msg, false, b6);
        assertEquals(msg, 22, i1);
        assertEquals(msg, -56, i2);
        assertEquals(msg, 0, i3);
        assertEquals(msg, 0, i4);
        assertEquals(msg, 0, i5);

    }

    public void testSystemPropertyConfiguratorNeg1() {
        System.setProperty("flex.three-int", "");
        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
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

    //FIXME - bad sysprops from prev test bleed over to this one!
    public void testSystemPropertyConfiguratorNeg2() {
        System.setProperty("flex.four-int", "garbage");
        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            assertEquals("configuration variable four-int requires an integer, got 'garbage'", e.getMessage());
            errorWasThrown = true;
        }
        assertTrue(errorWasThrown);
    }

    // fixme - bad sysprops from prev test bleed over to this one!
    public void testSystemPropertyConfiguratorNeg3() {
        System.setProperty("flex.five-int", "6.67");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            SystemPropertyConfigurator.load(cfgbuf, "flex");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            //e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.TypeMismatch);
            errorWasThrown = true;
        }
        assertTrue(errorWasThrown);
    }


    public void testCommandLineConfigurator() {

        String[] args = new String[]{
            "-one-string", "foo"
            , "-one-boolean"
            , "-three-boolean=True"
            , "-four-boolean=trUe"
            , "-five-boolean=False"
            , "-six-boolean=false"
            //, "-seven-boolean", "garbage"
            , "-one-int", "22"
            , "-two-int=-56"
            //, "-three-int", ""
            //, "-four-int", "garbage"
            //, "-five-int", "6.67"
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

        String msg = "Testing Configurator Type: " + "CommandLineConfigurator";

        String s1 = cfg.getOneString();

        boolean b1 = cfg.getOneBoolean();
        boolean b2 = cfg.getTwoBoolean();
        boolean b3 = cfg.getThreeBoolean();
        boolean b4 = cfg.getFourBoolean();
        boolean b5 = cfg.getFiveBoolean();
        boolean b6 = cfg.getSixBoolean();
        boolean b7 = cfg.getSevenBoolean();

        int i1 = cfg.getOneInt();
        int i2 = cfg.getTwoInt();
        int i3 = cfg.getThreeInt();
        int i4 = cfg.getFourInt();
        int i5 = cfg.getFiveInt();


        assertEquals(msg, "foo", s1);

        assertEquals(msg, true, b1);
        assertEquals(msg, false, b2);
        assertEquals(msg, true, b3);
        assertEquals(msg, true, b4);
        assertEquals(msg, false, b5);
        assertEquals(msg, false, b6);
        assertEquals(msg, false, b7);

        assertEquals(msg, 22, i1);
        assertEquals(msg, -56, i2);
        assertEquals(msg, 0, i3);
        assertEquals(msg, 0, i4);
        assertEquals(msg, 0, i5);

    }

    public void testCommandLineConfiguratorNeg1() {

        String[] args = new String[]{
            "-three-int", ""
        };

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

    public void testCommandLineConfiguratorNeg2() {

        String[] args = new String[]{
            "-four-int", "garbage"
        };

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        TestConfig cfg = new TestConfig();
        boolean errorWasThrown = false;
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            // e.printStackTrace();
            //System.out.println(e.getCode());
            assertTrue(e instanceof ConfigurationException.TypeMismatch);
            errorWasThrown = true;
        }
        assertTrue(errorWasThrown);
    }

    public void testCommandLineConfiguratorNeg3() {

        String[] args = new String[]{
            "-five-int", "6.67"
        };

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

    public void testFileConfigurator() {

        StringReader args = new StringReader("<fnord>"
                + "\n<one-string>foo</one-string>"
                + "\n<one-boolean>true</one-boolean>"
                + "\n<two-boolean>TRUE</two-boolean>"
                + "\n<three-boolean>True</three-boolean>"
                + "\n<four-boolean>trUe</four-boolean>"
                + "\n<five-boolean>False</five-boolean>"
                + "\n<six-boolean>false</six-boolean>"
                + "\n<one-int>22</one-int>"
                + "\n<two-int>-56</two-int>"
                // + "\n<three-int></three-int>"
                // + "\n<four-int>garbage</four-int>"
                // + "\n<five-int>6.67</five-int>"
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

        String msg = "Testing Configurator Type: " + "FileConfigurator";

        String s1 = cfg.getOneString();
        boolean b1 = cfg.getOneBoolean();
        boolean b2 = cfg.getTwoBoolean();
        boolean b3 = cfg.getThreeBoolean();
        boolean b4 = cfg.getFourBoolean();
        boolean b5 = cfg.getFiveBoolean();
        boolean b6 = cfg.getSixBoolean();
        int i1 = cfg.getOneInt();
        int i2 = cfg.getTwoInt();
        int i3 = cfg.getThreeInt();
        int i4 = cfg.getFourInt();
        int i5 = cfg.getFiveInt();

        assertEquals(msg, "foo", s1);
        assertEquals(msg, true, b1);
        assertEquals(msg, true, b2);
        assertEquals(msg, true, b3);
        assertEquals(msg, true, b4);
        assertEquals(msg, false, b5);
        assertEquals(msg, false, b6);
        assertEquals(msg, 22, i1);
        assertEquals(msg, -56, i2);
        assertEquals(msg, 0, i3);
        assertEquals(msg, 0, i4);
        assertEquals(msg, 0, i5);

    }

    public void testFileConfiguratorNeg1() {

        StringReader args = new StringReader("<fnord>"
                + "\n<three-int></three-int>"
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

    public void testFileConfiguratorNeg2() {

        StringReader args = new StringReader("<fnord>"
                + "\n<four-int>garbage</four-int>"
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

    public void testFileConfiguratorNeg3() {

        StringReader args = new StringReader("<fnord>"
                + "\n<five-int>6.67</five-int>"
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

    public void testFileConfiguratorNeg4() {

        StringReader args = new StringReader("<fnord>"
                + "\n<five-boolean>garbage</five-boolean>"
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

    protected void tearDown
            () throws Exception {

    }


    static class TestConfig {
        /////////////////////////////////////////////////////////////////////////////
        private boolean b1 = false;

        public void cfgOneBoolean(ConfigurationValue cv, boolean b) {
            this.b1 = b;
        }

        public boolean getOneBoolean() {
            return b1;
        }

        private boolean b2 = false;

        public void cfgTwoBoolean(ConfigurationValue cv, boolean b) {
            this.b2 = b;
        }

        public boolean getTwoBoolean() {
            return b2;
        }

        private boolean b3 = false;

        public void cfgThreeBoolean(ConfigurationValue cv, boolean b) {
            this.b3 = b;
        }

        public boolean getThreeBoolean() {
            return b3;
        }

        private boolean b4 = false;

        public void cfgFourBoolean(ConfigurationValue cv, boolean b) {
            this.b4 = b;
        }

        public boolean getFourBoolean() {
            return b4;
        }

        private boolean b5 = false;

        public void cfgFiveBoolean(ConfigurationValue cv, boolean b) {
            this.b5 = b;
        }

        public boolean getFiveBoolean() {
            return b5;
        }

        private boolean b6 = false;

        public void cfgSixBoolean(ConfigurationValue cv, boolean b) {
            this.b6 = b;
        }

        public boolean getSixBoolean() {
            return b6;
        }

        private boolean b7 = false;

        public void cfgSevenBoolean(ConfigurationValue cv, boolean b) {
            this.b7 = b;
        }

        public boolean getSevenBoolean() {
            return b7;
        }

        /////////////////////////////////////////////////////////////////////////////
        private String s;

        public void cfgOneString(ConfigurationValue cv, String s) {
            this.s = s;
        }

        public String getOneString() {
            return s;
        }

        /////////////////////////////////////////////////////////////////////////////
        private int i1;

        public void cfgOneInt(ConfigurationValue cv, int i) {
            this.i1 = i;
        }

        public int getOneInt() {
            return i1;
        }

        private int i2;

        public void cfgTwoInt(ConfigurationValue cv, int i) {
            this.i2 = i;
        }

        public int getTwoInt() {
            return i2;
        }

        private int i3;

        public void cfgThreeInt(ConfigurationValue cv, int i) {
            this.i3 = i;
        }

        public int getThreeInt() {
            return i3;
        }

        private int i4;

        public void cfgFourInt(ConfigurationValue cv, int i) {
            this.i4 = i;
        }

        public int getFourInt() {
            return i4;
        }

        private int i5;

        public void cfgFiveInt(ConfigurationValue cv, int i) {
            this.i5 = i;
        }

        public int getFiveInt() {
            return i5;
        }

    }


}




