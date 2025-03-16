package ru.biosoft.access.file;

import java.io.File;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.Environment;

public class FileTypeRegistry 
{
    /** Key for class in Environment properties. */
    public static final String FILE_TYPE_REGISTRY_CLASS = "FILE_TYPE_REGISTRY_CLASS";

    public static final FileType FILE_TYPE_TEXT = new FileType(
            new String[] {"", "txt"}, "ru.biosoft.access.file.FileTextTransformer",
            DataElementImporter.ACCEPT_LOW_PRIORITY, "Text file.");

    public static final FileType FILE_TYPE_BINARY = new FileType(
            new String[] {""}, null,
            DataElementImporter.ACCEPT_LOWEST_PRIORITY, "Binary file.");
    
    protected static final Logger log = Logger.getLogger( FileTypeRegistry.class.getName() );
    private static FileTypeRegistryImpl registry;
    
    protected static void checkRegistry()
    {
        if( registry == null )
        {
            if( Environment.getValue(FILE_TYPE_REGISTRY_CLASS) == null  )
            {
                registry = new FileTypeRegistryImpl(); 
                log.fine("FileTypeRegistry uses default FileTypeRegistryImpl.");
            }
            else
            {
                try
                {
                    Class<? extends FileTypeRegistryImpl> registryClass = (Class<? extends FileTypeRegistryImpl>)Environment.getValue(FILE_TYPE_REGISTRY_CLASS);
                    registry = registryClass.getDeclaredConstructor().newInstance();
                }
                catch(Throwable t)
                {
                    log.severe("FileTypeRegistry can not instantiate FileTypeRegistryImpl, class=" + 
                               Environment.getValue(FILE_TYPE_REGISTRY_CLASS) + ", error: " + t);
                }
            }
        }
    }

    public static void register(FileType fileType)
    {
        checkRegistry();
        registry.register(fileType);
    }

    public static FileType getFileType(File file)
    {
        checkRegistry();

        String fileName = file.getName(); 
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) 
            extension = fileName.substring(i+1);
        
        FileType ft = registry.getFileType(extension); 
        if( ft != null )
            return ft;

        if( true )// todo FileImporter.isTextFile(file) )
            return FILE_TYPE_TEXT;
        
        return FILE_TYPE_BINARY;
    }
    
}
