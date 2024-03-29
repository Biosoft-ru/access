package ru.biosoft.access.core;

import static ru.biosoft.access.core.DataCollectionConfigConstants.CLASS_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.NAME_PROPERTY;
import static ru.biosoft.access.core.DataCollectionConfigConstants.PLUGINS_PROPERTY;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class CollectionFactory
{
    protected static final Logger log = Logger.getLogger( CollectionFactory.class.getName() );

    ////////////////////////////////////////////////////////////////////////////
    // Root DataCollections
    //

    /**
     * Root DataCollections                   <br>
     * key is name of DataCollection (String) <br>
     * value is DataCollection
     */
    static HashMap<String, DataElement> rootMap = new HashMap<>();

    /**
     * Register DataCollection in root HashMap.
     *
     * @see #getDataCollection
     * @see #unregisterRoot
     */
    static public void registerRoot(DataCollection<?> dc)
    {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission( new RuntimePermission( "modifyRepository" ) );
        }

        if( log.isLoggable( Level.FINE ) )
            log.fine( "Put " + dc.getName() + " datacollection to rootMap" );

        rootMap.put( dc.getName(), dc );
    }

    static public Collection<String> getRootNames()
    {
        return new ArrayList<>( rootMap.keySet() );
    }

    /**
     * Unregister DataCollection in root HashMap.
     *
     * @see #getDataCollection
     * @see #registerRoot
     */
    static public void unregisterRoot(DataCollection<?> dataCollection)
    {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission( new RuntimePermission( "modifyRepository" ) );
        }

        rootMap.remove( dataCollection.getName() );
    }

    /**
     * Unregister all root DataCollection. Is useful for tests.
     */
    static public void unregisterAllRoot()
    {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission( new RuntimePermission( "modifyRepository" ) );
        }

        rootMap.clear();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Get DataElement 
    //

    private static ThreadLocal<Set<String>> currentPaths = new ThreadLocal<Set<String>>()
    {
        @Override
        protected Set<String> initialValue()
        {
            return new HashSet<>();
        }
    };

    static public boolean isDataElementCreating(String completeName)
    {
        return currentPaths.get().contains( completeName );
    }
    
    static public @CheckForNull DataElement getDataElement(String completeName)
    {
        return getDataElement( completeName, false );
    }

    /**
    * Returns the data collection with the specified complete name
    * relative of one of the data collection from data collections
    * registered in root hash map.
    *
    * @param completeName complete name
    * @return named DataCollection or null otherwise
    */
    static public @CheckForNull DataCollection getDataCollection(String completeName)
    {
        DataElement de = getDataElement( completeName );
        if( de instanceof DataCollection )
            return (DataCollection)de;

        return null;
    }

    /**
    * Returns the DataElement with the specified name.
    * 
    * @param completeName - complete DataElement name in CollectionFactory tree <br>
    * example: localhost/matrices/matrixlib.TransformedDataCollection/V$MYOD_01
    *
    * @return the DataElement with the specified name.
    * @see DataElement
    * see #getDataCollection(String)
    * @see DataCollection#getCompletePath()
    */
    static public @CheckForNull DataElement getDataElement(String completeName, boolean followSymLinks)
    {
        try
        {
            return getDataElementChecked( completeName, followSymLinks );
        }
        catch( RepositoryException ex )
        {
            if( ! ( ex instanceof DataElementNotFoundException ) )
                ex.log();

            return null;
        }
    }

    static public @Nonnull DataElement getDataElementChecked(String completeName, boolean followSymLinks) throws RepositoryException
    {
        if( completeName.isEmpty() )
            throw new DataElementNotFoundException( DataElementPath.EMPTY_PATH );

        if( currentPaths.get().contains( completeName ) )
            throw new DataElementCreatingException( DataElementPath.create( completeName ) );

        currentPaths.get().add( completeName );

        try
        {
            DataElement de = null;
            DataCollection<?> dc = null;
            StringTokenizer st = new StringTokenizer( completeName, DataElementPath.PATH_SEPARATOR );
            String name;
            while( st.hasMoreTokens() )
            {
                name = DataElementPath.unescapeName( st.nextToken() );
                if( dc == null )
                {
                    de = rootMap.get( name );
                    if( de == null )
                    {
                        currentPaths.get().remove( completeName );
                        throw new DataElementNotFoundException( DataElementPath.create( name ) );
                    }
                }
                else
                {
                    try
                    {
                        de = dc.get( name );
                    }
                    catch( DataElementGetException e )
                    {
                        throw e;
                    }
                    catch( Throwable e )
                    {
                        throw new DataElementGetException( e, dc.getCompletePath().getChildPath( name ) );
                    }
                    if( de == null )
                    {
                        throw new DataElementNotFoundException( dc.getCompletePath().getChildPath( name ) );
                    }
                }

                if( followSymLinks && de instanceof SymbolicLinkDataCollection )
                {
                    DataCollection<?> targetDC = ( (SymbolicLinkDataCollection)de ).getPrimaryCollection();
                    if( targetDC == null )
                    {
                        throw new SymbolicLinkException( ( (SymbolicLinkDataCollection)de ) );
                    }
                    de = targetDC;
                }

                if( st.hasMoreTokens() )
                {
                    if( de instanceof DataCollection )
                        dc = (DataCollection<?>)de;
                    else
                    {
                        throw new DataElementInvalidTypeException( DataElementPath.create( de ), DataCollection.class );
                    }
                }
            }
            return de;
        }
        finally
        {
            currentPaths.get().remove( completeName );
        }
    }

    static public @Nonnull <T extends DataElement> T getDataElement(String relativeName, DataCollection ancestor, @Nonnull Class<T> clazz)
            throws RepositoryException
    {
        DataElement de = null;
        DataCollection<?> dc = ancestor;
        StringTokenizer st = new StringTokenizer( relativeName, DataElementPath.PATH_SEPARATOR );
        String name;
        while( st.hasMoreTokens() )
        {
            name = DataElementPath.unescapeName( st.nextToken() );
            if( dc == null )
            {
                de = rootMap.get( name );
                if( de == null )
                {
                    throw new DataElementNotFoundException( DataElementPath.create( name ) );
                }
            }
            else
            {
                try
                {
                    de = dc.get( name );
                }
                catch( DataElementGetException e )
                {
                    throw e;
                }
                catch( Throwable e )
                {
                    throw new DataElementGetException( e, dc.getCompletePath().getChildPath( name ) );
                }
                if( de == null )
                {
                    throw new DataElementNotFoundException( dc.getCompletePath().getChildPath( name ) );
                }
            }
            if( st.hasMoreTokens() )
            {
                if( de instanceof DataCollection )
                    dc = (DataCollection<?>)de;
                else
                {
                    throw new DataElementInvalidTypeException( DataElementPath.create( de ), DataCollection.class );
                }
            }
        }
        if( de == null )
        {
            throw new DataElementNotFoundException( ancestor.getCompletePath().getRelativePath( relativeName ) );
        }
        return de.cast( clazz );
    }

    /**
     * Returns the DataElement with the specified name.
     * @param relativeName name of data element relative the ancestor
     * @param ancestor direct or indirect parent of requested data element
     * @return the DataElement with the specified name.
     * @see DataElement
     * @see #getRelativeName
     */
    static public DataElement getDataElement(String relativeName, DataCollection<?> ancestor)
    {
        StringTokenizer st = new StringTokenizer( relativeName, DataElementPath.PATH_SEPARATOR );
        DataElement de = ancestor;
        StringBuilder name;
        while( st.hasMoreTokens() )
        {
            try
            {
                name = new StringBuilder( DataElementPath.unescapeName( st.nextToken() ) );
                de = ancestor.get( name.toString() );

                if( de == null )
                {
                    // Compatibility code
                    // TODO: consider removing
                    // possibly '/' is used as part of DataElement name
                    while( st.hasMoreTokens() )
                    {
                        name.append( DataElementPath.PATH_SEPARATOR ).append( st.nextToken() );
                        de = ancestor.get( name.toString() );
                        if( de != null )
                            break;
                    }

                    if( de == null )
                        return null;
                }

                if( st.hasMoreTokens() )
                {
                    if( de instanceof DataCollection )
                        ancestor = (DataCollection<?>)de;
                    else
                    {
                        // one more try
                        name.append( DataElementPath.PATH_SEPARATOR ).append( st.nextToken() );
                        de = ancestor.get( name.toString() );
                        if( de == null || !st.hasMoreTokens() )
                            return de;

                        if( de instanceof DataCollection )
                            ancestor = (DataCollection<?>)de;
                        else
                            return null;
                    }
                }
            }
            catch( Throwable t )
            {
                throw ExceptionRegistry.translateException( t );
            }
        }

        return de;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Create data collection 
    //

    /**
    * Creates {@link DataCollection} with the specified parent and properties.
    */
    static public @Nonnull DataCollection createCollection(DataCollection<?> parent, Properties properties)
    {
        String className = properties.getProperty( CLASS_PROPERTY );
        DataElementPath childPath = DataElementPath.create( parent, properties.getProperty( NAME_PROPERTY, "" ) );

        try
        {
            String pluginNames = properties.getProperty( PLUGINS_PROPERTY );

            if( pluginNames == null )
                pluginNames = Environment.getPluginForClass( className );

            Class<? extends DataCollection> c = Environment.loadClass( className, pluginNames, DataCollection.class );
            Constructor<? extends DataCollection> constructor = c.getConstructor( DataCollection.class, Properties.class );

            return constructor.newInstance( parent, properties );
        }
        catch( DataElementCreateException e )
        {
            if( e.getProperty( "path" ).equals( childPath ) )
                throw e;

            throw new DataElementCreateException( e, childPath, DataCollection.class );
        }
        catch( Throwable t )
        {
            throw new DataElementCreateException( t, childPath, DataCollection.class );
        }
    }

    ////////////////////////////////////////
    // Utilities
    //

    /**
     * Returns complete name of DataElement relative its ancestor.
     */
    public static String getRelativeName(DataElement child, @Nonnull DataCollection<?> ancestor)
    {
        StringBuilder buffer = new StringBuilder( DataElementPath.escapeName( child.getName() ) );
        DataCollection<?> parent = child.getOrigin();

        while( parent != null && !parent.getCompletePath().equals( ancestor.getCompletePath() ) )
        {
            buffer.insert( 0, "/" );
            buffer.insert( 0, DataElementPath.escapeName( parent.getName() ) );

            parent = parent.getOrigin();
        }

        return buffer.toString();
    }

    public static DataCollection createRepository(String path) throws Exception
    {
        File file = new File( path, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
        FileInputStream stream = new FileInputStream( file );
        Properties propRepository = new Properties();
        propRepository.load( stream );
        stream.close();
        propRepository.put( DataCollectionConfigConstants.CONFIG_FILE_PROPERTY, file.getAbsolutePath() );
        propRepository.put( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, file.getAbsoluteFile().getParentFile().toString() );
        DataCollection<?> dc = getDataCollection( propRepository.getProperty( DataCollectionConfigConstants.NAME_PROPERTY ) );
        return dc == null ? createCollection( null, propRepository ) : dc;
    }

    public static void copyDataCollection(DataCollection source, DataCollection dest, FunctionJobControl jc, String successMessage)
    {
        Iterator<DataElement> it = source.iterator();
        int size = source.getSize();
        if( jc != null )
            jc.functionStarted( "Copying..." );
        try
        {
            for( int i = 0; it.hasNext(); i++ )
            {
                DataElement de = it.next();
                dest.put( de );
                if( jc != null && i % 10 == 0 )
                {
                    jc.checkStatus();
                    jc.setPreparedness( 100 * i / size );
                }
            }
            if( jc != null )
            {
                if( successMessage == null )
                {
                    successMessage = "Completed";
                }
                jc.functionFinished( successMessage );
            }
        }
        catch( Exception ex )
        {
            if( jc != null )
                jc.functionTerminatedByError( ex );

            log.log( Level.SEVERE, "Copy DataCollection error: ", ex );
        }
    }

    public static void save(@Nonnull DataElement de) throws DataElementPutException
    {
        DataElementPath.create( de ).save( de );
    }

    ////////////////////////////////////////////////////////////////////////////
    // TODO - refactoring
    // FolderDataCollection and Repository are required in package to uncomment other code

    //    /**
    //     * Returns list of DataCollections which start with given root path and can contain elements of specified type
    //     * @param root - String representing path from which search started. If null, then root of repository is assumed
    //     * @param wantedType - Class of wanted objects
    //     * @param limit - maximum number of collections to return (-1 for unlimited -- default)
    //     * @return array of DataCollection objects (empty array if nothing found)
    //     * @see findDataCollectionNames
    //     */
    //    static public <T extends DataElement> DataCollection<T>[] findDataCollections(DataElementPath root, Class<T> wantedType, int limit)
    //    {
    //        List<DataCollection<T>> result = new ArrayList<>();
    //        Set<DataElementPath> current = new HashSet<>();
    //        if( root != null && !root.isEmpty() )
    //            current.add( root );
    //        else
    //            rootMap.keySet().stream().map( DataElementPath::create ).forEach( current::add );
    //        while( !current.isEmpty() )
    //        {
    //            Set<DataElementPath> newCurrent = new HashSet<>();
    //            for( DataElementPath dcName : current )
    //            {
    //                //System.out.println(dcName);
    //                DataCollection<?> dc = dcName.optDataCollection();
    //                if( dc == null )
    //                    continue;
    //                Class<?> elementType = dc.getDataElementType();
    //                if( wantedType.isAssignableFrom( elementType ) )
    //                {
    //                    result.add( (DataCollection<T>)dc );
    //                    if( limit >= 0 && result.size() >= limit )
    //                        return result.toArray( new DataCollection[result.size()] );
    //                }
    //                if( elementType == DataCollection.class || dc instanceof FolderCollection )
    //                {
    //                    dc.stream().filter( de -> de instanceof DataCollection ).map( childDc -> ( (DataCollection<?>)childDc ).getCompletePath() )
    //                            .forEach( newCurrent::add );
    //                }
    //            }
    //            current = newCurrent;
    //        }
    //        return result.toArray( new DataCollection[0] );
    //    }
    //    /**
    //     * Returns list of DataCollections which start with given root path and can contain elements of specified type
    //     * @param root - String representing path from which search started. If null, then root of repository is assumed
    //     * @param wantedType - Class of wanted objects
    //     * @return array of DataCollection objects (empty array if nothing found)
    //     * @see findDataCollectionNames
    //     */
    //    static public <T extends DataElement> DataCollection<T>[] findDataCollections(DataElementPath root, Class<T> wantedType)
    //    {
    //        return findDataCollections( root, wantedType, -1 );
    //    }
    //
    //  ////////////////////////////////////////////////////////////////////////////
    //    // TransformedCollection issues
    //    //
    //
    //    static public DataCollection<?> createTransformedCollection(Repository parent, String name,
    //            Class<?> transformerClass, Class<? extends DataElement> dataElementType, String imgName,
    //            String childrenImage, String fileFilter, String startTag, String idTag, String endTag, String subDir) throws Exception
    //    {
    //        // Create primary collection config file and store it
    //        Properties primary = new ExProperties();
    //        primary.setProperty( DataCollection.CLASS_PROPERTY, FileEntryCollection2.class.getName() );
    //        primary.setProperty( DataCollection.FILE_PROPERTY, name + fileFilter );
    //        primary.setProperty( EntryCollection.ENTRY_START_PROPERTY, startTag );
    //        primary.setProperty( EntryCollection.ENTRY_ID_PROPERTY, idTag );
    //        primary.setProperty( EntryCollection.ENTRY_END_PROPERTY, endTag );
    //        primary.setProperty( EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"" );
    //        primary.setProperty( EntryCollection.ENTRY_KEY_FULL, "true" );
    //
    //        Properties transformed = new ExProperties();
    //        transformed.setProperty( DataCollection.CLASS_PROPERTY, TransformedDataCollection.class.getName() );
    //        transformed.setProperty( DataCollection.TRANSFORMER_CLASS, transformerClass.getName() );
    //
    //        if( dataElementType != null )
    //            transformed.setProperty( DataCollection.DATA_ELEMENT_CLASS_PROPERTY, dataElementType.getName() );
    //        if( imgName != null )
    //            transformed.setProperty( DataCollection.NODE_IMAGE, imgName );
    //        if( childrenImage != null )
    //            transformed.setProperty( DataCollection.CHILDREN_NODE_IMAGE, childrenImage );
    //
    //        return createDerivedCollection( parent, name, primary, transformed, subDir );
    //    }
    //
    //    public static Repository createLocalRepository(Repository parent, String name) throws Exception
    //    {
    //        Properties props = new Properties();
    //        props.setProperty( DataCollection.NAME_PROPERTY, name );
    //        props.setProperty( DataCollection.CLASS_PROPERTY, LocalRepository.class.getName() );
    //        return (Repository)createSubDirCollection( parent, name, props );
    //    }
    //
    //    public static DataCollection<?> createSubDirCollection(Repository parent, String name, Properties primary) throws Exception
    //    {
    //        return parent.createDataCollection( name, primary, name, null, null );
    //    }
    //
    //    public static DataCollection<?> createDerivedCollection(Repository parent, String name, Properties primary, Properties derived,
    //            String subDir) throws Exception
    //    {
    //        if( !primary.containsKey( DataCollection.NAME_PROPERTY ) )
    //            primary.setProperty( DataCollection.NAME_PROPERTY, name );
    //
    //        if( !derived.containsKey( DataCollection.NAME_PROPERTY ) )
    //            derived.setProperty( DataCollection.NAME_PROPERTY, name );
    //        if( !derived.containsKey( DataCollection.NEXT_CONFIG ) )
    //            derived.setProperty( DataCollection.NEXT_CONFIG, name + DataCollection.DEFAULT_FORMAT_CONFIG_SUFFIX );
    //
    //        // Create primary collection config file and store it
    //        File tmpDir = TempFiles.dir( "derivedCollection" );
    //
    //        try
    //        {
    //            File tmp = new File( tmpDir, derived.getProperty( DataCollection.NEXT_CONFIG ) );
    //            ExProperties.store( primary, tmp );
    //            return parent.createDataCollection( name, derived, subDir, new File[] {tmp}, null );
    //        }
    //        finally
    //        {
    //            ApplicationUtils.removeDir( tmpDir );
    //        }
    //    }
    //
    //    static public DataCollection<?> createTransformedFileCollection(Repository parent, String name, String filter,
    //            Class<? extends Transformer<?, ?>> transformerClass) throws Exception
    //    {
    //        return createTransformedFileCollection( parent, name, filter, transformerClass, new Properties() );
    //    }
    //
    //    static public DataCollection<?> createTransformedFileCollection(Repository parent, String name, String filter,
    //            Class<? extends Transformer<?, ?>> transformerClass, Properties additional) throws Exception
    //    {
    //        Properties primary = new Properties();
    //        primary.setProperty( DataCollection.NAME_PROPERTY, name );
    //        primary.setProperty( DataCollection.CLASS_PROPERTY, FileCollection.class.getName() );
    //        primary.setProperty( FileCollection.FILE_FILTER, filter );
    //
    //        Properties derived = new Properties();
    //        for( Object key : additional.keySet() )
    //        {
    //            derived.setProperty( (String)key, additional.getProperty( (String)key ) );
    //        }
    //        derived.setProperty( DataCollection.NAME_PROPERTY, name );
    //        derived.setProperty( DataCollection.CLASS_PROPERTY, TransformedDataCollection.class.getName() );
    //        derived.setProperty( DataCollection.TRANSFORMER_CLASS, transformerClass.getName() );
    //
    //        return createDerivedCollection( parent, name, primary, derived, name );
    //    }
    //
    //    static public <T extends DataElement> DataCollection<T> createTransformedSqlCollection(Repository parent, String name,
    //            Class<? extends SqlTransformer<T>> transformerClass, Class<T> dataElementType, Properties properties) throws Exception
    //    {
    //        properties.setProperty( DataCollection.NAME_PROPERTY, name );
    //        properties.setProperty( DataCollection.CLASS_PROPERTY, SqlDataCollection.class.getName() );
    //        properties.setProperty( SqlDataCollection.SQL_TRANSFORMER_CLASS, transformerClass.getName() );
    //        if( dataElementType != null )
    //            properties.setProperty( DataCollection.DATA_ELEMENT_CLASS_PROPERTY, dataElementType.getName() );
    //
    //        @SuppressWarnings ( "unchecked" )
    //        DataCollection<T> result = parent.createDataCollection( name, properties, null, null, null );
    //
    //        return result;
    //    }
    //

}
