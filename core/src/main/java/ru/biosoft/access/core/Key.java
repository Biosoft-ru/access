package ru.biosoft.access.core;

/**
 * Key for {@link Index} in {@link QuerySystem}. 
 */
public interface Key
{
    String serializeToString();
    
    boolean accept( Object arg );
    
    String removeKey();
}