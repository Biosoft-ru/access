package ru.biosoft.access.core;

/**
 * DataCollection representing symbolic link to another DataCollection.
 */
public class SymbolicLinkDataCollection extends DerivedDataCollection<DataElement, DataElement>
{
    private DataElementPath target;
    private SymbolicLinkDataCollectionListener listener;
    
    private final class SymbolicLinkDataCollectionListener implements DataCollectionListener
    {
        @Override
        public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            if(primaryCollection.isPropagationEnabled())
                propagateElementWillChange(primaryCollection, e);
        }
        @Override
        public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            if(primaryCollection.isPropagationEnabled())
                propagateElementWillChange(primaryCollection, e);
        }
        @Override
        public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            if(primaryCollection.isPropagationEnabled())
                propagateElementWillChange(primaryCollection, e);
        }
        @Override
        public void elementRemoved(DataCollectionEvent e) throws Exception
        {
            if(primaryCollection.isPropagationEnabled())
                propagateElementChanged(primaryCollection, e);
        }
        @Override
        public void elementChanged(DataCollectionEvent e) throws Exception
        {
            if(primaryCollection.isPropagationEnabled())
                propagateElementChanged(primaryCollection, e);
        }
        @Override
        public void elementAdded(DataCollectionEvent e) throws Exception
        {
            if(primaryCollection.isPropagationEnabled())
                propagateElementChanged(primaryCollection, e);
        }
    }

    public SymbolicLinkDataCollection(DataCollection<?> origin, String name, DataElementPath target)
    {
        super(origin, name, null, null);
        v_cache = null;
        this.target = target;
    }

    @Override
    protected void init()
    {
    }

    @Override
    protected synchronized DataCollection<DataElement> doGetPrimaryCollection()
    {
        if(primaryCollection == null)
        {
            try
            {
                primaryCollection = (DataCollection)target.getTargetElement();
                listener = new SymbolicLinkDataCollectionListener();
                primaryCollection.addDataCollectionListener(listener);
            }
            catch(Exception e) {}
        }
        return primaryCollection;
    }
    
    public DataElementPath getTarget()
    {
        return target;
    }

    @Override
    public void close() throws Exception
    {
        if(primaryCollection != null && listener != null)
        {
            primaryCollection.removeDataCollectionListener(listener);
        }
    }
}
