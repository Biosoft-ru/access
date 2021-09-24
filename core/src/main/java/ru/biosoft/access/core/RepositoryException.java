package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

/**
 * Exception which refers to the repository element.
 */
@SuppressWarnings("serial")
public abstract class RepositoryException extends LoggedException
{
    private static String KEY_PATH = "path";
    private static String KEY_TYPE = "type";
    
    public RepositoryException(Throwable cause, ExceptionDescriptor descriptor, DataElementPath path, String type)
    {
        super(ExceptionRegistry.translateException(cause), descriptor);
        
        properties.put(KEY_PATH, path == null ? DataElementPath.EMPTY_PATH : path);

        if( type != null )
        	properties.put(KEY_TYPE, type);
    }

    public RepositoryException(Throwable cause, ExceptionDescriptor descriptor, DataElementPath path)
    {
        this(cause, descriptor, path, null);
    }
    
    public RepositoryException(ExceptionDescriptor descriptor, DataElementPath path)
    {
        this(null, descriptor, path);
    }
    
    protected static String getClassTitle(Class<? extends DataElement> c)
    {
        return Environment.getClassTitle( c );
    }
}
