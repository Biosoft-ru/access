package ru.biosoft.access.file;

import java.util.List;
import java.util.Map;

public interface InfoProvider 
{
    /**
     * Returns properties for GenericFileDataCollection. 
     */
    public Map<String, Object> getProperties();

    /**
     * Returns properties for the specified file name.
     */
    public Map<String, Object> getFileInfo(String fileName);
    
    /**
     * Returns list of names for data collections that do not correspond files.
     * By this way GenericDataCollection can include not file based data collections,
     * for example SQLDataCollection.
     */
    public List<String> getDataCollections();
}
