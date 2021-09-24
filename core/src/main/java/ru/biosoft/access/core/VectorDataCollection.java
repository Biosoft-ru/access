package ru.biosoft.access.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import static ru.biosoft.access.core.DataCollectionConfigConstants.*;

/**
 *  DataCollection which contains all-in-memory elements stored in alphabetical order
 *  ( <B>DataElement</B>'s is stored in Vector ).
 */
public class VectorDataCollection<T extends DataElement> extends AbstractDataCollection<T>
{
    /** Back-end storage for data elements. */
    protected Map<String, T> elements = new TreeMap<>();
    protected List<String> vectorNameList;

    /**
     * Constructor to be used by {@link CollectionFactory} to create VectorDataCollection.
     *
     * <ul>Required properties.
     * <li>{@link DataCollectionConfigConstants#NAME_PROPERTY}</li>
     * </ul>
     *
     * @param parent     parent DataCollection
     * @param properties set of properties
     */
    public VectorDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent,properties);
        v_cache = null;
    }

    /**
     * Constructs data collection with specified name and parent.
     *
     * @param parent Parent for this data collection.
     * @param name Name of this data collection.
     * @param properties Properties to initialise {@link DataCollectionInfo}. Can be null.
     */
    public VectorDataCollection(String name, DataCollection<?> parent, Properties properties)
    {
        super(name, parent, properties);
        v_cache = null;
    }
    
    public VectorDataCollection(String name)
    {
        super(name, null, null);
        v_cache = null;
    }

    public VectorDataCollection(String name, Class<? extends T> elementClass, DataCollection<?> parent)
    {
        super(parent, createProperties(name, elementClass));
        v_cache = null;
    }

    private static Properties createProperties(String name, Class<?> elementClass)
    {
        Properties properties = new Properties();
        properties.setProperty(NAME_PROPERTY, name);
        properties.setProperty(DATA_ELEMENT_CLASS_PROPERTY, elementClass.getName());
        return properties;
    }

    /**
     * Returns the number of {@link ru.biosoft.access.core.DataElement data elements} in this data collection.
     *
     * @return the number of data elements in this data collection.
     */
    @Override
    public int getSize()
    {
        return elements.size();
    }

    /**
     * Returns an iterator over the elements in this list in alphabetically sorted sequence.
     *
     * @return an iterator over the elements in this list in alphabetically sorted sequence.
     */
    @Override
    public @Nonnull Iterator<T> iterator()
    {
        return elements.values().iterator();
    }

    @Override
    public Stream<T> stream()
    {
        return elements.values().stream();
    }

    /**
     * Returns an array containing all of the elements in this VectorDataCollection in the correct order;
     * the runtime type of the returned array is that of the specified array.
     *
     * @see Vector#toArray(Object[] a)
     */
    public @Nonnull T[] toArray(T[] a)
    {
        return elements.values().toArray(a);
    }
    
    public void putAll(Collection<? extends T> collection) throws Exception
    {
        for(T element: collection)
            put(element);
    }

    /**
     * Returns an unmodifiable list of the data element names contained in this data collection.
     * Query operations on the returned list "read through" to the internal name list,
     * and attempts to modify the returned list, whether direct or via its iterator,
     * result in an <code>UnsupportedOperationException</code>.
     *
     * The returned list is backed by the data collection,
     * so changes to the data collection are reflected in the returned list.
     *
     * @return Names of all elements in this data collection in alphabetically sorted order.
     */
    @Override
    public @Nonnull List<String> getNameList()
    {
        if(vectorNameList == null)
        {
            vectorNameList = new ArrayList<>( elements.keySet() );
        }
        if( getInfo().getQuerySystem() != null )
        {
            List<String> sortedList = new ArrayList<>( vectorNameList );
            sortNameList(sortedList);
            return sortedList;
        }
        return Collections.unmodifiableList(vectorNameList);
    }

    /**
     * Returns the element with the specified name from this data collection.
     *
     * @param name name of element to return.
     * @return the element with the specified name in this data collection.
     * @see #get(String)
     */
    @Override
    protected T doGet(String name)
    {
        return elements.get(name);
    }
    
    @Override
    public boolean contains(String name)
    {
        return elements.containsKey( name );
    }

    @Override
    public T get(String name)
    {
        if( !isValid() )
            return null;
        return doGet(name);
    }
    /**
     * Clear the whole collection
     */
    protected void clear()
    {
        elements.clear();
        vectorNameList = null;
    }

    /**
     * Add the specified element to this data collection.
     *
     * @param dataElement element to be added to this data collection.
     * @param isNew indicates whether this dataElement is new.
     * @throws IllegalArgumentException if dataElement is <tt>null</tt>.
     * @throws Exception If any error occurs.
     * @see AbstractDataCollection#put(DataElement)
     */
    @Override
    protected void doPut(T dataElement, boolean isNew) throws IllegalArgumentException
    {
        if( dataElement == null )
            throw new IllegalArgumentException("dataElement cannot be null.");

        String name = dataElement.getName();
        synchronized(elements)
        {
            if(!elements.containsKey(name)) vectorNameList = null;
            elements.put(name, dataElement);
        }
    }

    /**
     * Removes the specified data element from this data collection.
     *
     * @param name Name of specified data element
     * @throws Exception If any error occurs.
     * @see AbstractDataCollection#remove(String)
     */
    @Override
    protected void doRemove(String name) throws Exception
    {
        synchronized(elements)
        {
            if(elements.containsKey(name))
            {
                elements.remove(name);
                vectorNameList = null;
            }
        }
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        DataElement element = doGet(name);
        if(element instanceof SymbolicLinkDataCollection)
        {
            element = ((SymbolicLinkDataCollection)element).getPrimaryCollection();
        }

        if(element == null) 
        	return null;
        
        if(element instanceof DataCollection)
        {
            Properties elementProperties = ((DataCollection<?>)element).getInfo().getProperties();
            Map<String, String> properties = new HashMap<String, String>();
            for(java.util.Map.Entry<Object, Object> entry: elementProperties.entrySet())
            {
                if(entry.getKey() instanceof String && entry.getValue() instanceof String)
                    properties.put((String)entry.getKey(), (String)entry.getValue());
            }

            return new DataElementDescriptor(element.getClass(), Boolean.valueOf(elementProperties.getProperty(IS_LEAF)), properties);
        }
        return new DataElementDescriptor(element.getClass());
    }
    
    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return true;    // We can put any element into VDC
    }
}