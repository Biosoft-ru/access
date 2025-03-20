package ru.biosoft.access.file;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface InfoProvider
{
    /**
     * Returns properties for GenericFileDataCollection.
     * These properties can be used to create GenericFileDataCollection. 
     */
    public Properties getProperties();

    /**
     * Returns properties for the specified file name.
     */
    public Map<String, Object> getFileInfo(String fileName);
    
    public void setFileInfo(Map<String, Object> fileInfo) throws Exception;
    
    /**
     * Returns list of names for data collections that do not correspond files.
     * By this way GenericDataCollection can include not file based data collections,
     * for example SQLDataCollection.
     */
    public List<String> getDataCollections();
    
    public void addListener(InfoProviderListener l);
    
    void close() throws Exception;
}
