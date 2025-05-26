package ru.biosoft.access.file;

import java.io.IOException;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.v1.FileDataCollectionV1;

/**
 * Compatibility package for BioUML for FileDataCollection.
 */
@Deprecated
public class FileDataCollection extends FileDataCollectionV1 
{
    /** Constructor used by biouml framework. */
    public FileDataCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super(parent, properties);
    }
}
