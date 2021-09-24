package ru.biosoft.access.core;

import static ru.biosoft.access.core.DataCollectionConfigConstants.CACHING_STRATEGY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.CHILDREN_NODE_IMAGE;
import static ru.biosoft.access.core.DataCollectionConfigConstants.CONFIG_PATH_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.DATA_COLLECTION_LISTENER;
import static ru.biosoft.access.core.DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.FILE_PATH_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.IS_ROOT;
import static ru.biosoft.access.core.DataCollectionConfigConstants.MUTABLE;
import static ru.biosoft.access.core.DataCollectionConfigConstants.NAME_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.NODE_IMAGE;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.swing.event.EventListenerList;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertiesDPS;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.util.HashMapSoftValues;
import ru.biosoft.util.HashMapWeakValues;
import ru.biosoft.util.IconUtils;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.ReadAheadIterator;

/**
 * This abstract class provides default implementations for most of
 * the methods in the <code>DataCollection</code> interface. It takes care of
 * the management of listeners and provides some conveniences for generating
 * DataCollectionEvents and dispatching them to the listeners.
 *
 * <p>To create a concrete DataCollection as a subclass of
 * AbstractDataCollection you need only provide implementations for the
 * following methods:<br>
 * {@link #getNameList()}<br>
 * {@link #doGet(String)}<br>
 * if concrete DataColection mutable, then next methods should be implemented<br>
 * {@link #doPut(DataElement,boolean)}<br>
 * {@link #doRemove(String)}<br>
 */
abstract public class AbstractDataCollection<T extends DataElement> extends DataElementSupport implements DataCollection<T>
{
    private static class LazyDescriptor<T extends DataElement> extends LazyValue<DataElementDescriptor>
    {
        private final AbstractDataCollection<T> collection;

        private LazyDescriptor(AbstractDataCollection<T> collection)
        {
            super( "descriptor" );
            this.collection = collection;
        }

        @Override
        protected DataElementDescriptor doGet() throws Exception
        {
            Class<? extends DataElement> type = collection.getDataElementType();
            boolean leaf = !DataCollection.class.isAssignableFrom( type ) || collection.getInfo().isChildrenLeaf();
            return new DataElementDescriptor( type, collection.getInfo().getProperty(CHILDREN_NODE_IMAGE), leaf );
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    /**
     * Constructor to be used by {@link CollectionFactory} to create DataCollection.
     *
     * Makes info for this data collection.
     * <ul>Required properties
     * <li>{@link DataCollectionConfigConstants#NAME_PROPERTY}</li>
     * </ul>
     * <ul>Optional properties
     * <li>{@link DataCollectionConfigConstants#CONFIG_PATH_PROPERTY}</li>
     * <li>{@link DataCollectionConfigConstants#FILE_PATH_PROPERTY}</li>
     * <li>{@link DataCollectionConfigConstants#NODE_IMAGE}</li>
     * <li>{@link DataCollectionConfigConstants#CHILDREN_NODE_IMAGE}</li>
     * </ul>
     *
     * @param parent Parent data collection.
     * @param properties Properties for creating data collection (may be changed).
     *
     * TODO low makeInfo(Properties) should be moved to collection that only appears in repository.
     */
    public AbstractDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(properties.getProperty(NAME_PROPERTY, "null"), parent);
        
        initLog();
        
        preInit(properties);
        init( properties );
    }
    
    /**
     * Constructs data collection with specified name and parent.
     * This constructor can be used by subclasses if DataCollection is created without
     * using {@link CollectionFactory}.
     *
     * @param name name of this data collection.
     * @param parent Parent for this data collection.
     * @param properties Properties to initialize {@link DataCollectionInfo}. Can be null.
     */
    protected AbstractDataCollection(String name, DataCollection<?> parent, Properties properties)
    {
        super(name, parent);
        initLog();
        
        if( properties == null )
        {
            properties = new Properties();
            properties.setProperty(NAME_PROPERTY, name);
        }

        preInit(properties);
        init( properties );
    }

    /**
     * Here subclasses can make changes in properties to influence on init. 
     * @param properties
     */
    protected void preInit(Properties properties)
    {}

    public static final String MANUALLY_INIT_DC_LISTENER = "manually-init-dc-listener";

