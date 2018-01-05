package ru.biosoft.access.core;

/**
 * Property names that are widely used for DataCollection construction via Properties.  
 */
public class DataCollectionConfigConstants 
{
    /** Property for storing name of data collection. */
    public static final String NAME_PROPERTY = "name";

    /** Property for storing display name of data collection. */
    public static final String DISPLAY_NAME_PROPERTY = "displayName";
    
    /** Property for storing complete name of data collection. */
    public static final String COMPLETE_NAME_PROPERTY = "completeName";

    /** Property for storing class of data collection. */
    public static final String CLASS_PROPERTY = "class";

    /** Property for storing class name of data elements stored in this data collection. */
    public static final String DATA_ELEMENT_CLASS_PROPERTY = "data-element-class";

    /** Property for storing data collection description. Can be in HTML format. */
    public static final String DESCRIPTION_PROPERTY = "description";

    /** Property for storing class of data collection. */
    public static final String CLASSPATH_JAR_PROPERTY = "classpath-jar";

    /** Property for storing path to data collection config file. */
    public static final String CONFIG_PATH_PROPERTY = "configPath";

    /** Property for storing config file name. */
    public static final String CONFIG_FILE_PROPERTY = "configFile";
   
    /** Property for storing path to data collection file. */
    public static final String FILE_PATH_PROPERTY = "filePath";

    /** Property for storing name of data collection's file. */
    public static final String FILE_PROPERTY = "file";

    /** Property for storing filter of data collection's file. */
    public static final String FILTER_PROPERTY = "filter";

    /** Size of the element data on the disk (if available) */
    public static final String ELEMENT_SIZE_PROPERTY = "elementSize";

    /** Property for storing name of default data collection's config file. */
    public static final String DEFAULT_CONFIG_FILE = "default.config";

    /** Property for storing name of default repository file. */
    public static final String DEFAULT_REPOSITORY = "default.repository";

    /** Property for storing default suffix of data collection's config file. */
    public static final String DEFAULT_CONFIG_SUFFIX = ".config";

    /** Property for storing suffix of {@link ru.biosoft.access.LocalRepository} data collections */
    public static final String DEFAULT_NODE_CONFIG_SUFFIX = ".node.config";

    /** Property for storing suffix of {@link ru.biosoft.access.FileEntryCollection} data collections */
    public static final String DEFAULT_FORMAT_CONFIG_SUFFIX = ".format.config";

    /** Property for storing suffix of {@link ru.biosoft.access.FilteredDataCollection} data collections */
    public static final String DEFAULT_FILTER_CONFIG_SUFFIX = ".filter.config";


    /** Property for storing of image file names used for nodes of {@link ru.biosoft.access.LocalRepository} collections*/
    public static final String NODE_IMAGE = "node-image";

    /** Property for storing of image file names used for nodes of DataElement elements stored in {@link ru.biosoft.access.LocalRepository} collection */
    public static final String CHILDREN_NODE_IMAGE = "childrenNodeImage";

    /** Property for storing of visible flag. This flag is used for defining whether current node will be displayed in a Repository tree*/
    public static final String NODE_VISIBLE = "nodeVisible";

    /** Property for storing of  visible state. This flag is used for defining whether DataElement leafs of current node will be displayed in a Repository tree*/
    public static final String CHILDREN_LEAF = "isChildrenLeaf";
    
    /** Property indicating if collection or DataElement should be displayed as leaf in Repository */
    public static final String IS_LEAF = "isLeaf";

    /** Property for storing of */
    public static final String LATE_CHILDREN_INITIALIZATION = "lateChildrenInitialization";

    /** When removing data element from this DC, whether to remove its children  */
    public static final String REMOVE_CHILDREN = "remove-children";
    
    
    /*  Property for storing of primary data collection for derivedDataCollection */
    public static final String PRIMARY_COLLECTION = "primaryCollection";

    /** Property for storing class of transformer for {@link ru.biosoft.access.TransformedDataCollection} collections  */
    public static final String TRANSFORMER_CLASS = "transformer";
    
    /**
     * Property for storing  name of primary data collection config file. It is used for
     * {@link ru.biosoft.access.TransformedDataCollection}'s to specify primary data collection
     */
    public static final String NEXT_CONFIG = "nextConfig";
    
    /** Property for checking whether the data collection is mutable */
    public static final String MUTABLE = "mutable";

    public static final String COMPARATOR_OBJECT = "comparator-object";

    /** Indicates whether this data collection should be registered as root by {@link CollectionFactory#registerRoot}. */
    public static final String IS_ROOT = "root";

    /** Format for automatical name (identifier) generation. */
    public static final String ID_FORMAT = "id-format";
    
    /**
     * When creating new data element in this DC, whether to ask
     * user for element name or generate it automatically.
     * When "true", ID_FORMAT property should also be set.
     */
    public static final String ASK_USER_FOR_ID = "ask-user-for-id";
    
    public static final String CAN_CREATE_ELEMENT_FROM_BEAN = "can-create-element-from-bean";
    
    /** Database reference template public static final String */
    public static final String URL_TEMPLATE = "url-template";
    
    /**
     * Strategy for child caching. Possible values:
     * none - do not cache elements
     * weak - hold reference until GC starts (default)
     * soft - hold reference while enough space available
     * hard - persistently hold reference.
     */
    public static final String CACHING_STRATEGY = "caching-strategy";

    /** Disk quota (in bytes) for given collection */
    public static final String DISK_QUOTA_PROPERTY = "diskQuota";

    /** Property for storing necessary plugin ids. */
    public static final String PLUGINS_PROPERTY = "plugins";

    public static final String CAN_OPEN_AS_TABLE = "openAsTable";

    public static final String JOB_CONTROL_PROPERTY = "job-control";
}
