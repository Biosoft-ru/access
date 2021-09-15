package ru.biosoft.access.core;

import static ru.biosoft.access.core.DataCollectionConfigConstants.CONFIG_PATH_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.FILE_PATH_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.NEXT_CONFIG;
import static ru.biosoft.access.core.DataCollectionConfigConstants.PLUGINS_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.PRIMARY_COLLECTION;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ru.biosoft.exception.ExceptionRegistry;

/**
 * General class for derived data collection.
 *
 * <p>Subclasses should provide the difference of derived
 * data collection from primary one.
 *
 * <p> Derived data collection is listener of primary data collection.
 * This implementation provides delegation primary data collection events
 * to listeners of derived data collection.
 * 
 * T1 = this collection element type
 * T2 = primary collection element type
 *
 * @pending high we do not close primary collection. It is subclasses responsibility.
 */
public class DerivedDataCollection<T1 extends DataElement, T2 extends DataElement> extends AbstractDataCollection<T1>
{
    /** Primary collection. */
    protected DataCollection<T2> primaryCollection;

    /**
     * Constructor to be used by {@link CollectionFactory}.
     *
     * Obligatory properties are
     * <ul>
     * <li>{@link #NAME}</li>
     * <li>{@link #PRIMARY_COLLECTION}</li>
     * <li>{@link #FILTER_PROPERTY}</li>
     * </ul>
     *
     * @param parent
     * @param properties Properties for creating data parent .
     */
    public DerivedDataCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);

        Object obj = properties.get(PRIMARY_COLLECTION);
        if( obj instanceof DataCollection )
        {
            primaryCollection = (DataCollection)obj;
        }
        else if( obj instanceof String )
        {
            primaryCollection = CollectionFactory.getDataCollection((String)obj);
        }

        if( primaryCollection == null )
        {
            // loading of format config
            String nextFile = properties.getProperty(NEXT_CONFIG);
            String configPath = properties.getProperty(CONFIG_PATH_PROPERTY);
            if( configPath != null )
                configPath = configPath.trim();
            String filePath = properties.getProperty(FILE_PATH_PROPERTY);
            if( filePath != null )
                filePath = filePath.trim();
            if( filePath == null || filePath.equals("") )
                filePath = configPath;

            Properties nextProperties = null;
            if( nextFile != null )
            {
                nextFile = nextFile.trim();
                //TODO: before it was ExProperties
                //Do we have any non-path configPath variables
                //PluginEntry nextConfig = ApplicationUtils.resolvePluginPath( configPath ).child( nextFile );
                FileInputStream stream = new FileInputStream(configPath + "/" + nextFile);
                nextProperties = new Properties();
                nextProperties.load(stream);
                stream.close();

                if( nextProperties.get( CONFIG_PATH_PROPERTY ) == null )
                    nextProperties.put( CONFIG_PATH_PROPERTY, configPath );

                if( nextProperties.get(FILE_PATH_PROPERTY) == null )
                    nextProperties.put(FILE_PATH_PROPERTY, filePath);

                String plugins = properties.getProperty(PLUGINS_PROPERTY);
                if( plugins != null )
                {
                    if( nextProperties.containsKey(PLUGINS_PROPERTY) )
                    {
                        //TODO: distinct
                        plugins += ";" + nextProperties.getProperty(PLUGINS_PROPERTY);
                    }
                    nextProperties.put(PLUGINS_PROPERTY, plugins);
                }

                primaryCollection = CollectionFactory.createCollection(parent, nextProperties);
                primaryCollection.getInfo().addUsedFile(new File(configPath + "/" + nextFile));
            }
        }
        if( primaryCollection == null )
            throw new DataElementCreateException(getCompletePath());

        init();
    }

    /**
     * Constructs DerivedDataCollection with the parent, name and primary data collection.
     *
     * @param parent parent for this data collection.
     * @param parent name of this data collection.
     * @param primaryDC primary data collection.
     * @param properties Properties to initialise {@link DataCollectionInfo}. Can be null.
     */
    public DerivedDataCollection(DataCollection<?> parent, String name, DataCollection<T2> primaryDC, Properties properties)
    {
        super(name, parent, properties);
        primaryCollection = primaryDC;

        init();
    }

    protected void init()
    {
        // add files used by primary collection
        List<File> files = primaryCollection.getInfo().getUsedFiles();
        if( files != null )
        {
            for(File file: files)
                info.addUsedFile(file);
        }

    }

    ////////////////////////////////////////
    // Info methods
    //

    /**
     * Calls {@link mgl3.access.DataCollection#getInfo()} method of primary data collection.
     * @return Data collection info {@link mgl3.access.DataCollectionInfo data collection info}.
     * @see mgl3.access.DataCollectionInfo
     * @see #collection
     *
     public DataCollectionInfo getInfo()
     {
     return super.getInfo(); //collection.getInfo();
     }*/

    /**
     * Calls {@link DataCollection#getSize()} method of primary data collection.
     * @return Number of data element in primary data collection.
     */
    @Override
    public int getSize()
    {
        return doGetPrimaryCollection().getSize();
    }


    /**
     * Calls {@link DataCollection#getDataElementType()} method of primary data collection.
     * @return Type of data elements stored in the primary data collection.
     */
    @Override
    public Class<? extends DataElement> getDataElementType()
    {
        return doGetPrimaryCollection().getDataElementType();
    }

    /**
     * Calls {@link DataCollection#isMutable()}  method of primary data collection.
     * @return <b>true</b> if primary data collection is mutable,<br> <b>false</b> otherwise..
     */
    @Override
    public boolean isMutable()
    {
        return doGetPrimaryCollection().isMutable();
    }
    ////////////////////////////////////////
    // Data element access methods
    //
    /**
     * Calls {@link DataCollection#contains(String)}  method of primary data collection.
     *
     * @param name specified name of data element
     * @return <b>true</b> if primary data collection contains element with specified name,<br> <b>false</b> otherwise..
     */
    @Override
    public boolean contains(String name)
    {
        return doGetPrimaryCollection().contains(name);
    }

    public DataCollection<T2> getPrimaryCollection()
    {
        return doGetPrimaryCollection();
    }

    protected DataCollection<T2> doGetPrimaryCollection()
    {
        return primaryCollection;
    }

    /**
     * Calls {@link DataCollection#contains(DataElement)}  method of primary data collection.
     *
     * @param de specified data element
     * @return <b>true</b> if primary data collection contains specified element ,<br> <b>false</b> otherwise..
     */
    @Override
    public boolean contains(DataElement de)
    {
        return contains(de.getName());
    }
    
    protected final Object nameLock = new Object();
    protected List<String> sortedNames = null;
    /**
     * Calls {@link DataCollection#getNameList()} method of primary data collection
     *
     * @return primary data collection name list.
     */
    @Override
    public List<String> getNameList()
    {
        List<String> names = primaryCollection.getNameList();
        if( sortedNames == null || sortedNames.size() != names.size() )
        {
            synchronized( nameLock )
            {
                if( sortedNames == null || sortedNames.size() != names.size() )
                {
                    sortedNames = new ArrayList<>( names ); // copy namelist if returned one was internal or unmodified
                    sortNameList( sortedNames );
                }
            }
        }
        return sortedNames;
    }

    @Override
    public void removeFromCache(String dataElementName)
    {
        super.removeFromCache( dataElementName );
        if(primaryCollection instanceof AbstractDataCollection)
            ( (AbstractDataCollection<?>)primaryCollection ).removeFromCache( dataElementName );
    }

    ////////////////////////////////////////
    // Data element modification methods
    //
    /**
     * Calls {@link DataCollection#put(DataElement)} of primary collection
     * Puts the specified data element into the primary collection.
     *
     * @param element
     * @exception Exception If any errors
     */
    @Override
    protected void doPut(T1 element, boolean isNew) throws Exception
    {
        doGetPrimaryCollection().put((T2)element);
        synchronized( nameLock )
        {
            sortedNames = null;
        }
    }

    /**
     * Calls {@link DataCollection#remove(String)} of primary collection.
     * Remove the specified data element from the primary collection.
     *
     * @param element specified data element
     * @exception Exception If any errors
     * @todo  close()  in this method is misplaced. It is not cool solution !!!
     */
    @Override
    protected void doRemove(String name) throws Exception
    {
        if( DataCollection.class.isAssignableFrom(getDataElementType()) )
        {
            DataCollection<?> dc = (DataCollection)get(name);
            if( dc != null )
            {
                String[] nameList = dc.getNameList().toArray(new String[dc.getSize()]);
                for( String iname: nameList )
                {
                    try
                    {
                        dc.remove(iname);
                    }
                    catch( Exception e )
                    {
                        log.severe("Cannot remove "+getCompletePath().getChildPath(iname)+": " + ExceptionRegistry.log(e));
                    }
                }
                dc.close();
            }
        }
        doGetPrimaryCollection().remove(name);
        synchronized( nameLock )
        {
            sortedNames = null;
        }
    }

    /**
     * Calls  {@link DataCollection#get(String)} method of primary collection
     *
     * @param name   Name of specified data element.
     * @return the specified data element from the primary collection.
     * @exception Exception If any error
     */
    @Override
    public T1 doGet(String name) throws Exception
    {
        return (T1)doGetPrimaryCollection().get(name);
    }
    
    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        return doGetPrimaryCollection().getDescriptor(name);
    }

    @Override
    public void close() throws Exception
    {
        if( primaryCollection != getOrigin() )
        {
            primaryCollection.close();
        }
        super.close();
    }

    /**
     * Returns a string representation of the  derived data collection
     * @return string representation of the  derived data collection
     */
    @Override
    public String toString()
    {
        return super.toString() + ",collection = " + doGetPrimaryCollection();
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return doGetPrimaryCollection().isAcceptable(clazz);
    }
}