    private void init(Properties properties)
    {
        //pending CONFIG_PATH_PROPERTY?
        path = properties.getProperty(CONFIG_PATH_PROPERTY, ".");

        // Replace $path$ in all String properties on real path
        if( path.endsWith(File.separator) )
            path = path.substring(0, path.length() - 1);
        replaceToken(properties, CONFIG_PATH_PROPERTY, path);

        String filePath = properties.getProperty(FILE_PATH_PROPERTY, ".");
        if( filePath.endsWith(File.separator) )
            filePath = filePath.substring(0, filePath.length() - 1);
        replaceToken(properties, FILE_PATH_PROPERTY, filePath);
        
        String mutableStr = properties.getProperty(MUTABLE);
        if( mutableStr != null )
            mutable = Boolean.parseBoolean(mutableStr);

        // Make data collection info
        makeInfo(properties);

        // TODO
        // Add history listener
        //HistoryFacade.addHistoryListener(this);

        boolean manuallyInitDCListener = Boolean.parseBoolean( properties.getProperty( MANUALLY_INIT_DC_LISTENER, "false" ) );
        if( !manuallyInitDCListener && properties.get( DATA_COLLECTION_LISTENER ) != null )
        {
            try
            {
                Class<? extends DataCollectionListener> clazz = Environment.getListenerClassFromRegistry(properties.get(DATA_COLLECTION_LISTENER).toString());
                if( clazz == null )
                    throw new SecurityException("Listener class is not found in the registry: " + properties.get(DATA_COLLECTION_LISTENER));
                
                addDataCollectionListener(clazz.getConstructor(QuerySystem.class).newInstance(getInfo().getQuerySystem()));
            }
            catch( Throwable e )
            {
                new DataElementReadException(e, this, DATA_COLLECTION_LISTENER).log();
            }
        }

        registerRoot();
        initCache( properties );
    }


    /**
     * Register this DataCollection as root in {@link CollectionFactory#registerRoot}
     * if {@link DataCollectionConfigConstants#IS_ROOT} property is true;
     */
    protected void registerRoot()
    {
        String rootProperty = getInfo().getProperties().getProperty(IS_ROOT);
        if( "true".equalsIgnoreCase(rootProperty) )
            CollectionFactory.registerRoot(this);
    }

