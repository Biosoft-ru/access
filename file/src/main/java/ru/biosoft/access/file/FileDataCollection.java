package ru.biosoft.access.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.VectorDataCollection;


/**
 * FileDataCollection is {@link DataCollection} for storing files and directories
 * using {@link FileDataElement} and FileDataCollection objects correspondingly.
 * Thus for the specified directory the hierarchy of files and directories can be wrapped into this DataCollection.
 * 
 * FileDataCollection watches for file changes in the corresponding directory using {@link java.nio.WatchService}.     
 */
public class FileDataCollection<T extends DataElement> extends VectorDataCollection<DataElement>
{
    /** Property for storing filter file extension. */
    public static final String FILE_FILTER = "fileFilter";
    public static final String FILE_SUFFIX = "fileSuffix";
    public static final String WATCHED = "watched";

    /** The subdirectory corresponded to this collection. */
    protected File root;

    /**
     * FileFilter is used to select needed files from root directory as well as to check added filed.
     * 
     * @see #doPut(DataElement )
     */
    protected FileFilter filter;

    ////////////////////////////////////////
    // constructors
    //

    /**
     * Constructs data collection with parent.
     * Makes info for this data collection.
     * <ul>Required properties
     *   <li>{@link ru.biosoft.access.core.DataCollection#DataCollectionConfigConstants.NAME_PROPERTY}</li>
     *   <li>{@link ru.biosoft.access.core.DataCollection#PATH_PROPERTY}</li>
     * </ul>
     * <ul>Optional properties
     *   <li>{@link #FILE_FILTER}</li>
     *   <li>{@link ru.biosoft.access.core.DataCollection#DataCollectionConfigConstants.NODE_IMAGE}</li>
     *   <li>{@link ru.biosoft.access.core.DataCollection#DataCollectionConfigConstants.CHILDREN_NODE_IMAGE}</li>
     *   <li>{@link ru.biosoft.access.core.DataCollection#DataCollectionConfigConstants.NODE_VISIBLE}</li>
     * </ul>
     * @param parent Parent data collection.
     * @param properties Properties for creating data collection .
     */
    public FileDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        String file = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
        fileSuffix = properties.getProperty(FILE_SUFFIX);
        if( file == null )
        {
           file = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY); // pending - (by zha) is this correct?
           properties.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, file);
        }
        root = new File( file );
        init(properties);
    }

    /**
     * Initialize file collection from the properties.
     * @throws RuntimeException If error occurs.
     */
    protected void init(Properties properties)
    {
        //create FileFilter
        filter = new SimpleFileFilter(properties.getProperty(FILE_FILTER));
        File[] files = root.listFiles(filter);
        if( files!=null )
        {
            try
            {
                for( File file : files )
                {
                    if ( filter.accept ( file ) )
                    {
                        String name = file.getName();
                        if( fileSuffix != null )
                        {
                            if( !name.endsWith(fileSuffix) )
                                continue; // don't accept files with invalid suffix even if they pass filter
                            name = name.substring(0, name.length() - fileSuffix.length());
                        }

                        super.doPut(createElement(name, file), true);
                        getInfo().addUsedFile( file );
                    }
                }
            }
            catch (Exception exc)
            {
                log.log(Level.SEVERE, "Error during file collection creating root="+root,exc);
                throw new RuntimeException("FileCollection failed root="+root,exc);
            }
        }
    }
    
    public void reinit()
    {
        File[] files = root.listFiles(filter);
        if( files != null )
        {
            for( File file : files )
            {
                if( filter.accept( file ) )
                {
                    String name = file.getName();
                    if( fileSuffix != null )
                    {
                        if( !name.endsWith( fileSuffix ) )
                            continue; // don't accept files with invalid suffix even if they pass filter
                        name = name.substring( 0, name.length() - fileSuffix.length() );
                    }

                    if( contains( name ) )
                        continue;

                    put( createElement( name, file ) );
                    getInfo().addUsedFile( file );
                }
            }
        }
        
        for(FileDataElement de : this)
        {
            if(!de.getFile().exists())
                try
                {
                    remove( de.getName() );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not remove data element " + de.getName(), e);
                }
        }
    }

    /**
     * Creates element
     * @param name
     * @param file
     * @return
     */
    protected FileDataElement createElement(String name, File file)
    {
        return new FileDataElement(name, this, file);
    }

    /**
     * Returns <code>FileDataElement.class</code>
     *
     * @return <code>FileDataElement.class</code>
     */
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return FileDataElement.class;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        ArrayList<String> result = new ArrayList<>(super.getNameList());
        sortNameList(result);
        return result;
    }

    /**
     * Returns <code>true</code>
     *
     * @return <code>true</code>
     */
    /*
    public boolean isMutable()
    {
        return true;
    }
    */

    /**
     * Puts FileDataElement to the collection .
     *
     * @param dataElement FileDataElement object
     * @exception Exception If current filter does not accept file (has other extension) .
     * @see AbstractDataCollection#put(DataElement)
     */
    @Override
    protected void doPut(FileDataElement dataElement, boolean isNew)
    {
        try
        {
            File src = dataElement.getFile();

            // PENDING: what type of exception should be used
            if( !filter.accept( src ) )
                throw new Exception( "incompatible file type: " + src );

            File dst = new File( root, src.getName() );
            src.renameTo( dst );

            ( dataElement ).setFile( dst );
        }
        catch( Exception e )
        {
            throw new DataElementPutException( e, getCompletePath().getChildPath( dataElement.getName() ) );
        }
        super.doPut( dataElement, isNew );
    }

    /**
     * Checks whether passed file is suitable for putting to this collection
     */
    @Override
    public boolean isFileAccepted(File file)
    {
        return filter.accept(file);
    }

    protected File getFile()
    {
        return root;
    }

    /**
     * Removes FileDataElement element from collection,
     * and removes corresponding file in subdirectory.
     *
     * @param de Removed FileDataElement element
     * @exception Exception If removal is not successful.
     */
    @Override
    protected void doRemove( String name ) throws Exception
    {
        File file = new File( root, fileSuffix == null ? name : name+fileSuffix );
        if( !file.delete() )
            throw new DataElementPutException(new IOException("File can not be destroyed: "+file.getAbsolutePath()), getCompletePath().getChildPath( name ));
        super.doRemove( name );
    }

    /**
    * Implements simple filter for files.
    * @see FileFilter
    */
    private static class SimpleFileFilter implements FileFilter
    {
        protected String[] suffixes;

        public SimpleFileFilter(String suffixesString)
        {
            if( suffixesString == null )
                suffixesString = "*";

            if ( !suffixesString.equals("*") )
            {
                StringTokenizer tokens = new StringTokenizer(suffixesString, " ,;");
                suffixes = new String[tokens.countTokens()];

                for (int i = 0; i < suffixes.length; i++)
                    suffixes[i] = tokens.nextToken();
            } else
            {
                suffixes = new String[] {"*"};
            }
        }

        @Override
        public boolean accept(File pathname)
        {
            if(pathname.getName().equals("default.config")) return false;
            if (suffixes == null || suffixes.length==0 )
            {
                if ( pathname.isDirectory() )
                    return false;
                String name = pathname.getName();
                return(name.indexOf(".")==-1);
            }

            if ( suffixes[0].equals("*") )
                return true;

            String name = pathname.getName();

            for( String suffixe : suffixes )
            {
                if (name.toLowerCase().endsWith(suffixe.toLowerCase()))
                    return true;
            }

            return false;
        }
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        FileDataElement de = doGet(name);
        if(de == null) return null;
        Map<String, String> properties = new HashMap<>();
        properties.put(DataCollectionConfigConstants.NODE_IMAGE, IconFactory.getClassIconId(getDataElementType()));
        properties.put(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY, String.valueOf(de.getContentLength()));
        return new DataElementDescriptor(getDataElementType(), true, properties);
    }
}


