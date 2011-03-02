/**
 * Copyright (c) 2004 Macromedia, Inc. All rights reserved.
 * DO NOT REDISTRIBUTE THIS SOFTWARE IN ANY WAY WITHOUT THE EXPRESSED
 * WRITTEN PERMISSION OF MACROMEDIA.
 */
package flex2.compiler.swc;

import flex2.compiler.io.VirtualFile;
import flex2.compiler.io.InMemoryFile;
import flex2.compiler.io.FileUtil;
import flex2.compiler.util.MimeMappings;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * Tests Swc and SwcCache classes
 *
 * @author Brian Deitte
 */
public class SwcTest extends TestCase
{
    private static int LOAD_TIME = 100;  // short for JUnit, but make longer if you want a better test

    private static String path = "." + File.separator;
    public static final String SIMPLE_SWC = path + "test1A.swc";
    public static final String SIMPLE_SWC2 = path + "test1B.swc";
    public static final String SIMPLE_SWC3 = path + "test1C.swc";
    public static final String FILE_SWC = path + "test2A.swc";

    public static Test suite()
    {
        return new TestSuite(SwcTest.class);
    }

    protected void setUp() throws Exception
    {
        // create simple Swcs
        SwcDynamicArchive archive = new SwcDynamicArchive(SIMPLE_SWC);
        Swc swc = new Swc(archive, false);
        swc.save();
        archive = new SwcDynamicArchive(SIMPLE_SWC2);
        swc = new Swc(archive, false);
        swc.save();
        archive = new SwcDynamicArchive(SIMPLE_SWC3);
        swc = new Swc(archive, false);
        swc.save();

        //  create Swcs with files
        archive = new SwcDynamicArchive(FILE_SWC);
        swc = new Swc(archive, false);
        File file = new File(SIMPLE_SWC);
        VirtualFile swcFile = new InMemoryFile(file.toURL().openStream(), SIMPLE_SWC,
                                               MimeMappings.getMimeType(SIMPLE_SWC), file.lastModified());
        swc.addFile(swcFile);
        swc.save();
    }

    protected void tearDown() throws Exception
    {
        File file = new File(SIMPLE_SWC);
        file.delete();
        file = new File(SIMPLE_SWC2);
        file.delete();
        file = new File(SIMPLE_SWC3);
        file.delete();
        file = new File(FILE_SWC);
        file.delete();
    }

    public static void testSwc() throws Exception
    {
        SwcArchive archive = new SwcDynamicArchive(SIMPLE_SWC);
        Swc swc = new Swc(archive, true);
        archive = new SwcDynamicArchive(SIMPLE_SWC2);
        swc = new Swc(archive, true);
        archive = new SwcDynamicArchive(SIMPLE_SWC3);
        swc = new Swc(archive, true);
        archive = new SwcDynamicArchive(FILE_SWC);
        swc = new Swc(archive, true);
        VirtualFile file = swc.getFile(SIMPLE_SWC);
        assertNotNull(SIMPLE_SWC + " not found in " + FILE_SWC, file);
    }

    public static void testSwcOverwrite() throws Exception
    {
        SwcArchive archive = new SwcDynamicArchive(SIMPLE_SWC);
        Swc swc = new Swc(archive, false);
        swc.save();
        archive = new SwcDynamicArchive(SIMPLE_SWC);
        swc = new Swc(archive, true);
        swc.save();
    }

    public static void testSwcCache() throws Exception
    {
        SwcCache cache = new SwcCache();
        SwcGroup group = cache.getSwcGroup(new String[] { path });
        Map swcs = group.getSwcs();
        assertTrue("Found " + swcs.size() + " SWCs but expecting 4.", swcs.size() >= 4);
    }

    public static void testSwcCacheUnderLoad() throws Exception
    {
        //System.out.println("Starting SwcCache load test...");
        SwcCache cache = new SwcCache();
        LoadTest ls1 = new LoadTest(path, cache, 0);
        ls1.start();
        LoadTest ls2 = new LoadTest(path, cache, 2);
        ls2.start();
        LoadTest ls3 = new LoadTest(path, cache, 2);
        ls3.start();

        Thread.sleep(LOAD_TIME);
        LoadTest.stop = true;
        Thread.sleep(LOAD_TIME / 5);
        assertTrue("SwcCache load test failed.", ! (ls1.error || ls2.error || ls3.error));
        //System.out.println("SwcCache load test passed with " + LoadTest.count + " loads or saves over " + LOAD_TIME + " ms");
    }

    static class LoadTest extends Thread
    {
        public static boolean stop;
        public boolean error;
        private String path;
        private SwcCache cache;
        private boolean debug = false;
        private int id;
        private int type = 0;
        private static int num = 0;
        public static int count = 0;

        public LoadTest(String path, SwcCache cache, int type)
        {
            this.path = path;
            this.cache = cache;
            this.type = type;
            num = num + 1;
            id = num;
        }

        public void run()
        {
            try
            {
                while(! stop)
                {
                    boolean export = type == 1;
                    if (type == 0)
                        export = Math.random() > .5;

                    if (export)
                    {
                        SwcDynamicArchive archive = new SwcDynamicArchive(SIMPLE_SWC);
                        Swc swc = new Swc(archive, false);
                        cache.export(swc);
                        if (debug)
                        {
                            System.err.println("(" + id + ": Exported " + swc.getLocation() + ")");
                        }
                    }
                    else
                    {
                        SwcGroup group = cache.getSwcGroup(new String[] { path });
                        //SwcGroup group = cache.getSwcGroup(new String[] { path, path + "/frameworks" });
                        int size = group.getSwcs().size();
                        if (debug)
                        {
                            System.err.println("(" + id + ": Loaded " + size + " from " + path + ")");
                        }

                        if (size < 4)
                        {
                            System.err.println(size + " found instead of 4.");
                            for (Iterator it = group.getSwcs().values().iterator(); it.hasNext();)
                            {
                                Swc swc = (Swc)it.next();
                                System.out.println("Found " + swc.getLocation());
                            }
                            stop = true;
                            error = true;
                        }
                    }
                    count = ++count;
                    Thread.sleep((long)Math.random() * 100);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                stop = true;
                error = true;
            }
        }
    }
}
