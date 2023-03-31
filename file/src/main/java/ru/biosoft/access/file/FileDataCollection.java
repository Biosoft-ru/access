package ru.biosoft.access.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.Transformer;

//TODO: inherit from FileBasedCollection
public class FileDataCollection extends AbstractDataCollection<DataElement> implements FileBasedCollection<DataElement>, FolderCollection{
	
	private static final String BIOUML_YML_FILE = "biouml.yml";
	protected File rootFolder;
	protected File ymlFile;//optional yml file

	//Layer of descriptors, corresponding DataElements will be created in lazy way
	private Map<String, DataElementDescriptor> descriptors = new ConcurrentHashMap<>();
	
	//Sorted name list, folders first
	private List<String> nameList;
	
	//Parsed content of yaml file
	private Map<String, Object> yaml;
	private Map<String, Map<String, Object>> fileInfoByName;
	
	
	//Constructor used by biouml framework
	public FileDataCollection(DataCollection<?> parent, Properties properties) throws IOException
	{
		super(parent, properties);
		rootFolder = new File(properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY));
		this.ymlFile = new File(rootFolder, BIOUML_YML_FILE);
		
		reInit();
		
		watchFolder();
	}
	
	public synchronized void reInit() throws IOException
	{
		v_cache.clear();
		descriptors.clear();
		nameList = new CopyOnWriteArrayList<String>();
		
		reInitYaml();
		initFromFiles();
	}
	
	private void reInitYaml() throws IOException {
		yaml = Collections.emptyMap();
		fileInfoByName = Collections.emptyMap();

		try {
			if (!ymlFile.exists())
				return;
			YamlParser parser = new YamlParser();
			byte[] bytes = Files.readAllBytes(ymlFile.toPath());
			String text = new String(bytes);
			yaml = parser.parseYaml(text);

			fileInfoByName = new HashMap<>();
			Object filesObj = yaml.get("files");
			if (filesObj != null) {
				List<Map<String, Object>> files = (List<Map<String, Object>>) filesObj;
				for (Map<String, Object> fileInfo : files) {
					String name = (String) fileInfo.get("name");
					fileInfoByName.put(name, fileInfo);
				}
			}

			// Properties of this collection
			Object propsObj = yaml.get("properties");
			if (propsObj instanceof Map) {
				Map<String, String> props = (Map<String, String>) propsObj;
				getInfo().getProperties().putAll(props);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not init from biouml.yml, file will be ignored", e);
			yaml = Collections.emptyMap();
			fileInfoByName = Collections.emptyMap();
		}
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
	
	private boolean isBioUMLYAML(File file)
	{
		return file.getName().equals(BIOUML_YML_FILE);
	}
	
	@Override
	public boolean isFileAccepted(File file)
	{
		if(isBioUMLYAML(file))
			return false;
		
		//TODO: yaml.fileFilter
		return true;
	}
	@Override
	public File getChildFile(String name) {
		return new File(rootFolder, name);
	}
	
	
	private WatchKey watchKey;
	private void watchFolder() throws IOException {
		watchKey = FileSystemWatcher.INSTANCE.watchFolder(rootFolder, new FileSystemListener() {
			
			@Override
			public void added(Path path)throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))
				{
					reInit();
					return;
				}
				if(!isFileAccepted(file))
					return;
				
				fileUpdated(file);
			}
			
			@Override
			public void removed(Path path) throws Exception {
				File file = path.toFile();
				String name = file.getName();
				if(isBioUMLYAML(file))
				{
					reInit();
					return;
				}
				if(!isFileAccepted(file))
					return;

				fileRemoved(name);
			}
			
			@Override
			public void modified(Path path) throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))//TODO: detect what elements were changed and update only them
				{
					reInit();
					return;
				}
				if(!isFileAccepted(file))
					return;
				
				fileUpdated(file);
			}
			
			@Override
			public void overflow(Path dir) throws Exception {
				reInit();
			}
		});
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
			fireElementAdded(FileDataCollection.this, name);
		}else
		{
			DataElement oldFromCache = getFromCache(name);
			removeFromCache(name);
			fireElementChanged(FileDataCollection.this, FileDataCollection.this, name, oldFromCache, null);
		}
	}
	
	//called when file was removed
	private synchronized void fileRemoved(String name)
	{
		nameList.remove(name);
		descriptors.remove(name);
		DataElement oldFromCache = getFromCache(name);
		removeFromCache(name);
		fireElementRemoved(FileDataCollection.this, name, oldFromCache);
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
	protected void doPut(DataElement dataElement, boolean isNew) throws Exception
    {
		ru.biosoft.access.file.Environment ENV = ru.biosoft.access.file.Environment.INSTANCE;
		File file;
		if(dataElement.getClass().equals( ENV.getFileDataElementClass() ))
		{
			File existing = ENV.getFile(dataElement);
			file = getChildFile(dataElement.getName());
			
			try {
				Files.createLink(file.toPath(), existing.toPath());
			} catch(IOException e)
			{
				Files.copy(existing.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);	
			}
		} else if(dataElement instanceof FileDataCollection)
		{
			FileDataCollection fdc = (FileDataCollection) dataElement;
			file = fdc.rootFolder;
			storeElementProperties(fdc, null);
		}
		else {
			Transformer t = ENV.getTransformerForDataElement(dataElement);
			if (t == null)
				throw new UnsupportedOperationException("Can not save element of type " + dataElement.getClass());
			t.init(this, this);
			DataElement fileDataElement = t.transformOutput(dataElement); // Transformer will put file into folder
			storeElementProperties(dataElement, t.getClass());
			file = ENV.getFile(fileDataElement);
		}
		
		
		fileUpdated(file);
    }

	public void storeElementProperties(DataElement de, Class<?> transformerClass) throws IOException {
		Map<String, Object> propertiesAsMap = new LinkedHashMap<>();
		if(de instanceof DataCollection)
		{
			Properties properties = ((DataCollection)de).getInfo().getProperties();
			
			for(Object key : properties.keySet())
			{
				Object value = properties.get(key);
				propertiesAsMap.put((String)key, value);
			}
			propertiesAsMap.remove(DataCollectionConfigConstants.NAME_PROPERTY);
			propertiesAsMap.remove(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
			propertiesAsMap.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);

		}
		if(propertiesAsMap.isEmpty() && transformerClass == null)
			return;
		Map<String, Object> fileInfo = new LinkedHashMap<>();
		fileInfo.put(DataCollectionConfigConstants.NAME_PROPERTY, de.getName());
		if(!propertiesAsMap.isEmpty())
			fileInfo.put("properties", propertiesAsMap);
		if(transformerClass != null)
			fileInfo.put("transformer", transformerClass.getName());
		setFileInfo(fileInfo);
	}

	@Override
    protected void doRemove(String name) throws Exception
    {
    	new File(rootFolder, name).delete();
    	fileRemoved(name);
    }
    
    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
       return descriptors.get(name);
    }
    
    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz) {
    	//TODO: check if we have suitable transformer for this clazz
    	return true;
    }
    
    private DataElementDescriptor createDescriptor(File file) {
    	Map<String, Object> fileInfo = fileInfoByName.get(file.getName());
		Map<String, String> properties = fileInfo == null ? null : (Map<String, String>) fileInfo.get("properties");
    	if(file.isDirectory())
    		return new DataElementDescriptor(FileDataCollection.class, false, properties);
    	else
    	{
    		Transformer transformer = getTransformer(file);
    		Class<? extends DataElement> outputType = transformer == null ?  ru.biosoft.access.file.Environment.INSTANCE.getFileDataElementClass() : transformer.getOutputType();
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
    		
    		//Config path is required to store biouml speciic indices such as JDBM2Index for fasta
    		File configPath = new File(getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY), file.getName());
    		if(!configPath.exists())
    			configPath.mkdirs();
    		
    		properties.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configPath.getAbsolutePath());
    		
    		Map<String, Object> fileInfo = fileInfoByName.get(file.getName());
    		if(fileInfo != null && fileInfo.containsKey("properties"))
    			properties.putAll((Map)fileInfo.get("properties"));
    		
    		return new FileDataCollection(this, properties);
    	}
        DataElement fda = ru.biosoft.access.file.Environment.INSTANCE.createFileDataElement(file.getName(), this, file);
        
        Transformer transformer = getTransformer(file);
        if(transformer == null)
            return fda;
        transformer.init( this, this );
        return transformer.transformInput( fda );
    }

    private Transformer getTransformer(File file)
    {
    	//transformer can be set in biouml.yaml or auto-detected based on file extension
    	Map<String, Object> fileInfo = fileInfoByName.get(file.getName());
    	if(fileInfo != null && fileInfo.containsKey("transformer"))
		{
    		String transformerClassName = (String) fileInfo.get("transformer");
			Class<? extends Transformer> clazz = Environment.loadClass(transformerClassName, Transformer.class);
			try {
				return clazz.newInstance();
			} catch (InstantiationException|IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
        return getTransformerBasedOnFile(file);
        //TODO: transformerParameters
    }
    
    private Transformer  getTransformerBasedOnFile(File file)
    {
    	return ru.biosoft.access.file.Environment.INSTANCE.getTransformerForFile(file);
    }


    @Override
    public void close() throws Exception {
    	super.close();
    	FileSystemWatcher.INSTANCE.stopWatching(watchKey);
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
		Map<String, Object> res = fileInfoByName.get(name);
		if(res == null)
		{
			res = new LinkedHashMap<>();
			res.put("name", name);
		}
		return res;
	}
	public void setFileInfo(Map<String, Object> properties) throws IOException
	{
		String name = (String) properties.get("name");

		Map<String, Object> yaml;
		if(ymlFile.exists()) {
			YamlParser parser = new YamlParser();
			byte[] bytes = Files.readAllBytes(ymlFile.toPath());
			String text = new String(bytes);
			yaml = parser.parseYaml(text);
		}else
			yaml = new HashMap<>();

		fileInfoByName = new HashMap<>();
		Object filesObj = yaml.get("files");
		if(filesObj == null)
			yaml.put("files", filesObj = new ArrayList<>());
		List<Map<String, Object>> files = (List<Map<String, Object>>) filesObj;
		boolean found = false;
		for (int i = 0; i < files.size(); i++) {
			Map<String, Object> fileInfo = files.get(i);
			if (name.equals(fileInfo.get("name")))
			{
				files.set(i, properties);
				found = true;
				break;
			}
		}
		if(!found)
			files.add(properties);
		
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		Yaml parser = new Yaml(options);
		Writer writer = new BufferedWriter(new FileWriter(ymlFile));
		parser.dump(yaml, writer);
		writer.close();
	}
}
