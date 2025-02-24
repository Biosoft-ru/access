package ru.biosoft.access.file.v1;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

/**
 * Validates YAML representation of workflow
 */
public class YamlValidator
{
    private void error(String msg)
    {
    	throw new IllegalArgumentException(msg);
    }

    public void validate(Map<?, ?> rootMap) throws IllegalArgumentException
    {
        Set<String> allowedKeys = new HashSet<>();
        allowedKeys.add( "files" );
        allowedKeys.add( "fileFilter" );
        allowedKeys.add( "recursive" );
        allowedKeys.add( "properties" );
        for( Object key : rootMap.keySet() )
        {
            if( ! ( key instanceof String ) )
                error("Invalid key " + key);
            if( !allowedKeys.contains( key ) )
                error("Unsupported key: " + key);
        }
        
        Object recursive = rootMap.get("recursive");
        if(recursive != null && !(recursive instanceof Boolean))
        	error("Property 'recursive' should be boolean");

        Object fileFilter = rootMap.get( "fileFilter" );
        if( fileFilter != null && ! ( fileFilter instanceof String ) )
            error("Property 'fileFilter' should be string");
        
        Object properties = rootMap.get("properties");
        if(properties != null)
        	validateProperties(properties);
        
        Object files = rootMap.get("files");
        if(files != null)
        	validateFiles(files);
    }

    private void validateFiles(Object files) {
		if(!(files instanceof List))
			error("files shoule be a list");
		List<?> fileList = (List<?>) files;
		for(Object f : fileList)
			validateFile(f);
	}

	private void validateFile(Object obj) {
		if(!(obj instanceof Map))
			error("Element of 'files' should be a map of key-values");
		Map<?,?> m = (Map<?, ?>) obj;
		Set<String> allowedKeys = new HashSet<>();
        allowedKeys.add( "name" );
        allowedKeys.add( "format" );
        allowedKeys.add( "transformer" );
        allowedKeys.add( "properties" );
        for( Object key : m.keySet() )
        {
            if( ! ( key instanceof String ) )
                error("Invalid key " + key);
            if( !allowedKeys.contains( key ) )
                error("Unsupported key: " + key);
        }

        Object name = m.get("name");
        if(name != null && !(name instanceof String))
        	error("Name should be a string");
        
        Object format = m.get("format");
        if(format != null && !(format instanceof String))
        	error("Format should be a string");
        
        Object transformer = m.get("transformer");
        if(transformer != null && !(transformer instanceof String))
        	error("Transformer should be a string");
        
        Object properties = m.get("properties");
        if(properties != null)
        	validateProperties(properties);
        	
	}

	private void validateProperties(Object obj) {
    	if(!(obj instanceof Map))
    		error("Properties should be map of key-values");
    	Map<?,?> properties = (Map<?, ?>) obj;
        for( Map.Entry<?, ?> e : properties.entrySet() )
        {
            if(!(e.getKey() instanceof String ))
                error("Key is not a string: " + e.getKey());
            Object val = e.getValue();
            if(val != null && !(val instanceof String) && !(val instanceof Number) && !(val instanceof Boolean))
            	error("Type of property value should be string, number or boolean");
        }
	}
}
