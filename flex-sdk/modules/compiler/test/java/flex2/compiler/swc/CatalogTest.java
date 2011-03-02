/**
 * Copyright (c) 2004 Macromedia, Inc. All rights reserved.
 * DO NOT REDISTRIBUTE THIS SOFTWARE IN ANY WAY WITHOUT THE EXPRESSED
 * WRITTEN PERMISSION OF MACROMEDIA.
 */
package flex2.compiler.swc;

import flex2.compiler.swc.catalog.CatalogWriter;
import flex2.compiler.swc.catalog.CatalogReader;
import flex2.compiler.io.VirtualFile;
import flex2.compiler.io.InMemoryFile;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for SWC catalog reading/writing
 *
 * @author Brian Deitte
 */
public class CatalogTest extends TestCase
{
    public static Test suite()
    {
        return new TestSuite(CatalogTest.class);
    }

    public static void testSimpleCatalog() throws Exception
    {
        // TEST 1: check simple catalog for equality
        CatalogItems items = new CatalogItems();
        items.features = new Features();
        readWriteEquality(items, false);
    }

    public static void testComplexCatalog() throws Exception
    {
        // TEST 2: check complicated catalog for equality
	    // add versions
	    CatalogItems items = new CatalogItems();
	    items.versions = new Versions();
	    items.versions.setFlexVersion((long)2.0);
	    items.versions.setFlexBuild((long)2000.0);
        // add features
        items.features = new Features();
        items.features.setDebug(true);
        items.features.setScriptDeps(true);
        // add files
        VirtualFile file = new InMemoryFile("foobarblah".getBytes(), "virtfile", "text", 0);
        items.files.put(file.getName(), file);
        file = new InMemoryFile("test2".getBytes(), "virtfile2", "text", 0);
        items.files.put(file.getName(), file);
        // add components
        Component comp = new Component("classname", "name", "uri");
        comp.setIcon("icon");
        comp.setPreview("preview");
        items.components.put(comp.getClassName(), comp);
        // add libraries
        TestingSwcLibrary library = new TestingSwcLibrary(null, "path_to_swclibrary");
        Set defs = new TreeSet();
        defs.add("def1");
        defs.add("def2");
        defs.add("foo");
        defs.add("blah.blah.foo");
        SwcDependencySet depSet = new SwcDependencySet();
        depSet.addDependency(SwcDependencySet.EXPRESSION, "def1");
        depSet.addDependency(SwcDependencySet.EXPRESSION, "expr");
        depSet.addDependency(SwcDependencySet.INHERITANCE, "inh");
        depSet.addDependency(SwcDependencySet.NAMESPACE, "nam");
        depSet.addDependency(SwcDependencySet.SIGNATURE, "sig");
        library.addScript("swcscript1", defs, depSet, 123456);
        items.libraries.put(library.getPath(), library);

        readWriteEquality(items, false);
    }

    public static void testErrorOnRequired() throws Exception
    {
        // TEST 3: make sure error throw on required attribute
        CatalogItems items = new CatalogItems();
        Component comp = new Component(null, null, null);
        items.components.put("name", comp);
        try
        {
            readWriteEquality(items, false);
            assertTrue("Expected error not thrown!", false);
        }
        catch (Exception e)
        {

            System.out.println("Error correctly thrown: " + e.toString());
        }
    }

    public static void testErrorOnBadCatalog() throws Exception
    {
        // TEST 4- make sure error thrown on incorrect catalog
        try
        {
            String str = "<script foo='33' >";
            ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
            CatalogReader reader = new CatalogReader(stream, null, null);
            reader.read();
            stream.close();
            assertTrue("Excepted error not thrown!", false);
        }
        catch (Exception e)
        {
            System.out.println("Error correctly thrown: " + e.toString());
        }
    }

    private static void readWriteEquality(CatalogItems items, boolean debug)
            throws Exception
    {
        StringWriter strWriter = new StringWriter();
        CatalogWriter writer = new CatalogWriter(strWriter, items.versions, items.features, items.components.values(),
		                                         items.libraries.values(), items.files.values());
        writer.write();
        String catalog1 = strWriter.toString();

        CatalogItems readItems = new CatalogItems();
        try
        {
            ByteArrayInputStream stream = new ByteArrayInputStream(catalog1.getBytes());
            CatalogReader reader = new CatalogReader(stream, null, null);
            reader.read();
            readItems.features = reader.getFeatures();
            readItems.components = reader.getComponents();
            readItems.libraries = reader.getLibraries();
            readItems.files = reader.getFiles();
            stream.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.err.println("Catalog that could not be parsed:");
            System.err.println(catalog1);
        }
        strWriter = new StringWriter();
        writer = new CatalogWriter(strWriter, readItems.versions, readItems.features, readItems.components.values(),
		                           readItems.libraries.values(), readItems.files.values());
        writer.write();
        String catalog2 = strWriter.toString();

        boolean equal = catalog1.equals(catalog2);
        if (! equal || debug)
        {
            System.out.println(catalog1);
            System.out.println("-----");
            System.out.println(catalog2);
            System.out.println("-----");
        }
        assertTrue("Catalogs above are NOT equal", equal);
    }

    static class CatalogItems
    {
	    public Versions versions;
        public Features features;
        public Map components = new HashMap();
        public Map libraries = new HashMap();
        public Map files = new HashMap();
    }

    static class TestingSwcLibrary extends SwcLibrary
    {
        public TestingSwcLibrary( Swc swc, String path )
        {
            super(swc, path);
        }

        protected void parse()
        {
        }
    }
}
