package ru.biosoft.access.file;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileTypeRegistryImpl
{
    protected static final Logger log = Logger.getLogger( FileTypeRegistryImpl.class.getName() );

    protected static Map<String, FileType> byName = new HashMap<>();
    protected static Map<String, FileType> byExtension = new HashMap<>();

    public void register(FileType fileType)
    {
        byName.put( fileType.name, fileType );

        for ( String extension : fileType.getExtensions() )
        {
            if( !byExtension.containsKey( extension ) )
                byExtension.put( extension, fileType );
            else
            {
                FileType ft = byExtension.get( extension );

                if( ft.getPriority().isHigher( fileType.getPriority() ) )
                    continue;
                else if( fileType.getPriority().isHigher( ft.getPriority() ) )
                    byExtension.put( extension, fileType );

                else // ft.getPriority() == fileType.getPriority()
                    log.warning( "FileTypeRegistry: extension '" + extension + "'" + "corresponds to 2 file types with the same priority " + ft.getPriority()
                            + System.lineSeparator() + "FileType (used):    " + ft + System.lineSeparator() + "FileType (skipped): " + fileType );
            }
        }
    }

    public FileType getFileType(String name)
    {
        return byName.get( name );
    }

    public FileType getFileTypeByExtension(String extension)
    {
        return byExtension.get( extension );
    }

    public FileType getFileTypeByTransformer(String transformerClassName)
    {
        if( transformerClassName == null )
            return FileTypeRegistry.FILE_TYPE_BINARY;
        return byName.values().stream().filter( ft -> transformerClassName.equals( ft.getTransformerClassName() ) ).findFirst().orElse( null );
    }

    public Stream<FileType> fileTypes()
    {
        return byName.values().stream();
    }
}
