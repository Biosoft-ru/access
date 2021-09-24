package ru.biosoft.access.core.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.ChunkedList;

/**
 * TODO high document and test
 */
public class FilteredDataCollection<T extends DataElement> extends DerivedDataCollection<T,T> implements DataCollectionListener, SortableDataCollection<T>
{
    /** Key for setting filter in properties. */
    public static final String FILTER_PROPERTY = "filter";
    private Filter<? super T> filter;

    /** Determines is names in filteredNames list are sorted.  */
    private boolean sorted = false;

    /**
     * Constructor to be used by {@link CollectionFactory}.
     *
     * Obligatory properties are
     * <ul>
     * <li>{@link ru.biosoft.access.core.DataCollectionConfigConstants#NAME_PROPERTY}</li>
     * <li>{@link ru.biosoft.access.core.DataCollectionConfigConstants#PRIMARY_COLLECTION}</li>
     * <li>{@link #FILTER_PROPERTY}</li>
     * </ul>
     *
     * @param parent filtered data collection parent.
     * @param properties Properties for filtered collection.
     */
    public FilteredDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);

        Object filterProp = properties.get(FILTER_PROPERTY);
        if( filterProp instanceof Filter )
            filter = (Filter<? super T>)filterProp;
        else if( filterProp instanceof String )
        {
            try
            {
                String plugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                Class<? extends Filter> filterClass = Environment.loadClass( (String)filterProp, plugins, Filter.class );
                filter = filterClass.newInstance();
            }
            catch( Exception e )
            {
                log.severe(this+": Can not instantiate filter " + ExceptionRegistry.log(e));
            }
        }

        init(filter, (FunctionJobControl)properties.get(DataCollectionConfigConstants.JOB_CONTROL_PROPERTY));
    }

    /**
     * Creates FilteredDataCollection with the specified parent, name,
     * primary data collection and filter.
     *
     * @param parent Parent for this data collection.
     * @param name Name of this data collection.
     * @param primaryCollection Primary data collection.
     * @param filter Filter to be applied to primary data collection.
     * @param properties Properties to initialize {@link DataCollectionInfo}. Can be null.
     */
    public FilteredDataCollection(DataCollection<?> parent, String name, DataCollection<T> primaryCollection, Filter<? super T> filter,
            FunctionJobControl jobControl, Properties properties)
    {
        super(parent, name, primaryCollection, properties);
        init(filter, jobControl);
    }
    
    public FilteredDataCollection(DataCollection<T> primaryCollection, Filter<? super T> filter)
    {
        this(primaryCollection.getOrigin(), primaryCollection.getName(), primaryCollection, filter, null, null);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Initialization
    //
    
    protected void init(Filter<? super T> filter, FunctionJobControl jobControl)
    {
        //v_cache = null; // don't use a cache for filtered data collection

    	this.filter = filter;
        if( this.filter == null )
            this.filter = Filter.INCLUDE_ALL_FILTER;

    	initNames(jobControl);
        primaryCollection.addDataCollectionListener(this);
    }
    
    /**
     * @pending high provide more optimal filteredNames initilization for:
     * 1) filter is disabled;
     */
    protected void initNames(FunctionJobControl jobControl)
    {
        if( filter==Filter.INCLUDE_NONE_FILTER )
            return;
        if( filter==Filter.INCLUDE_ALL_FILTER )
        {
            filteredNames = new ArrayList<>();
            List<String> names = primaryCollection.getNameList();
            if( names==null )
                return;
            Iterator<String> iter = names.iterator();
            while( iter.hasNext() )
            {
                filteredNames.add( (String)iter.next() );
            }
            return;
        }

        int count = 0;
        int curr = 0;
        if( jobControl != null )
        {
            if( isTerminated( jobControl ) )
                return;
            jobControl.functionStarted();
            jobControl.setPreparedness(0);
        }
        
        String prevName = "";
        String name;
        sorted = true;
        try
        {
            if( QueryFilter.class.isAssignableFrom(filter.getClass()) )
            {
                filteredNames = new ArrayList<>();
                List<String> list = ( (QueryFilter<?>)filter ).doQuery( primaryCollection );
                
                if( jobControl!=null )
                {
                    if( isTerminated( jobControl ) )
                        return;
                    curr = list.size();
                    count = curr*2;
                    jobControl.setPreparedness( (int)(((float)curr/(float)count)*100.0) );
                }
 
                if( list != null )
                {
                    Iterator<String> it = list.iterator();
                    while( it.hasNext() )
                    {
                         if( jobControl!=null )
                         {
                             if( isTerminated(jobControl) )
                                 return;

                             curr++;
                             jobControl.setPreparedness( (int)(((float)curr/(float)count)*100.0) );
                         }
                         name = it.next();
                        
                        if( sorted && name.compareTo(prevName) < 0 )
                            sorted = false;
                        
                        filteredNames.add(name);
                        prevName = name;
                    }

                    if( jobControl != null )
                        jobControl.functionFinished();

                    return;
                }
            }
        }
        catch( Throwable t )
        {
        	(new InternalException(t)).log(log);
        }

        final int chunkSize = 1000;
        final List<Integer> indexes = new ArrayList<>();
        count = primaryCollection.getSize();
        int passed = 0;
        prevName = "";
        sorted = true;

        for( String deName : primaryCollection.getNameList() )
        {
            T de;
            try
            {
                de = primaryCollection.get( deName );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            curr++;
            if( jobControl != null )
            {
                if( isTerminated(jobControl) )
                    return;
                jobControl.setPreparedness( (int) ( ( (float)curr / (float)count ) * 100.0 ) );
            }

            if( filter.isAcceptable( de ) )
            {
                name = de.getName();
                if( name.compareTo(prevName) < 0 )
                    sorted = false;
                prevName = name;
                passed++;
                if( passed % chunkSize == 0 )
                {
                    indexes.add( curr );
                }
            }
        }

        filteredNames = new ChunkedList<String>( passed, chunkSize, sorted )
        {
            List<String> primaryNames = primaryCollection.getNameList();

            @Override
            protected String[] getChunk(int from, int to)
            {
                String[] result = new String[to - from];
                int pos = from == 0 ? 0 : indexes.get( from / chunkSize - 1 );
                try
                {
                    for( int i = 0; i < result.length; )
                    {
                        T de = primaryCollection.get( primaryNames.get( pos++ ) );
                        if( filter.isAcceptable( de ) )
                        {
                            result[i++] = de.getName();
                        }
                    }
                }
                catch( Exception e )
                {
                }
                return result;
            }
        };
        
        if( jobControl!=null )
        {
            jobControl.functionFinished();
        }
    }

    private boolean isTerminated(JobControl jobControl)
    {
        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST || jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR )
        {
            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //
    
    protected List<String> filteredNames;
    protected List<String> getFilteredNames()
    {
        return filteredNames;
    }

    public Filter<? super T> getFilter()
    {
        return filter;
    }

    @Override
    public int getSize()
    {
        return getFilteredNames().size();
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        if(!contains(name)) return null;
        return super.getDescriptor(name);
    }

    ////////////////////////////////////////////////////////////////////////////
    // DataCollection methods implementation
    //

   
    @Override
    public boolean contains(String name)
    {
        if( sorted )
        {
            return ( Collections.binarySearch(getFilteredNames(), name) >= 0 );
        }
        return getFilteredNames().contains(name);
    }

    /**
     * Just a stub. Do nothing for filtered data collection.
     */
    @Override
    protected void cachePut(DataElement de)
    {}

    @Override
    public List<String> getNameList()
    {
        return Collections.unmodifiableList(getFilteredNames());
    }

    @Override
    public T get(String name) throws Exception
    {
        T de = doGetPrimaryCollection().get(name);
        if(de == null || !filter.isAcceptable(de)) return null;
        return de;
    }

    @Override
    public T put(T de) throws DataElementPutException
    {
        T added = null;
        if( filter.isAcceptable(de) )
        {
            added = super.put(de);
            if( !super.contains(de.getName()) )
            {
                added = null;
            }
        }
        return added;
    }

    @Override
    public void remove(String name) throws Exception
    {
        if( name == null )
            return;
        if( contains(name) )
        {
            super.remove(name);
            if( !super.contains(name) )
                getFilteredNames().remove(name);
        }
    }

    @Override
    public void close() throws Exception
    {
        primaryCollection.removeDataCollectionListener(this);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Listeners
    //

    private String removedElementName = null;

    @Override
    public void elementWillRemove(DataCollectionEvent event) throws DataCollectionVetoException, Exception
    {
        String name = event.getDataElementName();
        if( contains(name) )
        {
            removedElementName = name;
            fireElementWillRemove(event.getSource(), name);
        }
    }

    @Override
    public void elementRemoved(DataCollectionEvent event) throws Exception
    {
        String name = event.getDataElementName();
        if( name.equals(removedElementName) )
        {
            removedElementName = null;
            initNames(null);
            fireElementRemoved(event.getSource(), name, null);
        }
    }
    @Override
    public void elementWillAdd(DataCollectionEvent event) throws DataCollectionVetoException, Exception
    {
        String name = event.getDataElementName();
        //        if( contains( name ) )
        //        {
        fireElementWillAdd(event.getSource(), name);
        //        }
    }
    @Override
    public void elementAdded(DataCollectionEvent event) throws Exception
    {
        String name = event.getDataElementName();
        if( filter.isAcceptable((T)event.getOwner().get(name)) )
        {
            initNames(null);
            fireElementAdded(event.getSource(), name);
        }
    }
    /**
     * @todo URGENT Use filter
     * @pending firing events removed because of endless loop, should be checked
     */
    @Override
    public void elementWillChange(DataCollectionEvent event) throws DataCollectionVetoException, Exception
    {
        if( contains(event.getDataElementName()) )
        {
            fireElementWillChange(this, event.getOwner(), event.getDataElementName(), null);
        }
    }
    /**
     * @todo URGENT Use filter
     * @pending firing events removed because of endless loop, should be checked
     */
    @Override
    public void elementChanged(DataCollectionEvent event) throws Exception
    {
        String name = event.getDataElementName();
        initNames(null);
        if( contains(name) )
        {
            if( filter.isAcceptable((T)event.getOwner().get(name)) )
            {
                fireElementChanged(this, this, name, null, null);
            }
            else
            {
                fireElementWillRemove(this, name);
                fireElementRemoved(this, name, null);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // SortableDataCollection methods implementation
    //

    @Override
    public boolean isSortingSupported()
    {
        return (primaryCollection instanceof SortableDataCollection) && ((SortableDataCollection<T>)primaryCollection).isSortingSupported();
    }

    @Override
    public String[] getSortableFields()
    {
        if(!isSortingSupported()) return null;
        return ((SortableDataCollection<T>)primaryCollection).getSortableFields();
    }

    @Override
    public List<String> getSortedNameList(String field, boolean direction)
    {
    	List<String> filteredNameList = getNameList(); 
        
    	if( !isSortingSupported() ) 
        	return filteredNameList;
        
        // TODO: lazy implementation
        List<String> sortedList = ((SortableDataCollection<T>)primaryCollection).getSortedNameList(field, direction);
        Set<String> nameList = new HashSet<>(filteredNameList);
        
        List<String> sortedFilteredList = new ArrayList(filteredNameList.size());
        for(String s: sortedList)
        {
        	if( nameList.contains(s) )
        		sortedFilteredList.add(s);
        }

        return sortedFilteredList;
    }

    @Override
    public Iterator<T> getSortedIterator(String field, boolean direction, int from, int to)
    {
        List<String> sortedNameList = getSortedNameList(field, direction);
        return AbstractDataCollection.createDataCollectionIterator(this, sortedNameList.subList(from, to).iterator());
    }
}