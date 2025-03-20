package ru.biosoft.access.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implements InfoProvider that stores all information in memory.
 */
public class MemoryInfoProvider implements InfoProvider 
{
	protected Properties properties = new Properties();
	
	protected Map<String, Map<String, Object>> fileInfoByName = new LinkedHashMap<>();
	
	protected List<String> collections = new ArrayList<>();
	
    public Properties getProperties()
    {
        return properties;
    }

    public Map<String, Object> getFileInfo(String fileName)
    {
        return fileInfoByName.get(fileName);
    }
    
	@Override
	public void setFileInfo(Map<String, Object> fileInfo) throws Exception {
		String name = (String) fileInfo.get("name");
		fileInfoByName.put(name, fileInfo);
	}
    
    public List<String> getDataCollections()
    {
        return collections;
    }

    
    private List<InfoProviderListener> listeners = new ArrayList<>();
    
	@Override
	public void addListener(InfoProviderListener l) {
		listeners.add(l);
	}
	
	protected void fireInfoChanged() throws Exception
	{
		for(InfoProviderListener l : listeners)
			l.infoChanged();
	}

	@Override
	public void close() throws Exception {
		
	}


}