    /**
     * Replace $token$ with specified value in all String properties.
     * 
     * @param props All properties which will be parsed and replaced.
     * @param token token (without bound $) which should be replaced.
     * @param value value for replacing token.
     */
    static public void replaceToken(Properties props, String token, String value)
    {
        if( props==null )
            return;

        final String template = "$"+token+"$";
        for(Entry<Object, Object> entry : props.entrySet())
        {
            Object o = entry.getValue();
            if( !(o instanceof String) ) continue;
            String prop = (String)o;
            int pos = prop.indexOf(template);
            if( pos>0 )
            {
                prop = prop.substring(0,pos) +
                       value +
                       prop.substring(pos+template.length());
                entry.setValue( prop );
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Info methods
    //

    protected boolean mutable = true;

    /**
     * Returns false.
     * Override this method for return correct value.
     * @return Always return false.
     */
    @Override
    public boolean isMutable()
    {
        return isValid() && mutable;
    }

    /**
     * Gets the type of DataElements stored in the data collection.
     *
     * The DataElement type should be specified using {&link DATA_ELEMENT_CLASS_PROPERTY}
     * or should be specified in derived collections.
     *
     * @return Type of DataElements stored in the data collection.
     */
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        try
        {
            if(info.getProperty(DATA_ELEMENT_CLASS_PROPERTY) != null)
                return info.getPropertyClass(DATA_ELEMENT_CLASS_PROPERTY, DataElement.class);
        }
        catch( DataElementReadException e )
        {
            e.log();
        }
        return DataElement.class;
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return isValid() && isMutable() && clazz.isAssignableFrom(getDataElementType());
    }

    /**
     * Gets data collection info.
     * @return Data collection info.
     * @see DataCollectionInfo
     */
    @Override
    public DataCollectionInfo getInfo()
    {
        return info;
    }

    /**
     * Gets size of data collection.
     * This method must be overridden for performance improving.
     * @return Number of data element in this data collection.
     */
    @Override
    public int getSize()
    {
        if( !isValid() )
            return 0;
        return getNameList().size();
    }

    @Override
    public boolean isEmpty()
    {
        return getSize() == 0;
    }

    /** Utility method that returns data collection description. */
    public String getDescription()
    {
        return getInfo().getDescription();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Data element access methods
    //

    /**
     * Check if this data collection contains the specified data element.
     * @param element tested data element.
     * @return <b>true</b> if this data collection contains the data element which name equals to
     * specified data element, otherwise return <b>false</b>.
     * @see #contains(String)
     * @todo Check type of data element.
     */
    @Override
    public boolean contains(DataElement element)
    {
        return contains(element.getName());
    }

    /**
     *
     * Returns <tt>true</tt> if this data collection contains the element with specified name.
     *
     * @param name name of element whose presence in this data collection is to be tested.
     * @return <tt>true</tt> if this data collection contains the element with specified name.
     * @see #contains(ru.biosoft.access.core.DataElement)
     */
    @Override
    public boolean contains(String name)
    {
        if( !isValid() )
            return false;
        boolean retFlag = false;
        try
        {
            retFlag = ( get(name) != null );
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, getCompletePath() + ".get(" + name + ") throws exception.", exc);
        }
        return retFlag;
    }

    /**
     * Gets data element with specified name.
     * This implementation supports cache and for actual access use {@link #doGet(String)}.
     * @param name Name of the data element (cannot be <b>null</b>).
     * @return DataElement or <B>null</B> if data element not found in the data collection.
     * @throws java.lang.Exception If error raised in {@link #doGet(String)}
     * @see #doGet(String)
     * @see #v_cache
     * @todo Check type of data element.
     */
    @Override
    public T get(String name) throws Exception
    {
        if( !isValid() )
            return null;
        T de = null;
        if( v_cache != null )
            de = v_cache.get(name);
        if( de == null )
        {
            try
            {
                de = doGet(name);
            }
            catch(DataElementGetException e)
            {
                throw e;
            }
            catch(Throwable e)
            {
                throw new DataElementGetException(e, getCompletePath().getChildPath(name));
            }
            if( de != null )
            {
                // basic validation
                if(!Objects.equals( de.getName(), name ))
                    throw new DataElementGetException(
                            new InternalException( "Name of created object is invalid: " + de.getName() + "', should be: '" + name + "'" ),
                            getCompletePath().getChildPath( name ) );
            }
            if( de != null && v_cache != null )
                cachePut(de);
        }
        return de;
    }

    protected LazyValue<DataElementDescriptor> dataElementDescriptor = new LazyDescriptor<>(this);
    
    /**
     * Default implementation returns descriptor with the following properties:
     * Element type is getDataElementType() of current collection
     * Element is not leaf if getDataElementType() is instance of DataCollection and not getInfo().isChildrenLead()
     * No icon or other properties are attached
     */
    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        if(!isValid())
            return null;
        
        return dataElementDescriptor.get();
    }

    /**
     * Puts to cache data element.
     *
     *
     * @param de Stored data element
     * @see #put(DataElement)
     */
    protected void cachePut(T de)
    {
        if( v_cache == null )
            return;

        v_cache.put(de.getName(), de);
    }

    protected void validateName(String dataElementName)
    {
        if( dataElementName == null || dataElementName.equals("") )
            throw new IllegalArgumentException("Cannot store DataElement with empty name");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Data element modification methods
    //
    /**
     * Adds the specified data element to the collection.
     * Notifies all listeners if the data element was added or changed.
     * <code>{@link #doPut(DataElement,boolean)}</code> method is used to put the data element.
     * If the data collection previously contained the specified element,
     * the old value is replaced.<br>
     * Note that this implementation slow enough!!!
     * @param element Data element that will be put in the data collection (Cannot be <b>null</b>).
     * @return previous version of the data element, or null if there was no one.
     * @throws java.util.UnsupportedOperationException if the data collection is unmutable.
     * @throws java.lang.Exception If error occurred.
     * @see #doPut(DataElement,boolean)
     * @see #isMutable()
     * @todo should check DataElement type
     */
    @Override
    public T put(T element) throws DataElementPutException
    {
        if( !isValid() )
            return null;
        if( element == null )
            return null;

        T prev = null;
        String dataElementName = element.getName();
        validateName(dataElementName);
        if( checkMutable() )
        {
            try
            {
                if( contains(dataElementName) )
                {
                    try
                    {
                        prev = get(dataElementName);
                    }
                    catch(Throwable t)
                    {
                        // Cannot retrieve old element: ok, let's log it and try to continue anyways
                        ExceptionRegistry.log(t);
                    }
                }
                boolean isNew = prev == null;
                doAddPreNotify(dataElementName, isNew);

                DataElement oldElement = null;
                if(!isNew)
                {
                    try
                    {
                        oldElement = doGet(dataElementName);
                    }
                    catch( Throwable t )
                    {
                        // Cannot retrieve old element: ok, let's log it and try to continue anyways
                        new DataElementGetException(t, getCompletePath().getChildPath(dataElementName)).log();
                    }
                }
                try
                {
                    doPut(element, isNew);
                }
                catch( DataElementPutException e )
                {
                    if(e.getProperty("path").equals(getCompletePath().getChildPath(dataElementName)))
                        throw e;
                    throw new DataElementPutException(e, getCompletePath().getChildPath(dataElementName));
                }
                catch( Throwable t )
                {
                    throw new DataElementPutException(t, getCompletePath().getChildPath(dataElementName));
                }
                cachePut(element);

                doAddPostNotify(dataElementName, isNew, oldElement);
            }
            catch( DataElementPutException e )
            {
                throw e;
            }
            catch( DataCollectionVetoException ex )
            {
                if( log.isLoggable(Level.FINE) )
                    log.fine("Veto exception for <" + dataElementName + ">, is caught.");
            }
            catch( Throwable t )
            {
                throw new DataElementPutException(t, getCompletePath().getChildPath(dataElementName));
            }
        }
        return prev;
    }

    /**
     * Sends notification to the listeners before <code>{@link #doPut(DataElement,boolean)}</code> operation.
     * If data collection contains the data element with specified name, <code>{@link DataCollectionListener#elementWillChange(DataCollectionEvent )}</code>
     * of listeners is called, or <code>{@link DataCollectionListener#elementWillAdd(DataCollectionEvent )}</code> otherwise
     *
     *
     * @param dataElementName  name of added/changed DataElement.
     * @param bNew  is it new?
     * @exception Exception If any error
     * @exception DataCollectionVetoException If listener does not want to continue change/put action.
     *
     */
    protected void doAddPreNotify(String dataElementName, boolean bNew) throws Exception, DataCollectionVetoException
    {
        if( !bNew )
            fireElementWillChange(this, this, dataElementName, null);
        else
            fireElementWillAdd(this, dataElementName);
    }

    /**
     * Sends notification to the listeners after <code>{@link #doPut(DataElement,boolean)}</code> operation.
     * If data collection contains the data element with specified name, <code>{@link DataCollectionListener#elementChanged(DataCollectionEvent )}</code>
     * of listeners is called, or <code>{@link DataCollectionListener#elementAdded(DataCollectionEvent )}</code> otherwise
     *
     *
     * @param dataElementName  name of added/changed DataElement.
     * @param bNew  is it new?
     * @exception Exception If any error
     */
    protected void doAddPostNotify(String dataElementName, boolean bNew, DataElement oldElement) throws Exception
    {
        if( !bNew )
            fireElementChanged(this, this, dataElementName, oldElement, null);
        else
            fireElementAdded(this, dataElementName);
    }

    /**
     * Removes the specified data element from the collection, if present.
     * Notifies all listeners if the data element was removed.
     * <code>{@link #doRemove(String)}</code> method is used to remove the data element.
     *
     * @param name
     * @exception UnsupportedOperationException If the data collection is unmutable.
     *
     * @exception Exception If any error.
     * @see #doRemove(String)
     * @see #isMutable()
     */
    @Override
    public void remove(String name) throws Exception, UnsupportedOperationException
    {
        if( name == null || !isValid() )
            return;
        if( checkMutable() )
        {
            if( !contains(name) )
            {
                if( log.isLoggable(Level.FINE) )
                    log.fine("can not remove <" + name + ">, data collection does not contains this element.");
                return;
            }

            try
            {
                doRemovePreNotify(name);
                DataElement oldElement = null;
                try
                {
                    oldElement = doGet(name);
                }
                catch( Exception e )
                {
                    log.log(Level.WARNING, "While removing "+DataElementPath.create(this, name)+": cannot get old element", e);
                }
                doRemove(name);
                if( v_cache != null )
                    v_cache.remove(name);
                doRemovePostNotify(name, oldElement);
            }
            catch( DataCollectionVetoException ex )
            {
                log.info("Veto exception <" + name + ">, is caught.");
            }
        }
    }

    protected void doRemovePreNotify(String name) throws Exception, DataCollectionVetoException
    {
        fireElementWillRemove(this, name);
    }

    protected void doRemovePostNotify(String name, DataElement oldElement) throws Exception, DataCollectionVetoException
    {
        fireElementRemoved(this, name, oldElement);
    }

    ////////////////////////////////////////
    // Listener issues
    //
    /**
     * Adds listener to this data collection.
     * @param listener Listener of this data collection.
     */
    @Override
    public void addDataCollectionListener(DataCollectionListener listener)
    {
        listenerList.add(DataCollectionListener.class, listener);
    }

    /**
     * Removes listener from this data collection.
     * @param listener Listener of this data collection.
     */
    @Override
    public void removeDataCollectionListener(DataCollectionListener listener)
    {
        listenerList.remove(DataCollectionListener.class, listener);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utils
    //

    @Override
    public @Nonnull Iterator<T> iterator()
    {
        if(!isValid())
            return Collections.<T>emptyList().iterator();
            		
        return createDataCollectionIterator(this);
    }

    /**
     * Utility method to help write iterator() for DataCollection if getNameList() and get() methods are already implemented.
     * 
     * @param dc DataCollection to create iterator for
     * @return created Iterator
     */
    public static @Nonnull
    <T extends DataElement> Iterator<T> createDataCollectionIterator(final DataCollection<T> dc)
    {
        return createDataCollectionIterator(dc, dc.getNameList().iterator());
    }
    
    /**
     * Utility method to help write iterator() for DataCollection if getNameList() and get() methods are already implemented.
     * @param dc DataCollection to create iterator for
     * @param nameIterator iterator which returns names
     * @return created Iterator
     */
    public static @Nonnull <T extends DataElement> Iterator<T> createDataCollectionIterator(final DataCollection<T> dc, final Iterator<String> nameIterator)
    {
        return new ReadAheadIterator<T>()
        {
            @Override
            protected T advance()
            {
                while( nameIterator.hasNext() )
                {
                    try
                    {
                        String name = nameIterator.next();
                        T de = dc.get(name);
                        if(de != null)
                            return de;
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                }
                return null;
            }
        };
    }
    
    
    /**
     * try to sort name list by element titles
     * @return true if sorting complete
     */
    protected boolean sortNameList(List<String> list)
    {
        QuerySystem qs = getInfo().getQuerySystem();
        if( qs != null )
        {
            @SuppressWarnings("unchecked")
			final Index<String> titleIndex = qs.getIndex("title");
            if( titleIndex != null )
            {
                try
                {
                    Collections.sort(list, (o1, o2) -> {
                        String title1 = titleIndex.get(o1);
                        String title2 = titleIndex.get(o2);
                        if( title1 != null && title2 != null )
                        {
                            if( title1.length() > 0 && title2.length() > 0 )
                            {
                                char a1 = title1.charAt(0);
                                char a2 = title2.charAt(0);
                                if( Character.isDigit(a1) && !Character.isDigit(a2) )
                                {
                                    return 1;
                                }
                                else if( !Character.isDigit(a1) && Character.isDigit(a2) )
                                {
                                    return -1;
                                }
                            }
                            return title1.compareToIgnoreCase(title2);
                        }
                        return 0;
                    });
                }
                catch( Throwable t )
                {
                }
                return true;
            }
        }
        return false;
    }

    private DataElementPath completeName = null;
    @Override
    public @Nonnull DataElementPath getCompletePath()
    {
        if( completeName == null )
        {
            DataCollection<?> origin = getOrigin();
            completeName = ( origin == null ? DataElementPath.EMPTY_PATH : origin.getCompletePath() ).getChildPath(getName());
        }
        return completeName;
    }

    /**
     * Returns a String representation of the data collection.
     *
     * @return String representation of the data collection.
     */
    @Override
    public String toString()
    {
        return getClass().getName() + ": " + getCompletePath();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected
    //
   
    /** @todo Document */
    protected String path;

    /** Data collection info for this data collection. */
    protected DataCollectionInfo info;
    
    protected Logger log;

    /**
     * Initialize logging.
     * @see java.util.logging.Logger
     */
    protected void initLog()
    {
        log = Logger.getLogger(getClass().getName());
    }

    /**
     * Makes data collection info.
     * @param properties Properties with which collection created.
     */
    protected void makeInfo(Properties properties)
    {
        info = new DataCollectionInfo(this, properties);

        if( properties != null )
        {
            String imageName = properties.getProperty(NODE_IMAGE);
            if( imageName != null )
            {
                if( path != null )
                {
                    info.setNodeImage( Environment.getImageIcon( path, imageName ) );
                }
                else
                {
                    info.setNodeImage( Environment.getImageIcon( imageName ) );
                }
            }
            imageName = properties.getProperty(CHILDREN_NODE_IMAGE);
            if( imageName != null )
                info.setChildrenNodeImage(IconUtils.getImageIcon(path, imageName));
        }
    }
    
    ///////////////////////////////////////////////////////////////////
    // Event notification issues
    //

    protected boolean notificationEnabled = true;

    @Override
    public boolean isNotificationEnabled()
    {
        return notificationEnabled;
    }

    @Override
    public void setNotificationEnabled(boolean isEnabled)
    {
        notificationEnabled = isEnabled;
    }

    /**
     * AbstractDataCollection subclasses must call this method <b>after</b>
     * a data element is added to the collection.
     *
     * @param source the DataCollection that changed, typically "this".
     * @param dataElementName the new data element name.
     *
     * @see DataCollectionEvent
     */
    protected void fireElementAdded(Object source, String dataElementName) throws Exception
    {
        if( !notificationEnabled )
            return;

        Object[] listeners = listenerList.getListenerList();
        DataCollectionEvent e = new DataCollectionEvent(source, DataCollectionEvent.ELEMENT_ADDED, this, dataElementName, null);

        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == DataCollectionListener.class )
            {
                try
                {
                    ( (DataCollectionListener)listeners[i + 1] ).elementAdded(e);
                }
                catch( Throwable t )
                {
                    log.severe("Error during elementAdded notificaton: "+ExceptionRegistry.log(t));
                }
            }
        }
        DataCollection<?> origin = getOrigin();
        if( origin != null && origin.isPropagationEnabled() && !CollectionFactory.isDataElementCreating( getCompletePath().toString() ) )
        {
            origin.propagateElementChanged(this, e);
        }
    }

    /**
     * Call {@link DataCollectionListener#elementWillChange(DataCollectionEvent)} for all listeners.

     * @param source source of event
     * @param owner data collection whose element was changed.
     * @param dataElementName changed data element name.
     * @throws DataCollectionVetoException If listener cancel change of data element.
     * @throws Exception If error occurs.
     */
    protected void fireElementWillChange(Object source, DataCollection<?> owner, String dataElementName, DataCollectionEvent primaryEvent)
            throws DataCollectionVetoException, Exception
    {
        if( !notificationEnabled )
            return;

        Object[] listeners = listenerList.getListenerList();
        DataCollectionEvent e = new DataCollectionEvent(source, DataCollectionEvent.ELEMENT_WILL_CHANGE, owner, dataElementName,
                primaryEvent);
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == DataCollectionListener.class )
            {
                try
                {
                    ( (DataCollectionListener)listeners[i + 1] ).elementWillChange(e);
                }
                catch( DataCollectionVetoException ve )
                {
                    throw ve;
                }
                catch( Throwable t )
                {
                    log.severe("Error during elementWillChange notificaton: "+ExceptionRegistry.log(t));
                }
            }
        }

        DataCollection<?> origin = getOrigin();
        if( origin != null )
        {
            origin.propagateElementWillChange(this, e);
        }
    }

    /**
     * Call {@link DataCollectionListener#elementWillAdd(DataCollectionEvent)} for all listeners.
     * @param source Source of event
     * @param dataElementName Data element which will be added.
     * @throws DataCollectionVetoException If listener cancel adding of data element.
     * @throws Exception If error occurs.
     */
    protected void fireElementWillAdd(Object source, String dataElementName) throws DataCollectionVetoException, Exception
    {
        if( !notificationEnabled )
            return;

        Object[] listeners = listenerList.getListenerList();
        DataCollectionEvent e = new DataCollectionEvent(source, DataCollectionEvent.ELEMENT_WILL_ADD, this, dataElementName, null);
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == DataCollectionListener.class )
            {
                try
                {
                    ( (DataCollectionListener)listeners[i + 1] ).elementWillAdd(e);
                }
                catch( DataCollectionVetoException ve )
                {
                    throw ve;
                }
                catch( Throwable t )
                {
                    log.severe("Error during elementWillAdd notificaton: "+ExceptionRegistry.log(t));
                }
            }
        }
        DataCollection<?> origin = getOrigin();
        if( origin != null )
        {
            origin.propagateElementWillChange(this, e);
        }
    }

