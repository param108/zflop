////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.swc;

/**
 * The features enabled for a Swc
 *
 * @author Brian Deitte
 */
public class Features
{
    private boolean debug;
    private boolean externalDeps = false;
    private boolean scriptDeps = true;

    public boolean isComponents()
    {
        return components;
    }

    public void setComponents( boolean components )
    {
        this.components = components;
    }

    public boolean isFiles()
    {
        return files;
    }

    public void setFiles( boolean files )
    {
        this.files = files;
    }

    private boolean files = false;
    private boolean components = false;
    //private boolean methodDeps;

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public boolean hasExternalDeps()
    {
        return externalDeps;
    }

    public void setExternalDeps(boolean externalDeps)
    {
        this.externalDeps = externalDeps;
    }

    public boolean isScriptDeps()
    {
        return scriptDeps;
    }

    public void setScriptDeps(boolean scriptDeps)
    {
        this.scriptDeps = scriptDeps;
    }


    /*public boolean isMethodDeps()
    {
        return methodDeps;
    }

    public void setMethodDeps(boolean methodDeps)
    {
        this.methodDeps = methodDeps;
    }
    */
}
