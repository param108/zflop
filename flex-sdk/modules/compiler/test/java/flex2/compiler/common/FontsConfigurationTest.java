package flex2.compiler.common;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.StringReader;

import flex2.compiler.common.FontsConfiguration;
import flex2.compiler.config.ConfigurationBuffer;
import flex2.compiler.config.FileConfigurator;
import flex2.compiler.config.ConfigurationException;
import flex2.compiler.config.CommandLineConfigurator;
import flex2.compiler.config.ConfigurationValue;
import flex2.compiler.util.ThreadLocalToolkit;

public class FontsConfigurationTest extends TestCase {


    public FontsConfigurationTest() {
    }

    public static Test suite() {
        return new TestSuite(FontsConfigurationTest.class);
    }


    public void testFileConfigurator() {
        StringReader args = new StringReader(""
                + "\n<myconfig>"
                + "\n<fonts>"
                + "\n   <max-cached-fonts>20</max-cached-fonts>"
                + "\n   <max-glyphs-per-face>1000</max-glyphs-per-face>"
                + "\n   <managers>"
                + "\n       <manager-class>macromedia.fonts.JREFontManager</manager-class>"
                + "\n       <manager-class>macromedia.fonts.BatikFontManager</manager-class>"
                + "\n   </managers>"
                + "\n   <languages>"
                + "\n       <language-range>"
                + "\n           <lang>en</lang>"
                + "\n           <range>U+0020-U+007E</range>"
                + "\n       </language-range>"
                + "\n       <language-range>"
                + "\n           <range>U+0020-U+007E</range>"
                + "\n           <lang>blah</lang>"
                + "\n       </language-range>"
                + "\n   </languages>"
                + "\n</fonts>"
                + "\n</myconfig>\n");

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(MyConfig.class);
        MyConfig cfg = new MyConfig();
        try {
            FileConfigurator.load(cfgbuf, args, "fakefile", "myconfig");
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        System.out.println(FileConfigurator.formatBuffer(cfgbuf, "myconfig"));
        System.out.println(CommandLineConfigurator.usage("myconfig", "defaultVar", cfgbuf, null,
                                                         ThreadLocalToolkit.getLocalizationManager(), ""));
    }


    public void CommandLineConfiguratorLeaf() {
        String[] args = new String[]{"-managers", "foo","bar", "-max-cached-fonts","10"};

        ConfigurationBuffer cfgbuf = new ConfigurationBuffer(MyConfig.class);
        //System.out.println(CommandLineConfigurator.longUsage(cfgbuf, null));

        MyConfig cfg = new MyConfig();
        try {
            CommandLineConfigurator.parse(cfgbuf, null, args);
            cfgbuf.commit(cfg);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            fail(e.toString());
        }


        assertEquals("10", cfg.getFontsConfiguration().getMaxCachedFonts());
        assertEquals("foo", cfg.getFontsConfiguration().getManagers().get(0));
        assertEquals("bar", cfg.getFontsConfiguration().getManagers().get(1));

    }

    public void CommandLineConfiguratorAlias() {
            String[] args = new String[]{"-fman","abc.def","efg.hij"};

            ConfigurationBuffer cfgbuf = new ConfigurationBuffer(MyConfig.class);
            cfgbuf.addAlias("fman","fonts.managers");
            //System.out.println(CommandLineConfigurator.longUsage(cfgbuf, null));

            MyConfig cfg = new MyConfig();
            try {
                CommandLineConfigurator.parse(cfgbuf, null, args);
                cfgbuf.commit(cfg);
            } catch (ConfigurationException e) {
                e.printStackTrace();
                fail(e.toString());
            }

            assertEquals("abc.def", cfg.getFontsConfiguration().getManagers().get(0));
            assertEquals("efg.hij", cfg.getFontsConfiguration().getManagers().get(1));
        }


    public static class MyConfig {
        //this is how I would implement connecting the child font config configuration within the mxmlc configuration class - kq
        public FontsConfiguration fonts = new FontsConfiguration();

        public FontsConfiguration getFontsConfiguration() {
            return fonts;
        }
    }

}



