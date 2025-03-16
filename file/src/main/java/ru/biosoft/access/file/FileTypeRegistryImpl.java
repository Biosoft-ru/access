package ru.biosoft.access.file;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Default FileTypeRegistryImpl implementation.
 * It is the application responsibility to register corresponding file types. 
 */
public class FileTypeRegistryImpl 
{
    protected static Logger log = Logger.getLogger(FileTypeRegistryImpl.class.getName() );
    protected Map<String, FileType> fileTypeMap = new HashMap<>();
    
    public void register(FileType fileType)
    {
        for(String extension : fileType.getExtensions())
        {
            if( !fileTypeMap.containsKey(extension) )
                fileTypeMap.put(extension, fileType);
            else
            {
                FileType ft = fileTypeMap.get(extension);

                if( ft.getPriority() > fileType.getPriority() )
                    continue;
                
                else if(ft.getPriority() < fileType.getPriority() )
                    fileTypeMap.put(extension, fileType);
                
                else // ft.getPriority() == fileType.getPriority() 
                    log.warning("FileTypeRegistry: extension '" + extension + "'" +
                                "corresponds to 2 file types with the same priority " + ft.getPriority() + 
                                System.lineSeparator() + "FileType (used):    " + ft + 
                                System.lineSeparator() + "FileType (skipped): " + fileType ); 
            }
            
        }
    }
    
    public FileType getFileType(String extension)
    {
        return fileTypeMap.get(extension);
    }
}
