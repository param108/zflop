package flash.localization;

import junit.framework.TestCase;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Roger Gonzalez
 * fixme: need to somehow automatically install the test files into the appropriate places under classes or whatever.
 *
 */
public class LocalizationTest extends TestCase
{
    private LocalizationManager mgr;
    protected void setUp() throws Exception
    {
        mgr = new LocalizationManager();

        String root = new File("L10N_test").getAbsolutePath();

        mgr.addLocalizer( new XLRLocalizer( root ));
        mgr.addLocalizer( new ResourceBundleLocalizer() );
    }


    public void testOne()
    {
        Map params = new HashMap();
        params.put( "what", "test" );
        String result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.bar.xyzzy.one", params );

        assertTrue("hello, world! this is a test!".equals( result ));
    }

    public void testTwo()
    {
        Map params = new HashMap();
        params.put( "formal", new Boolean( true ));
        String result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.whatever.two", params );
        assertTrue("Hello, world!".equals( result ));

        params.put( "formal", "false" );
        result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.whatever.two", params );
        assertTrue("Hiya, world!".equals( result ));
    }

    public void testThree()
    {
        Map params = new HashMap();
        params.put( "count", new Integer(0) );
        String result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.whatever.three", params );
        assertTrue("There were no files.".equals( result ));
        params.put( "count", new Integer(1) );
        result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.whatever.three", params );
        assertTrue("There was one file.".equals( result ));
        params.put( "count", new Integer(100) );
        result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.whatever.three", params );
        assertTrue("There were 100 files.".equals( result ));
    }

    public void testFour()
    {
        Map params = new HashMap();
        params.put( "thing", "test" );
        String result = mgr.getLocalizedTextString( Locale.getDefault(), "foo.rb.four", params );
        assertTrue( "This test is wacky.".equals( result ) );
    }
}
