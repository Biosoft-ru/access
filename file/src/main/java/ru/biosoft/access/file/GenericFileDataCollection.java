package ru.biosoft.access.file;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchKey;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FileUtils;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.PropertiesHolder;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.file.MemoryInfoProvider.ChangedInfo;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;


public class GenericFileDataCollection extends AbstractDataCollection<DataElement> implements FileBasedCollection<DataElement>, FolderCollection
{
	protected File rootFolder;
    protected InfoProvider infoProvider;

	//Layer of descriptors, corresponding DataElements will be created in lazy way
	private Map<String, DataElementDescriptor> descriptors = new ConcurrentHashMap<>();
	
	//Sorted name list, folders first
	private List<String> nameList;
	
    private WatchKey watchKey;
    private FileSystemListener listener;

    private FilePatternFilter filter;
	
	//Constructor used by biouml framework
	public GenericFileDataCollection(DataCollection<?> parent, Properties properties) throws IOException
	{
		super(parent, properties);
		rootFolder = new File(properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY));
		
		createInfoProvider(properties);
		getInfo().getProperties().putAll(infoProvider.getProperties());
		
		reInit();
		
		watchFolder();
	}
	
	private void createInfoProvider(Properties properties) throws IOException {
		String infoProviderStr = properties.getProperty("infoProvider", "yaml");
		switch(infoProviderStr)
		{
		case "yaml":
			infoProvider = new YamlInfoProvider(rootFolder);break;
		case "sql":
			throw new IllegalArgumentException("sql info provider not yet supported");
		default:
			throw new IllegalArgumentException("Unknown info provider: " + infoProviderStr);
		}
		infoProvider.addListener(new InfoProviderListener() {
			@Override
			public void infoChanged() throws Exception {
				reInit();
			}

            @Override
            public void infoChanged(Object changed) throws Exception
            {
                if( changed instanceof ChangedInfo )
                    reInitChanged( (ChangedInfo) changed );
                else
                    reInit();

            }
		});
	}

	public synchronized void reInit() throws IOException
	{
        filter = new FilePatternFilter( rootFolder, infoProvider.getFileFilter() );
		v_cache.clear();
		descriptors.clear();
		nameList = new CopyOnWriteArrayList<String>();
		initFromFiles();
	}

    public synchronized void reInitChanged(ChangedInfo changed) throws IOException
    {
        Set<String> toReinit = new HashSet<>( changed.added );
        for ( String deleted : changed.deleted )
        {
            v_cache.remove( deleted );
            descriptors.remove( deleted );
            nameList.remove( deleted );
        }
        for ( String modified : changed.modified )
        {
            v_cache.remove( modified );
            descriptors.remove( modified );
            nameList.remove( modified );
            toReinit.add( modified );
        }
        if( toReinit.isEmpty() )
            return;
        nameList = new CopyOnWriteArrayList<String>();
        initFromFilesChanged( toReinit );
    }

	private synchronized void initFromFiles() {
		for(File file : rootFolder.listFiles())
		{
			if(!isFileAccepted(file))
				continue;
			DataElementDescriptor descriptor = createDescriptor(file);
			descriptors.put(file.getName(), descriptor);
		}
		nameList = new CopyOnWriteArrayList<>(descriptors.keySet());
		sortNameList(nameList);
	}
	
    private synchronized void initFromFilesChanged(Set<String> changed)
    {
        for ( File file : rootFolder.listFiles() )
        {
            if( !changed.contains( file.getName() ) )
                continue;
            if( !isFileAccepted( file ) )
                continue;
            DataElementDescriptor descriptor = createDescriptor( file );
            descriptors.put( file.getName(), descriptor );
        }
        nameList = new CopyOnWriteArrayList<>( descriptors.keySet() );
        sortNameList( nameList );
    }

	@Override
	public boolean isFileAccepted(File file)
	{
		if(YamlInfoProvider.isBioUMLYAML(file))
			return false;
		boolean recursive = (Boolean) infoProvider.getProperties().getOrDefault("recursie", true);
		if(recursive && file.isDirectory())
            if( filter.isExcluded( file ) )
                return false;
            else
                return true;
        if( filter.isExcluded( file ) )
		    return false;
        return true;
//		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher( "glob:" + fileFilter );
//		return pathMatcher.matches(Paths.get(file.getName()));
	}
	
	@Override
	public File getChildFile(String name) {
		return new File(rootFolder, name);
	}
	

	private void watchFolder() throws IOException {
        listener = new FileSystemListener()
        {
			@Override
			public void added(Path path)throws Exception {
				File file = path.toFile();
				if(!isFileAccepted(file))
					return;
				
				fileUpdated(file);
			}
			
			@Override
			public void removed(Path path) throws Exception {
				File file = path.toFile();
				String name = file.getName();
				if(!isFileAccepted(file))
					return;

				fileRemoved(name);
			}
			
			@Override
			public void modified(Path path) throws Exception {
				File file = path.toFile();
				if(!isFileAccepted(file))
					return;
				
				fileUpdated(file);
			}
			
			@Override
			public void overflow(Path dir) throws Exception {
				reInit();
			}
        };
        watchKey = FileSystemWatcher.INSTANCE.watchFolder( rootFolder, listener );
	}
	
	//called when file was added or modified
	private synchronized void fileUpdated(File file)
	{
		String name = file.getName();
		boolean isNew = !descriptors.containsKey(name); 
		DataElementDescriptor descriptor = createDescriptor(file);
		descriptors.put(name, descriptor);
		if(isNew)
		{
			nameList.add(name);
			sortNameList(nameList);
			fireElementAdded(GenericFileDataCollection.this, name);
		}else
		{
			DataElement oldFromCache = getFromCache(name);
			removeFromCache(name);
			fireElementChanged(GenericFileDataCollection.this, GenericFileDataCollection.this, name, oldFromCache, null);
		}
	}
	
	//called when file was removed
	private synchronized void fileRemoved(String name)
	{
		nameList.remove(name);
		descriptors.remove(name);
		DataElement oldFromCache = getFromCache(name);
		removeFromCache(name);
		fireElementRemoved(GenericFileDataCollection.this, name, oldFromCache);
	}
	

	@Override
	public synchronized List<String> getNameList() {
		return nameList;
	}

	@Override
	protected boolean sortNameList(List<String> list) {
		Comparator<String> cmp = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				DataElementDescriptor d1 = descriptors.get(o1);
				DataElementDescriptor d2 = descriptors.get(o2);
				if(d1.isLeaf() == d2.isLeaf())
					return o1.compareTo(o2);
				if(d1.isLeaf())
					return 1;
				return -1;
			}
		};
		Collections.sort(list, cmp );
		return true;
	}
	
	@Override
	public synchronized DataElement get(String name) throws Exception {
		return super.get(name);
	}
	
	@Override
	protected DataElement doGet(String name) throws Exception {
		if(!descriptors.containsKey(name))//TODO: synchronize access to descriptors
			return null;
		File file = getFile(name);
		if (!file.exists())
			return null;
		return createElement(file);
	}
	
	@Override
	public synchronized DataElement put(DataElement element) throws DataElementPutException {
		DataElement old = super.put(element);
		removeFromCache(element.getName());//AbstractDataCollection will put element into cache, but FileDataCollection wants to recreate it from file resulting in possibly distinct element
		return old;
	}

	@Override
	protected void doPut(DataElement dataElement, boolean isNew) throws Exception
    {
        File file = null;
		if(dataElement.getClass().equals( FileDataElement.class ))
		{
			FileDataElement fde = (FileDataElement) dataElement;
			File existing = fde.getFile();
			file = getChildFile(dataElement.getName());
			
			if(!file.equals(existing))
			{
				try {
					Files.createLink(file.toPath(), existing.toPath());
                }
                catch (IOException e)
				{
					Files.copy(existing.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);	
				}
			}
        }
        else if( dataElement instanceof GenericFileDataCollection )
		{
			GenericFileDataCollection fdc = (GenericFileDataCollection) dataElement;
			file = fdc.rootFolder;
			storeElementProperties(fdc, null);
		}
        else
        {
			ru.biosoft.access.file.v1.Environment ENV = ru.biosoft.access.file.v1.Environment.INSTANCE;
			Transformer t = ENV.getTransformerForDataElement(dataElement);
			if (t == null)
				throw new UnsupportedOperationException("Can not save element of type " + dataElement.getClass());
			t.init(this, this);
			FileDataElement fde = (FileDataElement)t.transformOutput(dataElement); // Transformer will put file into folder
            Properties properties = null;
            if( dataElement instanceof PropertiesHolder )
                properties = ((PropertiesHolder) dataElement).getProperties();
            else if(dataElement instanceof DataCollection)
                properties = ((DataCollection) dataElement).getInfo().getProperties();
            storeElementProperties(fde, t.getClass(), properties);
			file = fde.getFile();
		}
		
		fileUpdated(file);
    }

    public void storeElementProperties(DataElement de, Class<?> transformerClass) throws Exception
    {
        storeElementProperties(de, transformerClass, de instanceof DataCollection ? ((DataCollection) de).getInfo().getProperties() : null);
    }

    public void storeElementProperties(DataElement de, Class<?> transformerClass, Properties properties) throws Exception
    {
		Map<String, Object> propertiesAsMap = new LinkedHashMap<>();
        if( properties != null )
		{
			for(Object key : properties.keySet())
			{
				Object value = properties.get(key);
				propertiesAsMap.put((String)key, value);
			}
			propertiesAsMap.remove(DataCollectionConfigConstants.NAME_PROPERTY);
			propertiesAsMap.remove(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
			propertiesAsMap.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
            propertiesAsMap.remove(DataCollectionConfigConstants.FILE_PROPERTY);

		}
        //        if( propertiesAsMap.isEmpty() )
        //			return;
		Map<String, Object> fileInfo = new LinkedHashMap<>();
		fileInfo.put(DataCollectionConfigConstants.NAME_PROPERTY, de.getName());
		if(!propertiesAsMap.isEmpty())
			fileInfo.put("properties", propertiesAsMap);
		if(transformerClass != null)
        {
			fileInfo.put("transformer", transformerClass.getName());
            FileType type = FileTypeRegistry.getFileTypeByTransformer( transformerClass.getName() );
            if( type != null )
                fileInfo.put( "type", type.getName() );
        }
		setFileInfo(fileInfo);
	}

	@Override
    protected void doRemove(String name) throws Exception
    {
        DataElement oldElement = null;
        try
        {
            oldElement = doGet(name);
        }
        catch (Exception e)
        {
        }
        File file = new File(rootFolder, name);
        if ( file.isDirectory() )
            FileUtils.deleteDirectory(file);
        else
            file.delete();
        if ( oldElement != null && oldElement instanceof DataCollection )
        {
            DataCollectionInfo dci = ((DataCollection) oldElement).getInfo();
            List<File> usedFiles = dci.getUsedFiles();
            if ( usedFiles != null )
                for ( File f : usedFiles )
                {
                    try
                    {
                        if ( f.isDirectory() )
                            FileUtils.deleteDirectory(f);
                        else
                            f.delete();
                    }
                    catch (Exception e)
                    {
                    }
                }
        }
    	fileRemoved(name);
    }
    
    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
       return descriptors.get(name);
    }
    
    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz) {
        return isAcceptable( clazz, true );
    }

    public boolean isAcceptable(Class<? extends DataElement> clazz, boolean strict)
    {
        if( FileDataElement.class.equals( clazz ) || clazz.isAssignableFrom( FolderCollection.class ) )
            return true;

        ru.biosoft.access.file.v1.Environment ENV = ru.biosoft.access.file.v1.Environment.INSTANCE;
        List<Class<? extends Transformer>> tClasses = ENV.getTransformerForClass( FileDataElement.class, clazz, strict );
        return !(tClasses.isEmpty());
    }
    
    private DataElementDescriptor createDescriptor(File file) {
    	Map<String, Object> fileInfo = infoProvider.getFileInfo(file.getName());
		Map<String, String> properties = fileInfo == null ? null : (Map<String, String>) fileInfo.get("properties");
    	if(file.isDirectory())
    		return new DataElementDescriptor(GenericFileDataCollection.class, false, properties);
    	else
    	{
    		Transformer transformer = getTransformer(file);
    		Class<? extends DataElement> outputType = transformer == null ?  FileDataElement.class : transformer.getOutputType();
    		return new DataElementDescriptor(outputType, true, properties);
    	}
	}
    
    public File getFile(String name)
    {
        return new File(rootFolder, name);
    }
    
    private DataElement createElement(File file) throws Exception
    {
    	if(file.isDirectory())
    	{
    		Properties properties = new Properties();
    		properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, file.getName());
    		properties.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, file.getAbsolutePath());

    		/*
    		//Config path is required to store biouml speciic indices such as JDBM2Index for fasta
    		File configPath = new File(getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY), file.getName());
    		if(!configPath.exists())
    			configPath.mkdirs();
    		
    		properties.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configPath.getAbsolutePath());
    		*/
    		
    		
    		Map<String, Object> fileInfo = infoProvider.getFileInfo(file.getName());
    		if(fileInfo != null && fileInfo.containsKey("properties"))
    			properties.putAll((Map)fileInfo.get("properties"));
    		
            GenericFileDataCollection result = new GenericFileDataCollection(this, properties);
            
            //result.getInfo().addUsedFile(configPath);
            
            return result;
    	}
        FileDataElement fda = new FileDataElement(file.getName(), this, file);
        
        Map<String, Object> fileInfo = infoProvider.getFileInfo(file.getName());
        Transformer transformer = getTransformer(file);
        if(transformer == null)
            return fda;
        transformer.init( this, this );

        //@todo: pass Properties to transformer before element is created
        Properties propertiesFromYaml = new Properties();
        if( fileInfo != null && fileInfo.containsKey("properties") )
        {
            Map fileInfoMap = (Map) fileInfo.get("properties");
            for ( Object propertyName : fileInfoMap.keySet() )
            {
                propertiesFromYaml.setProperty(String.valueOf(propertyName), String.valueOf(fileInfoMap.get(propertyName)));
            }
        }
        if( transformer instanceof PropertiesHolder )
        {
            ((PropertiesHolder) transformer).setProperties(propertiesFromYaml);
        }

        DataElement result = transformer.transformInput( fda );
        if(result instanceof DataCollection)
        {
        	Properties properties = ((DataCollection) result).getInfo().getProperties();
            properties.putAll(propertiesFromYaml);
        }
        
		return result;
    }

    private Transformer getTransformer(File file)
    {
    	//file type can be set in .info or auto-detected based on file extension
    	Map<String, Object> fileInfo = infoProvider.getFileInfo(file.getName());
    	if(fileInfo != null && fileInfo.containsKey("type"))
		{
    		String type = (String) fileInfo.get("type");
    		FileType ft = FileTypeRegistry.getFileType(type);
    		if(ft == null)
    		{
    			log.warning("Can not find " + type + " in FileTypeRegistry");
    			return null;
    		}
    		return createTransformer(ft.getTransformerClassName());
		}
        return getTransformerBasedOnFile(file);
        //TODO: transformerParameters
    }
    
    private Transformer  getTransformerBasedOnFile(File file)
    {
    	FileType ft = FileTypeRegistry.detectFileType(file);
    	return createTransformer(ft.getTransformerClassName());
    }
    
    private static Transformer createTransformer(String className)
    {
    	if(className == null)
    		return null;
    	Class<? extends Transformer> clazz;
        try
        {
            clazz = Environment.loadClass(className, Transformer.class);
        }
        catch (LoggedClassNotFoundException | LoggedClassCastException e)
        {
            return null;
        }

        try
        {
            return clazz.getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
        {
			throw new RuntimeException(e);
		}
    }


    @Override
    public void close() throws Exception {
    	super.close();
    	infoProvider.close();
        FileSystemWatcher.INSTANCE.stopWatching( watchKey, listener );
    }
    
    @Override
    protected void finalize() throws Throwable {
    	close();
    	super.finalize();
    }

	@Override
	public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz) {
		File folder = new File(rootFolder, name);
		folder.mkdir();
		fileUpdated(folder);
		try {
			return (DataCollection) get(name);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<String, Object> getFileInfo(String name)
	{
		Map<String, Object> res = infoProvider.getFileInfo(name);
		if(res == null)
		{
			res = new LinkedHashMap<>();
			res.put("name", name);
		}
		return res;
	}
	
	public void setFileInfo(Map<String, Object> properties) throws Exception
	{
		infoProvider.setFileInfo(properties);
	}

    public static DataCollection<?> initGenericFileDataCollection(DataCollection parent, File dir) throws Exception
    {
        return initGenericFileDataCollection( parent, dir, null );
    }

    public static DataCollection<?> initGenericFileDataCollection(DataCollection parent, File dir, Properties properties) throws Exception
    {
        Properties collectionProperties = properties == null ? new Properties() : (Properties) properties.clone();
        //Read all properties from yaml file so they could be used in super constructor
        YamlInfoProvider provider = new YamlInfoProvider( dir );
        collectionProperties.putAll( provider.getProperties() );
        provider.setProperties( collectionProperties );

        collectionProperties.putIfAbsent( DataCollectionConfigConstants.CLASS_PROPERTY, GenericFileDataCollection.class.getName() );
        collectionProperties.putIfAbsent( DataCollectionConfigConstants.NAME_PROPERTY, dir.getName() );
        collectionProperties.putIfAbsent( DataCollectionConfigConstants.FILE_PATH_PROPERTY, dir.getPath() );

        return CollectionFactory.createCollection( parent, collectionProperties );
    }

    public Map<String, Object> getChildProperties(String name, String transformerClass)
    {
        if( transformerClass == null )
            return null;
        Transformer transformer = createTransformer(transformerClass);
        if( transformer == null )
            return null; //Element not supported for this collection

        Map<String, Object> properties = new LinkedHashMap<>();
        //Get empty properties fields from transformer
        //@todo: should create by element or get from element? 
        //@todo: properties should be DynamicProperties with editor etc, not only strings
        if( transformer instanceof PropertiesHolder )
        {
            Properties p = ((PropertiesHolder) transformer).createProperties();
            p.entrySet().forEach(e -> {
                properties.put(e.getKey().toString(), e.getValue());
            });
        }
        //Get properties from yaml file, overwrite empty ones created by transformer
        Map<String, Object> fileInfo = infoProvider.getFileInfo(name);
        if( fileInfo != null && fileInfo.containsKey("properties") )
        {
            Map fileInfoMap = (Map) fileInfo.get("properties");
            for ( Object propertyName : fileInfoMap.keySet() )
            {
                properties.put(String.valueOf(propertyName), fileInfoMap.get(propertyName));
            }
        }
        return properties;
    }
}
