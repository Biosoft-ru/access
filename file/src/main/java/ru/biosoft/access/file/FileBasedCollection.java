package ru.biosoft.access.file;

import java.io.File;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Collection where internal elements generally are represented as file.
 */
public interface FileBasedCollection<T extends DataElement> extends DataCollection<T>
{
    public boolean isFileAccepted(File file);
    public File getChildFile(String name);
}
