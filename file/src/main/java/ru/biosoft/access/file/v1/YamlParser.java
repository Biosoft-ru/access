package ru.biosoft.access.file;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlParser
{
    @SuppressWarnings ( "unchecked" )
    public Map<String, Object> parseYaml(String text)
    {
        Yaml parser = new Yaml();

        Object root = parser.load( text );
        if( ! ( root instanceof Map ) )
            throw new IllegalArgumentException("Yaml should be a map of key-values, but get " + root);

        Map<?, ?> rootMap = (Map<?, ?>)root;
        YamlValidator validator = new YamlValidator();
        validator.validate( rootMap );
        return (Map<String, Object>)rootMap;
    }
}
