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
import java.util.logging.Level;

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
	private Map<String, DataElementDescriptor> descriptors = new HashMap<>();
	
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
		
		reInitYaml();
		initFromFiles();
		
		watchFolder();
	}
	
	
	public void reInit() throws Exception
	{
		v_cache.clear();
		descriptors.clear();
		nameList.clear();
		
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

	private void initFromFiles() {
		for(File file : rootFolder.listFiles())
		{
			if(!isFileAccepted(file))
				continue;
			DataElementDescriptor descriptor = createDescriptor(file);
			descriptors.put(file.getName(), descriptor);
		}
		nameList = new ArrayList<>();
		nameList.addAll(descriptors.keySet());
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
				String name = file.getName();
				if(isBioUMLYAML(file))
				{
					reInit();
					return;
				}
				if(!isFileAccepted(file))
					return;
				
				DataElementDescriptor descriptor = createDescriptor(file);
				descriptors.put(name, descriptor);
				nameList.add(name);
				sortNameList(nameList);
				fireElementAdded(FileDataCollection.this, name);
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
				
				nameList.remove(name);
				descriptors.remove(name);
				DataElement oldFromCache = getFromCache(name);
				removeFromCache(name);
				fireElementRemoved(FileDataCollection.this, name, oldFromCache);
			}
			
			@Override
			public void modified(Path path) throws Exception {
				File file = path.toFile();
				String name = file.getName();
				if(isBioUMLYAML(file))//TODO: detect what elements were changed and update only them
				{
					reInit();
					return;
				}
				if(!isFileAccepted(file))
					return;
				
				DataElementDescriptor descriptor = createDescriptor(file);
				descriptors.put(name, descriptor);
				DataElement oldFromCache = getFromCache(name);
				removeFromCache(name);
				fireElementChanged(FileDataCollection.this, FileDataCollection.this, name, oldFromCache, null);
			}
			
			@Override
			public void overflow(Path dir) throws Exception {
				reInit();
			}
		});
	}

	@Override
	public List<String> getNameList() {
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
		//TODO: sort folders first
		return true;
	}
	
	@Override
	protected DataElement doGet(String name) throws Exception {
		if(!descriptors.containsKey(name))
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
		if(dataElement.getClass().equals( ENV.getFileDataElementClass() ))
		{
			File existing = ENV.getFile(dataElement);
			File target = getChildFile(dataElement.getName());
			
			try {
				Files.createLink(target.toPath(), existing.toPath());
			} catch(IOException e)
			{
				Files.copy(existing.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);	
			}
			return;
		}
		
		Transformer t = ENV.getTransformerForDataElement(dataElement);
		if(t==null)
			throw new UnsupportedOperationException("Can not save element of type " + dataElement.getClass());
		t.init(this, this);
		t.transformOutput(dataElement);
		//Transformer will put file into folder
		//TODO: Correct extension for the new File or add record in yaml
    }

	@Override
    protected void doRemove(String name) throws Exception
    {
    	new File(rootFolder, name).delete();
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
    	if(file.isDirectory())
    		return new DataElementDescriptor(FileDataCollection.class, false);
    	else
    	{
    		Map<String, Object> fileInfo = fileInfoByName.get(file.getName());
    		Map<String, String> properties = fileInfo == null ? null : (Map<String, String>) fileInfo.get("properties");
    		
    		Transformer transformer = getTransformer(file);
    		Class<? extends DataElement> outputType = transformer == null ?  ru.biosoft.access.file.Environment.INSTANCE.getFileDataElementClass() : transformer.getOutputType();
    		return new DataElementDescriptor(outputType, true, properties);
    	}
	}
    
    public File getFile(String name)
    {
        return new File(rootFolder, name);
    }
    
    private synchronized DataElement createElement(File file) throws Exception
    {
    	if(file.isDirectory())
    	{
    		Properties properties = new Properties();
    		properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, file.getName());
    		properties.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, file.getAbsolutePath());
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
		if (filesObj != null) {
			List<Map<String, Object>> files = (List<Map<String, Object>>) filesObj;
			for(int i = 0; i < files.size(); i++)
			{
				Map<String, Object> fileInfo = new HashMap<>();
				if( name.equals(fileInfo.get("name")))
					files.set(i, properties);
			}
		}
		Yaml parser = new Yaml();
		Writer writer = new BufferedWriter(new FileWriter(ymlFile));
		parser.dump(yaml, writer);
		writer.close();
	}
}
