package ru.biosoft.access.file;

import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;

/**
 * Implements InfoProvider that stores all information in memory.
 */
public class MemoryInfoProvider implements InfoProvider 
{
    /**
     * Returns properties for GenericFileDataCollection. 
     */
    public Map<String, Object> getProperties()
    {
        return null;
    }

    /**
     * Returns properties for the specified file name.
     */
    public Map<String, Object> getFileInfo(String fileName)
    {
        return null;
    }
    
    /**
     * Returns list of names for data collections that do not correspond files.
     * By this way GenericDataCollection can include not file based data collections,
     * for example SQLDataCollection.
     */
    public List<String> getDataCollections()
    {
        return null;
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

}