    /**
     * Call {@link DataCollectionListener#elementWillRemove(DataCollectionEvent)} for all listeners.
     * @param source Source of event
     * @param dataElementName Name of data element which will be removed.
     * @throws DataCollectionVetoException If listener cancel removing of data element.
     * @throws Exception If error occurs.
     */
    protected void fireElementWillRemove(Object source, String dataElementName) throws DataCollectionVetoException, Exception
    {
        if( !notificationEnabled )
            return;

        Object[] listeners = listenerList.getListenerList();
        DataCollectionEvent e = new DataCollectionEvent(source, DataCollectionEvent.ELEMENT_WILL_REMOVE, this, dataElementName, null);
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == DataCollectionListener.class )
            {
                try
                {
                    ( (DataCollectionListener)listeners[i + 1] ).elementWillRemove(e);
                }
                catch( DataCollectionVetoException ve )
                {
                    throw ve;
                }
                catch( Throwable t )
                {
                    log.severe("Error during elementWillRemove notificaton: "+ExceptionRegistry.log(t));
                }
            }
        }
        DataCollection<?> origin = getOrigin();
        if( origin != null )
        {
            origin.propagateElementWillChange(this, e);
        }
    }

    /**
     * AbstractDataCollection subclasses must call this method <b>after</b>
     * a data element of the collection change.
     *
     * @param source the DataCollection that changed, typically "this".
     * @param dataElementName Name of the changed data element.
     *
     * @see DataCollectionEvent
     */
    protected void fireElementChanged(Object source, DataCollection<?> owner, String dataElementName, DataElement oldElement,
            DataCollectionEvent primaryEvent) throws Exception
    {
        if( !notificationEnabled )
            return;

        Object[] listeners = listenerList.getListenerList();
        DataCollectionEvent e = new DataCollectionEvent(source, DataCollectionEvent.ELEMENT_CHANGED, owner, dataElementName, oldElement,
                primaryEvent);
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == DataCollectionListener.class )
            {
                try
                {
                    ( (DataCollectionListener)listeners[i + 1] ).elementChanged(e);
                }
                catch( Throwable t )
                {
                    log.severe("Error during elementChanged notificaton: "+ExceptionRegistry.log(t));
                }
            }
        }
        DataCollection<?> origin = getOrigin();
        if( origin != null  && origin.isPropagationEnabled() && !CollectionFactory.isDataElementCreating( getCompletePath().toString() ))
        {
            origin.propagateElementChanged(this, e);
        }
    }

    /**
     * AbstractDataCollection subclasses must call this method <b>after</b>
     * a data element is removed from the collection.
     *
     * @param source the DataCollection that changed, typically "this".
     * @param dataElementName Name of the removed data element.
     *
     * @see DataCollectionEvent
     */
    protected void fireElementRemoved(Object source, String dataElementName, DataElement oldElement) throws Exception
    {
        if( !notificationEnabled )
            return;

        Object[] listeners = listenerList.getListenerList();
        DataCollectionEvent e = new DataCollectionEvent(source, DataCollectionEvent.ELEMENT_REMOVED, this, dataElementName, oldElement,
                null);
        for( int i = listeners.length - 2; i >= 0; i -= 2 )
        {
            if( listeners[i] == DataCollectionListener.class )
            {
                try
                {
                    ( (DataCollectionListener)listeners[i + 1] ).elementRemoved(e);
                }
                catch( Throwable t )
                {
                    log.severe("Error during elementRemoved notificaton: "+ExceptionRegistry.log(t));
                }
            }
        }
        DataCollection<?> origin = getOrigin();
        if( origin != null  && origin.isPropagationEnabled() && !CollectionFactory.isDataElementCreating( getCompletePath().toString() ))
        {
            origin.propagateElementChanged(this, e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Propagation issues
    //

    protected boolean propagationEnabled = true;

    @Override
    public boolean isPropagationEnabled()
    {
        return propagationEnabled;
    }

    @Override
    public void setPropagationEnabled(boolean propagationEnabled)
    {
        this.propagationEnabled = propagationEnabled;
    }

    /**
     * @todo comment
     */
    @Override
    public void propagateElementWillChange(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        try
        {
            if( source.getOrigin().isPropagationEnabled() )
                fireElementWillChange(this, source.getOrigin(), source.getName(), primaryEvent);
        }
        catch( Exception e )
        {
            log.severe("Error during firing element will changed: "+ExceptionRegistry.log(e));
        }
    }

    /**
     * @todo comment
     */
    @Override
    public void propagateElementChanged(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        try
        {
            if( source.getOrigin().isPropagationEnabled() )
                fireElementChanged(this, source.getOrigin(), source.getName(), null, primaryEvent);
        }
        catch( Exception e )
        {
            log.severe("Error during firing element changed: "+ExceptionRegistry.log(e));
        }
    }

    /**
     * Check whether the data collection is mutable.
     * @throws UnsupportedOperationException if collection is immutable.
     */
    protected boolean checkMutable()
    {
        if( !isMutable() )
        {
            log.severe("Collection " + getCompletePath() + " is immutable.");
            //throw new UnsupportedOperationException("Collection "+getCompletePath()+" is read only.");
        }
        return isMutable();
    }

    /**
     * This method should be implemented in subclasses
     * to get the specified data element from the collection.
     * @param name Name of needed data element.
     * @return Data element or <b>null</b> if data element with specified name not found.
     * @throws Exception If element cannot be fetched
     * @see #get(String)
     * @see #iterator()
     */
    protected T doGet(String name) throws Exception
    {
        throw new UnsupportedOperationException("Method must be implemented in subclass");
    }

    /**
     * This method should be implemented in mutable subclasses
     * to put the specified data element into the collection.
     *
     * @throws UnsupportedOperationException Always throws.
     * @see #put(DataElement)
     */
    protected void doPut(T dataElement, boolean isNew) throws Exception
    {
        throw new UnsupportedOperationException("AbstractDataCollection.doPut() :" + " You must overide this method in derived classes.");
    }

    /**
     * This method should be implemented in mutable subclasses
     * to remove the specified data element from the collection.
     *
     * @throws UnsupportedOperationException Always .
     * @see #remove(String)
     */
    protected void doRemove(String name) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Cache issues
    //

    /** Cache for already accessed data elements.*/
    protected Map<String, T> v_cache;

    private void initCache(Properties properties)
    {
        String cachingStrategy = properties.getProperty( CACHING_STRATEGY, "weak" );

        if(cachingStrategy.equals( "weak" ))
            v_cache = new HashMapWeakValues();
        else if(cachingStrategy.equals( "soft" ))
            v_cache = new HashMapSoftValues();
        else if(cachingStrategy.equals( "hard" ))
            v_cache = new ConcurrentHashMap<>();
        else if(cachingStrategy.equals( "none" ))
            v_cache = null;
        else
        {
            log.warning( "Unknown caching-strategy '" + cachingStrategy + "' for '" + DataElementPath.create( this ) + "'" );
            v_cache = new HashMapWeakValues();
        }
    }
    
    @Override
    public T getFromCache(String dataElementName)
    {
        if( v_cache != null )
            return v_cache.get(dataElementName);
        return null;
    }
    
    public Stream<T> cachedElements()
    {
        if(v_cache == null || v_cache.size() == 0 )
            return Stream.empty();
        
        return v_cache.values().stream();
    }

    public DataElement getNonCached(String dataElementName) throws Exception
    {
        return doGet(dataElementName);
    }

    public void removeFromCache(String dataElementName)
    {
        if( v_cache != null )
            v_cache.remove(dataElementName);
    }

    /**
     * Release the DataElement specified by its name from the cache.
     *
     * @pending whether this operation should be called by close operation.
     */
    @Override
    public void release(String name)
    {
        if( v_cache != null )
            v_cache.remove(name);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Closing issues
    //

    /**
     * Close the data collection and release all used resources.
     *
     * Default implementation provides following actions:
     * <ul>
     *  <li>closing {@link DataCollectionInfo} that provides closing of {@link QuerySystem}</li>
     * </ul>
     *
     *  @pending close all child DataCollections (registered in cache)
     *  @pending clear the cache
     *  @pending release reference to this data collection in parent cache
     */
    @Override
    public void close() throws Exception
    {
        if( info.isQuerySystemInitialized() )
        {
            QuerySystem querySystem = info.getQuerySystem();
            if( querySystem != null )
                querySystem.close();
        }

        if( v_cache != null )
        {
            try
            {
                for(T de : v_cache.values())
                {
                    if( de instanceof DataCollection )
                        ( (DataCollection<?>)de ).close();
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Data collection " + getName() + ", error during closing: " + t, t);
            }
        }
    }
   


    public DynamicPropertySet getDynamicProperties()
    {
        return new PropertiesDPS(getInfo().getProperties(), true);
    }

    protected boolean valid = true;
    @Override
    public boolean isValid()
    {
        return valid;
    }

    @Override
    public void reinitialize() throws LoggedException
    {
        if(isValid())
            return;
        valid = true;
        getNameList();
        if(isValid())
        {
            DataCollection<?> origin = getOrigin();
            if( origin != null  && origin.isPropagationEnabled() && !CollectionFactory.isDataElementCreating( getCompletePath().toString() ))
            {
                origin.propagateElementChanged(this, null);
            }
        }
    }

    @Override
    public DataCollection<?> clone(DataCollection<?> origin, String name) throws CloneNotSupportedException 
    {
        @SuppressWarnings("unchecked")
        AbstractDataCollection<T> clone = (AbstractDataCollection<T>) super.clone(origin, name);
        clone.completeName = null;
        clone.dataElementDescriptor = new LazyDescriptor<>(clone);
    
        return clone;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private
    
    /** List of listeners. */
    private final EventListenerList listenerList = new EventListenerList();
    
}