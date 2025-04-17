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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Implements InfoProvider that stores all information in memory.
 */
public class YamlInfoProvider extends MemoryInfoProvider 
{
	private static final Logger log = Logger.getLogger(YamlInfoProvider.class.getName());
	
	public static final String YAML_FILE = ".info";
	public static final String YANL_TMP_FILE = ".info.tmp";
	
	private File rootFolder;
	private File yamlFile;//optional yml file
    private WatchKey watchKey;
    private FileSystemListener listener;
	
	public YamlInfoProvider(File rootFolder) throws IOException
	{
		this.rootFolder = rootFolder;
		this.yamlFile = new File(rootFolder, YAML_FILE);
		reInitYaml();
		watchFolder();
	}

	@Override
    public synchronized Properties getProperties()
    {
        return super.getProperties();
    }

	@Override
    public synchronized Map<String, Object> getFileInfo(String fileName)
    {
        return super.getFileInfo(fileName);
    }
	
	@Override
	public synchronized void setFileInfo(Map<String, Object> fileInfo) throws Exception {
		super.setFileInfo(fileInfo);
		writeYaml();
		fireInfoChanged();
	}

	@Override
    public synchronized List<String> getDataCollections()
    {
        return super.getDataCollections();
    }
    
    @Override
    public void close() throws Exception {
        FileSystemWatcher.INSTANCE.stopWatching( watchKey, listener );
    }
    
	public static boolean isBioUMLYAML(File file)
	{
		return file.getName().equals(YAML_FILE); 
	}

	private void watchFolder() throws IOException {
        listener = new FileSystemListener()
        {
			
			@Override
			public void added(Path path)throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))
				{
					reInitYaml();
					fireInfoChanged();
					return;
				}
			}
			
		
			@Override
			public void removed(Path path) throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))
				{
					reInitYaml();
					fireInfoChanged();
					return;
				}
			}
			
			@Override
			public void modified(Path path) throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))//TODO: detect what elements were changed and update only them
				{
					reInitYaml();
					fireInfoChanged();
					return;
				}
			}
			
			@Override
			public void overflow(Path dir) throws Exception {
				reInitYaml();
				fireInfoChanged();
			}
        };
        watchKey = FileSystemWatcher.INSTANCE.watchFolder( rootFolder, listener );
	}
	
	private synchronized void reInitYaml() throws IOException 
	{
		fileInfoByName.clear();
		properties.clear();
		collections.clear();
		
		try {
			if (!yamlFile.exists())
				return;
			YamlParser parser = new YamlParser();
			byte[] bytes = Files.readAllBytes(yamlFile.toPath());
			String text = new String(bytes);
			Map<String, Object> yaml = parser.parseYaml(text);
			
			fileInfoByName.clear();
			Object filesObj = yaml.get("files");
			if (filesObj != null) {
				List<Map<String, Object>> files = (List<Map<String, Object>>) filesObj;
				for (Map<String, Object> fileInfo : files) {
					String name = (String) fileInfo.get("name");
					fileInfoByName.put(name, fileInfo);
				}
			}

			// Properties of this collection
			properties.clear();
			Object propsObj = yaml.get("properties");
			if (propsObj instanceof Map) {
				Map<String, String> props = (Map<String, String>) propsObj;
                for ( String propName : props.keySet() )
                {
                    properties.setProperty(propName, String.valueOf(props.get(propName)));
                }
                //properties.putAll(props);
			}
			
			collections.clear();
			Object collectionsObj = yaml.get("collections");
			if(collectionsObj instanceof List)
			{
				collections.addAll((List<String>) collectionsObj);
			}
			
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not init from " + YAML_FILE + ", file will be ignored", e);
			fileInfoByName.clear();
			properties.clear();
			collections.clear();
		}
	}


	private synchronized void writeYaml() throws IOException {
		//recreate yaml object from fileInfoByName, properties and collections
		Map<String, Object> yaml = new LinkedHashMap<>();
		List<Map<String, Object>> files = new ArrayList<>();
		files.addAll(fileInfoByName.values());
		yaml.put("files", files);
		yaml.put("properties", properties);
		yaml.put("collections", collections);
		
		
		//write to .info.tmp
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		Yaml parser = new Yaml(options);
		
		File tmp = new File(rootFolder, YANL_TMP_FILE);
		Writer writer = new BufferedWriter(new FileWriter(tmp));
		parser.dump(yaml, writer);
		writer.close();
		
		//rename .info.tmp into .info atomically
		Files.move(tmp.toPath(), yamlFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

}