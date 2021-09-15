package ru.biosoft.access.core;

import static ru.biosoft.access.core.DataCollectionConfigConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;

public class DataCollectionInfo
{
	protected static Logger log = Logger.getLogger(DataCollectionInfo.class.getName());

    protected DataCollection<? extends DataElement> dc = null;

    ////////////////////////////////////////////////////////////////////////////
    // Constructor and initialization issues
    //

    public DataCollectionInfo(DataCollection<? extends DataElement> dc, Properties properties)
    {
        this.properties = properties;
        this.dc = dc;

        if( properties != null )
        {
            String path = properties.getProperty(CONFIG_PATH_PROPERTY, ".");
            if( path != null )
            {
                String imageName = properties.getProperty(NODE_IMAGE);
                if( imageName != null )
                    nodeImage = ( Environment.getImageIcon( path, imageName ) );

                imageName = properties.getProperty(CHILDREN_NODE_IMAGE);
                if( imageName != null )
                    childrenNodeImage = Environment.getImageIcon( path, imageName );
            }

            displayName = properties.getProperty(DISPLAY_NAME_PROPERTY);
            if( displayName == null )
                displayName = dc.getName();

            String strVisible = properties.getProperty(NODE_VISIBLE);
            if( strVisible != null )
                visible = strVisible.equals("true");

            strVisible = properties.getProperty(CHILDREN_LEAF);
            if( strVisible != null )
                childrenLeaf = strVisible.equals("true");

            String strLate = properties.getProperty(LATE_CHILDREN_INITIALIZATION);
            if( strLate != null )
                lateChildrenInitialization = strLate.equals("true");

            Comparator<DataElement> comparator = (Comparator<DataElement>)properties.get(COMPARATOR_OBJECT);
            if( comparator != null )
                setComparator(comparator);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties to customize DataCollection node view in RepositoryPane
    //

    /** DataCollection node name in repository pane. */
    private String displayName;
    public String getDisplayName()
    {
        return displayName;
    }
    public void setDisplayName(String value)
    {
        displayName = value;
    }

    /** DataCollection description. Can be in HTML format. */
    public String getDescription()
    {
        return getProperty(DESCRIPTION_PROPERTY);
    }
    public void setDescription(String description)
    {
        if(description == null)
            getProperties().remove(DESCRIPTION_PROPERTY);
        else
            getProperties().setProperty(DESCRIPTION_PROPERTY, description);
    }


    /** Specifies whether this DataCollection should be visible in repository pane.  */
    private boolean visible = true;
    /**
     * Is this collection should be visible in repository tree.
     * @return Is this collection should be visible in repository tree.
     */
    public boolean isVisible()
    {
        return visible;
    }
    /**
     * Set this collection visible/invisible in repository tree.
     * @param f Is this collection should be visible in repository tree.
     */
    public void setVisible(boolean f)
    {
        visible = f;
    }


    /** Specifies whether DataCollection elements should be visible in Repository pane. */
    private boolean childrenLeaf = false;
    /**
     * Is elements of this collection should be visible in repository tree.
     * @return Is elements of this collection should be visible in repository tree.
     */
    public boolean isChildrenLeaf()
    {
        return childrenLeaf;
    }
    /**
     * Set elements of this collection visible/invisible in repository tree.
     * @param f Is elements of this collection should be visible in repository tree.
     */
    public void setChildrenLeaf(boolean f)
    {
        childrenLeaf = f;
    }

    /** Image for this DataCollection node in repository pane. */
    private ImageIcon nodeImage;
    /** @return image for this DataCollection node in repository pane. */
    public ImageIcon getNodeImage()
    {
        return nodeImage;
    }
    /** Set image for this DataCollection node in repository pane. */
    public void setNodeImage(ImageIcon img)
    {
        nodeImage = img;
    }
    
    /**
     * Sets node image by resource location.
     * 
     * @param className some class
     * @param path resource path relative to class specified by className
     */
    public void setNodeImageLocation(Class<?> className, String path)
    {
        String iconLocation = Environment.getResourceLocation(className, path);
        getProperties().setProperty( NODE_IMAGE, iconLocation);

        setNodeImage( Environment.getImageIcon( iconLocation ) );
    }

    /** Image for DataCollection element nodes in repository pane. */
    private ImageIcon childrenNodeImage = null;
    public ImageIcon getChildrenNodeImage()
    {
        return childrenNodeImage;
    }
    public void setChildrenNodeImage(ImageIcon img)
    {
        childrenNodeImage = img;
    }

    /**
     * Sets children node image by resource location
     * @param className some class
     * @param path resource path relative to class specified by className
     */
    public void setChildrenNodeImageLocation(Class<?> className, String path)
    {
        String iconLocation = Environment.getResourceLocation(className, path);
        if(iconLocation != null)
        {
            getProperties().setProperty(CHILDREN_NODE_IMAGE, iconLocation);
            setChildrenNodeImage( Environment.getImageIcon( iconLocation ) );
        }
    }

    /** Specifies {@link Comparator} to sort DataCollection elements in repository pane. */
    private Comparator<DataElement> comparator;
    public void setComparator(Comparator<DataElement> value)
    {
        comparator = value;
    }
    public Comparator<DataElement> getComparator()
    {
        return comparator;
    }

    private boolean lateChildrenInitialization = true;
    public boolean isLateChildrenInitialization()
    {
        return lateChildrenInitialization;
    }
    public void setLateChildrenInitialization(boolean lateChildrenInitialization)
    {
        this.lateChildrenInitialization = lateChildrenInitialization;
    }

    private String error;

    public String getError()
    {
        return error;
    }
    public void setError(String error)
    {
        this.error = error;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Query system
    //

    /**
     * {@link QuerySystem} to accelerate DataCollection elements filtering.
     *
     */
    private QuerySystem querySystem = null;
    public void setQuerySystem(QuerySystem system)
    {
        this.querySystem = system;
    }
    public QuerySystem getQuerySystem()
    {
        //lasy initialization: init QuerySystem on getUsedFiles or getQuerySystem
        if( querySystem == null )
        {
            initQuerySystem();
        }
        
        return querySystem;
    }
    
    /**
     *  Checks if query system already initialized
     */
    public boolean isQuerySystemInitialized()
    {
        if( querySystem == null )
            return false;
        return true;
    }

    /**
     * 
     */
    protected void initQuerySystem()
    {
        if(properties == null)
            return;

        // TODO
        // Bypass query system creation for local repository as
        // its config may include query system for derived module
        //if(  dc instanceof LocalRepository )
        //    return;

        // try initialize QuerySystem
        try
        {
            String className = properties.getProperty(QuerySystem.QUERY_SYSTEM_CLASS);
            if( className != null )
            {
                Class<? extends QuerySystem> c = Environment.getQuerySystemClassFromRegistry(className);
                if( c == null )
                {
                    c = Environment.loadClass(className, QuerySystem.class);
                }
                Constructor<? extends QuerySystem> constructor = c.getConstructor(DataCollection.class);
                querySystem = constructor.newInstance(dc);

                // register index files
                Index[] indexes = querySystem.getIndexes();
                if( indexes != null )
                {
                    for( int i = 0; i < indexes.length; i++ )
                    {
                        if( indexes[i].getIndexFile() != null )
                            addUsedFile(indexes[i].getIndexFile());
                    }
                }
            }
        }
        catch( Throwable t )
        {
            log.severe("Can not initialize query system for " + dc.getCompletePath() + ": " + ExceptionRegistry.log(t));
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // For Internal usage
    //

    /**
     *  Array with list of all files used by data collection.
     *  This list used for permanently removing of data collection.
     */
    private List<File> usedFiles = null;

    /**
     * Return list of all files used by data collection.
     * @return List of all files used by data collection.
     */
    public List<File> getUsedFiles()
    {
        //lasy initialization: init QuerySystem on getUsedFiles or getQuerySystem
        if( querySystem == null )
        {
            initQuerySystem();
        }
        return usedFiles == null ? null : Collections.unmodifiableList(usedFiles);
    }

    /** Say that file used by data collection. */
    public void addUsedFile(File file)
    {
        if( usedFiles == null )
            usedFiles = new ArrayList<>();
        usedFiles.add(file);
    }

    /** DataCollection properties from config file.*/
    private final Properties properties;
    public Properties getProperties()
    {
        return properties;
    }

    /** Returns value of the specified property from config file.*/
    public String getProperty(String key)
    {
        return properties.getProperty(key);
    }
    
    public <T> Class<? extends T> getPropertyClass(String key, Class<T> superClass) throws DataElementReadException
    {
        Object property = properties.get(key);
        if(property == null)
            property = properties.getProperty(key);
        
        if(property == null)
            throw new DataElementReadException(dc, key);
        try
        {
            Class<?> clazz = null;
            if(property instanceof Class)
            {
                clazz = (Class<?>)property;
            } 
            else
            {
                String plugins = properties.getProperty(PLUGINS_PROPERTY);
                try
                {
                    clazz = Environment.loadClass(property.toString());
                }
                catch( Exception e )
                {
                    if(plugins != null)
                        clazz = Environment.loadClass( property.toString(), plugins);
                }
                
                if(clazz == null)
                    throw new LoggedClassNotFoundException(property.toString(), plugins);
            }
            
            if( superClass.isAssignableFrom(clazz) )
                return (Class<? extends T>) clazz;
            
            throw new LoggedClassCastException(clazz.getName(), superClass.getName());
        }
        catch( Throwable e )
        {
            throw new DataElementReadException(e, dc, key);
        }
    }

    /**
     * Write new property to original config file.
     *
     * Currently this method is essential only for adding {@link QuerySystem}
     * to existing DataCollection. 
     */
    public void writeProperty(String key, String value) throws Exception
    {
        String configPath = properties.getProperty(CONFIG_PATH_PROPERTY);
        String configFile = properties.getProperty(CONFIG_FILE_PROPERTY);
        if( configPath == null || configFile == null )
            throw new NullPointerException("DataCollection " + properties.getProperty(NAME_PROPERTY)
                    + ": config file or path is not specified.");

        configFile = configPath + "/" + configFile;

        FileInputStream fis = new FileInputStream(configFile);
        Properties originalProperties = new Properties();
        originalProperties.load(fis);
        fis.close();

        originalProperties.put(key, value);

        FileOutputStream fos = new FileOutputStream(configFile);
        originalProperties.store(fos, null);
        fos.close();

        properties.put(key, value);
    }
    
    /////
    
    transient Map<String, Object> transientValues = new HashMap<>();
    public void setTransientValue(String key, Object value)
    {
        transientValues.put(key, value);
    }
    
    public Object getTransientValue(String key)
    {
        return transientValues.get(key);
    }
}
