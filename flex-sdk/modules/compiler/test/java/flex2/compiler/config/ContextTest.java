package flex2.compiler.config;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;

public class ContextTest extends TestCase {
    String someFileName;
    String someDir;
    String someFile;
    StringReader args;

    public ContextTest() {
    }

    protected void setUp() throws Exception {

        someFileName = "somefile.xml";
        someFile = new File(this.getClass().getResource("").getPath() + someFileName).getAbsolutePath();
        someDir = new File(this.getClass().getResource("").getPath()).getAbsolutePath();

        File f = new File(someFile);
        String text = "foo";
        PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)));
        out.write(text);
        out.close();

        args = new StringReader("<fnord>"
                + "\n<string>" + someFileName + "</string>"
                + "</fnord>\n");

    }

    public static Test suite
            () {
        return new TestSuite(ContextTest.class);
    }

    public void testFileCfgStr() {

        TestConfig cfg = new TestConfig();

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(TestConfig.class);
        try {
            FileConfigurator.load(cfgbuf, args, someFileName, someDir, "fnord");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }


    }


    protected void tearDown() throws Exception {

    }

    class TestConfig {

        private String s1;

        public void cfgString(ConfigurationValue cv, String s) {
            this.s1 = s;
        }

        public String getString() {
            return s1;
        }


    }

}