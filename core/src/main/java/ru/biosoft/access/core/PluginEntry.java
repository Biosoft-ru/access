package ru.biosoft.access.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class PluginEntry implements Comparable<PluginEntry>
{
    public abstract PluginEntry[] children() throws IOException;
    public abstract PluginEntry child(String name);
    public abstract PluginEntry getParent();
    public abstract String getName();
    public abstract boolean is(File file);
    public abstract boolean isDirectory();
    public abstract InputStream getInputStream() throws IOException;
    public abstract boolean exists();
    public abstract File getFile();
    public abstract File extract() throws IOException;
    
    @Override
    public int compareTo(PluginEntry o)
    {
        return getName().compareTo( o.getName() );
    }
}