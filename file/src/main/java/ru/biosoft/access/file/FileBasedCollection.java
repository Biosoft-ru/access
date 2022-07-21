package ru.biosoft.access.file;

import java.io.File;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Collection where each internal element is represented as file 
 * @author lan
 */
public interface FileBasedCollection<T extends DataElement> extends DataCollection<T>
{
    public boolean isFileAccepted(File file);
    public File getChildFile(String name);
}
