package ru.biosoft.access.core;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * DataCollection is a set of homogeneous DataElements.
 * Every {@link DataElement} can be unambiguously accessed from the collection
 * by its name.<br>
 *
 * Method names in DataCollection whether it reasonable follows
 * to convention for Java beans methods, otherwise to convention
 * used by java.utils.Collection.
 *
 * @see DataElement
 */
@PropertyName("collection")
public interface DataCollection<T extends DataElement> extends DataElement, Iterable<T>
{
    ////////////////////////////////////////
    // Info methods
    //

    /**
     * Returns the number of elements in this data collection.
     * @return Size of this data collection.
     */
    int getSize();
    
    /**
     * @return true if collection is empty
     */
    boolean isEmpty();

    /**
     * Returns the type of DataElements stored in the data collection.
     * @return Type of DataElements stored in the data collection.
     */
    @Nonnull Class<? extends DataElement> getDataElementType();
    
    /**
     * Returns true if element of given class can be stored in this collection
     * @param clazz class to check
     * @return true if element of given class can be stored in this collection
     */
    public boolean isAcceptable(Class<? extends DataElement> clazz);

    /**
     * Returns <b>true</b> if this data collection is mutable, <b>false</b> otherwise.
     * @return <b>true</b> if primary data collection is mutable,<br> <b>false</b> otherwise..
     */
    boolean isMutable();

    /**
     * Returns additional info for this data collection.
     * Can return <code>null</code>.
     * @return Additional info for this data collection.
     * @see DataCollectionInfo
     */
    DataCollectionInfo getInfo();

    ////////////////////////////////////////////////////////////////////////////
    // Data element access methods
    //

    /**
     * Returns <b>true</b> if this data collection contains the element with the specified name, <b>false</b> otherwise
     *
     *
     * @param name name of data element
     * @return <b>true</b> if this data collection contains the element with specified name,<br> <b>false</b> otherwise
     */
    boolean contains(String name);

    /**
     * Returns <b>true</b> if this data collection contains the specified element, <b>false</b> otherwise
     *
     * @param element specified data element
     * @return <b>true</b> if this data collection contains the element wit specified name,<br> <b>false</b> otherwise
     */
    boolean contains(DataElement element);

    /**
     * Returns the <code>DataElement</code> with the specified name.
     * Returns <code>null</code> if the data collection
     * contains no data element for this name.
     */
    T get(String name) throws Exception;

    /**
     * Returns the <code>DataElementDescriptor</code> for the DataElement with the specified name
     * Return value is not specified if element doesn't exists (check contains(name) by yourself!)
     */
    DataElementDescriptor getDescriptor(String name);

    /**
     * Returns an iterator over the data elements in this collection.
     * There are no guarantees concerning the order in which the elements
     * are returned. If the data collection is modified while an iteration
     * over it is in progress, the results of the iteration are undefined.
     */
    @Override
    @Nonnull Iterator<T> iterator();

    /**
     * Returns an unmodifiable view of the data element name list.
     * Query operations on the returned list "read through" to the internal name list,
     * and attempts to modify the returned list, whether direct or via its iterator,
     * result in an <code>UnsupportedOperationException</code>.
     *
     * The returned list is backed by the data collection,
     * so changes to the data collection are reflected in the returned list.
     *
     * The name list can be sorted or unsorted depending on the DataCollection
     * implementing class.
     *
     * @return  list of names
     */
    @Nonnull List<String> getNameList();

    ////////////////////////////////////////
    // Data element modification methods
    //

    /**
     * Adds the specified data element to the collection.
     * If the data collection previously contained the specified element,
     * the old value is replaced.
     *
     * @return previous version of the data element, or null if there was no one.
     * @throws RepositoryAccessDeniedException if access is denied
     * @throws DataElementPutException if put failed for some other reason
     * @see #isMutable
     */
    T put(T obj) throws DataElementPutException, RepositoryAccessDeniedException;

    /**
     * Removes the specified data element from the collection, if present.
     * Does nothing if null is supplied
     * Notifies all listeners if the data element was removed.
     *
     * @throws java.util.UnsupportedOperationException if the data collection is unmutable.
     * @throws java.lang.Exception If error occurred.
     * @see #isMutable
     */
    void remove(String name) throws Exception;
    
    /**
     * @return true if DataCollection is in valid state; false otherwise
     */
    boolean isValid();
    
    /**
     * try to reinitialize an invalid collection
     * @throws LoggedException if reinitialization fails; in this case collection remains invalid
     */
    void reinitialize() throws LoggedException;

    ////////////////////////////////////////////////////////////////////////////
    // Listener issues
    //

    /**
     * Add a listener to the list that's notified each time a change
     * to the data collection occurs.
     *
     * @param l the DataCollectionListener
     */
    void addDataCollectionListener(DataCollectionListener l);

    /**
     * Remove a listener from the list that's notified each time a
     * change to the data collection occurs.
     *
     * @param l the DataCollectionListener
     */
    void removeDataCollectionListener(DataCollectionListener l);

    /**
     * @todo comment
     */
    void propagateElementWillChange(DataCollection<?> source, DataCollectionEvent primaryEvent);

    /**
     * @todo comment
     */
    void propagateElementChanged(DataCollection<?> source, DataCollectionEvent primaryEvent);

    boolean isPropagationEnabled();
    void setPropagationEnabled(boolean isEnabled);

    boolean isNotificationEnabled();
    void setNotificationEnabled(boolean isEnabled);

    ////////////////////////////////////////////////////////////////////////////
    // Utils
    //

    /**
     * @return DataElementPath object representing the path to this collection
     */
    @Override
    @Nonnull DataElementPath getCompletePath();

    /**
     * Closes data collection, releases all resources.
     * This method invalidates DataCollection instance.
     * @throws java.lang.Exception If error occurred.
     */
    void close() throws Exception;

    /**
     * Release DataElement with the specified name from the DataCollection cache.
     * @pending whether this operation should be called by close operation.
     */
    void release(String dataElementName);

    /**
     * Return DataElement only if cache exists and this element is in cache.
     */
    DataElement getFromCache(String dataElementName);
    
    ////////////////////////////////////////////////////////////////////////////
    // Streams
    //

    default Stream<String> names()
    {
        return getNameList().stream(); 
    }

    default Stream<T> stream()
    {
    	if( this.isEmpty() )
    		return Stream.empty();
    	
        return names().map( name -> {
            try
            {
                return get( name );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        });
    }


    @SuppressWarnings ( "unchecked" )
    default <TT extends T> Stream<TT> stream(Class<TT> elementClass)
    {
        return (Stream<TT>) ( stream().filter( elementClass::isInstance ) );
    }

}
