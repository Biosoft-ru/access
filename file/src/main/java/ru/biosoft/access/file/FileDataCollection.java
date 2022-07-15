package ru.biosoft.access.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.Transformer;

//TODO: inherit from FileBasedCollection
public class FileDataCollection extends AbstractDataCollection<DataElement> {
	
	protected File rootFolder;
	protected File ymlFile;//optional yml file

	//Layer of descriptors, corresponding DataElements will be created in lazy way
	private Map<String, DataElementDescriptor> descriptors = new HashMap<>();
	
	//Sorted name list, folders first
	private List<String> nameList;
	
	
	//Constructor used by biouml framework
	public FileDataCollection(DataCollection<?> parent, Properties properties) throws IOException
	{
		super(parent, properties);
		rootFolder = new File(properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY));
		this.ymlFile = new File(rootFolder, ".biouml.yml");
		
		initFromFiles();
		watchFolder();
	}
	
	private void initFromFiles() {
		for(File file : rootFolder.listFiles())
		{
			DataElementDescriptor descriptor = createDescriptor(file);
			descriptors.put(file.getName(), descriptor);
		}
		nameList = new ArrayList<>();
		nameList.addAll(descriptors.keySet());
		sortNameList(nameList);
	}
	
	
	private WatchKey watchKey;
	private void watchFolder() throws IOException {
		watchKey = FileSystemWatcher.INSTANCE.watchFolder(rootFolder, new FileSystemListener() {
			
			@Override
			public void added(Path path) {
				File file = path.toFile();
				String name = file.getName();
				DataElementDescriptor descriptor = createDescriptor(file);
				descriptors.put(name, descriptor);
				nameList.add(name);
				sortNameList(nameList);
				fireElementAdded(FileDataCollection.this, name);
			}
			
			@Override
			public void removed(Path path) {
				File file = path.toFile();
				String name = file.getName();
				nameList.remove(name);
				descriptors.remove(name);
				DataElement oldFromCache = getFromCache(name);
				removeFromCache(name);
				fireElementRemoved(FileDataCollection.this, name, oldFromCache);
			}
			
			@Override
			public void modified(Path path) {
				File file = path.toFile();
				String name = file.getName();
				DataElementDescriptor descriptor = createDescriptor(file);
				descriptors.put(name, descriptor);
				DataElement oldFromCache = getFromCache(name);
				removeFromCache(name);
				fireElementChanged(FileDataCollection.this, FileDataCollection.this, name, oldFromCache, null);
			}
			
			@Override
			public void overflow(Path dir) {
				v_cache.clear();
				descriptors.clear();
				nameList.clear();
				initFromFiles();
			}
		});
	}

	@Override
	public List<String> getNameList() {
		return nameList;
	}

	@Override
	protected boolean sortNameList(List<String> list) {
		Collections.sort(list);
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
		List<Transformer> tList = Transformers.getByOutType(dataElement.getClass());
		Transformer t = tList == null || tList.isEmpty() ? null : tList.get(0);
		if(t==null)
			throw new UnsupportedOperationException("Can not save element of type " + dataElement.getClass());
		FileDataElement fde = (FileDataElement) t.transformOutput(dataElement);
		
		//TODO: Correct extension for the new File
		Files.move(fde.file.toPath(), getFile(dataElement.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
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
    
    private DataElementDescriptor createDescriptor(File file) {
    	if(file.isDirectory())
    		return new DataElementDescriptor(FileDataCollection.class, false);
    	else
    	{
    		Transformer<FileDataElement, DataElement> transformer = getTransformer(file);
    		Class<? extends DataElement> outputType = transformer == null ? FileDataElement.class : transformer.getOutputType();
    		return new DataElementDescriptor(outputType, true);
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
        FileDataElement fda = new FileDataElement( file.getName(), this, file );
        
        Transformer<FileDataElement, DataElement> transformer = getTransformer(file);
        if(transformer == null)
            return fda;
        transformer.init( null, this );
        return transformer.transformInput( fda );
    }

    private Transformer<FileDataElement, DataElement> getTransformer(File file)
    {
        //TODO: Check yaml first
        return getTransformerBasedOnFile(file);
    }
    
    private Transformer<FileDataElement, DataElement>  getTransformerBasedOnFile(File file)
    {
        String name = file.getName();
        int dotIdx = name.lastIndexOf( '.' );
        String ext = dotIdx == -1 ? "" : name.substring( dotIdx + 1 );
        List<Transformer> tList = Transformers.getByExtension(ext);
		return tList == null || tList.isEmpty() ? null : tList.get(0);
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
	
}
