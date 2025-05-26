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
	public static final String YAML_TMP_FILE = ".info.tmp";
	
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
    public void setProperties(Properties newProperties) throws Exception
    {
        if( properties.equals( newProperties ) )
            return;
        super.setProperties( newProperties );
        writeYaml();
        fireInfoChanged();

    }

	@Override
    public synchronized Map<String, Object> getFileInfo(String fileName)
    {
        return super.getFileInfo(fileName);
    }
	
	@Override
	public synchronized void setFileInfo(Map<String, Object> fileInfo) throws Exception {
        ChangedInfo changed = new ChangedInfo();
        changed.allchanged = false;
        changed.modified.add( (String) fileInfo.get( "name" ) );
		super.setFileInfo(fileInfo);
		writeYaml();
        fireInfoChanged( changed );
	}

    @Override
    public void setFileFilter(List<String> filter) throws Exception
    {
        if( !filter.equals( fileFilter ) )
            return;
        super.setFileFilter( filter );
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
                    ChangedInfo changed = reInitYaml();
                    if( changed.allChanged() )
                        fireInfoChanged();
                    else if( changed.elementsChanged() )
                        fireInfoChanged( changed );
					return;
				}
			}
			
		
			@Override
			public void removed(Path path) throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))
				{
                    ChangedInfo changed = reInitYaml();
                    if( changed.allChanged() )
                        fireInfoChanged();
                    else if( changed.elementsChanged() )
                        fireInfoChanged( changed );
					return;
				}
			}
			
			@Override
			public void modified(Path path) throws Exception {
				File file = path.toFile();
				if(isBioUMLYAML(file))//TODO: detect what elements were changed and update only them
				{
                    ChangedInfo changed = reInitYaml();
                    if( changed.allChanged() )
                        fireInfoChanged();
                    else if( changed.elementsChanged() )
                        fireInfoChanged( changed );
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
	
    private synchronized ChangedInfo reInitYaml() throws IOException
	{
        if( !yamlFile.exists() )
        {
            fileInfoByName.clear();
            properties.clear();
            collections.clear();
            fileFilter.clear();
            return new ChangedInfo();
        }

        Map<String, Map<String, Object>> newFileInfoByName = new LinkedHashMap<String, Map<String, Object>>();
        Properties newProperties = new Properties();
        List<String> newCollections = new ArrayList<>();
        List<String> newFileFilter = new ArrayList<>();
		
		try {
			YamlParser parser = new YamlParser();
			byte[] bytes = Files.readAllBytes(yamlFile.toPath());
			String text = new String(bytes);
			Map<String, Object> yaml = parser.parseYaml(text);
			
            //fileInfoByName.clear();
			Object filesObj = yaml.get("files");
			if (filesObj != null) {
				List<Map<String, Object>> files = (List<Map<String, Object>>) filesObj;
				for (Map<String, Object> fileInfo : files) {
					String name = (String) fileInfo.get("name");
                    newFileInfoByName.put( name, fileInfo );
				}
			}

			// Properties of this collection
            //properties.clear();
			Object propsObj = yaml.get("properties");
			if (propsObj instanceof Map) {
				Map<String, String> props = (Map<String, String>) propsObj;
                for ( String propName : props.keySet() )
                {
                    newProperties.setProperty( propName, String.valueOf( props.get( propName ) ) );
                }
                //properties.putAll(props);
			}
			
            //collections.clear();
			Object collectionsObj = yaml.get("collections");
			if(collectionsObj instanceof List)
			{
                newCollections.addAll( (List<String>) collectionsObj );
			}
			
            //fileFilter.clear();
            Object fileFilterObj = yaml.get( "fileFilter" );
            if( fileFilterObj instanceof List )
            {
                newFileFilter.addAll( (List<String>) fileFilterObj );
            }

            //Compare old and new YAML items and reinit only if changed
            //reinit all if properties, collections or fileFilter changed
            //reinit only changed elements if any
            if( collections.equals( newCollections ) && fileFilter.equals( newFileFilter ) )
            {
                if( properties.keySet().stream().allMatch( p -> newProperties.containsKey( p ) && properties.get( p ).equals( newProperties.get( p ) ) )
                        && properties.keySet().containsAll( newProperties.keySet() ) )
                {
                    ChangedInfo ch = getChangedElements( fileInfoByName, newFileInfoByName );
                    fileInfoByName = newFileInfoByName;
                    return ch;
                }
            }
            fileInfoByName = newFileInfoByName;
            properties = newProperties;
            collections = newCollections;
            fileFilter = newFileFilter;
            return new ChangedInfo();

		} catch (Exception e) {
			log.log(Level.WARNING, "Can not init from " + YAML_FILE + ", file will be ignored", e);
			fileInfoByName.clear();
			properties.clear();
			collections.clear();
            return new ChangedInfo();
		}
	}

    private ChangedInfo getChangedElements(Map<String, Map<String, Object>> oldinfo, Map<String, Map<String, Object>> newinfo)
    {
        ChangedInfo changed = new ChangedInfo();
        if( oldinfo.isEmpty() && newinfo.isEmpty() )
            return changed;
        for ( String old : oldinfo.keySet() )
        {
            if( !newinfo.containsKey( old ) )
                changed.deleted.add( old );
            else if( !oldinfo.get( old ).equals( newinfo.get( old ) ) )
                changed.modified.add( old );
        }
        for ( String newel : newinfo.keySet() )
        {
            if( !oldinfo.containsKey( newel ) )
                changed.added.add( newel );
        }
        changed.allchanged = false;
        return changed;
    }

	private synchronized void writeYaml() throws IOException {
		//recreate yaml object from fileInfoByName, properties and collections
		Map<String, Object> yaml = new LinkedHashMap<>();
		List<Map<String, Object>> files = new ArrayList<>();
		files.addAll(fileInfoByName.values());
		yaml.put("files", files);
        if( !properties.isEmpty() )
		yaml.put("properties", properties);
        if( !collections.isEmpty() )
            yaml.put( "collections", collections );
        if( !fileFilter.isEmpty() )
            yaml.put( "fileFilter", fileFilter );
		
		//write to .info.tmp
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		Yaml parser = new Yaml(options);
		
		File tmp = new File(rootFolder, YAML_TMP_FILE);
		Writer writer = new BufferedWriter(new FileWriter(tmp));
		parser.dump(yaml, writer);
		writer.close();
		//rename .info.tmp into .info atomically
		Files.move(tmp.toPath(), yamlFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

	}

}