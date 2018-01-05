package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception indicates that some data inside {@link DataElement} cannot be read
 * To indicate that the element itself cannot be read use {@link DataElementGetException}.
 */
@SuppressWarnings("serial")
public class DataElementReadException extends RepositoryException
{
    public static final String KEY_OBJECT = "object";

    public static final ExceptionDescriptor ED_CANNOT_READ = new ExceptionDescriptor("CannotRead",
            LoggingLevel.TraceIfNoCause, "Cannot read $object$ from $path$");

    public DataElementReadException(Throwable t, DataElementPath path, Object obj)
    {
        super(t, ED_CANNOT_READ, path);

        if(obj != null)
            properties.put(KEY_OBJECT, obj);
    }

    public DataElementReadException(DataElementPath path, Object obj)
    {
        this(null, path, obj);
    }
    
    public DataElementReadException(Throwable t, DataElement de, Object obj)
    {
        this(t, DataElementPath.create(de), obj);
    }
    
    public DataElementReadException(DataElement de, Object obj)
    {
        this(null, de, obj);
    }
    
    public DataElementReadException(DataElement de)
    {
        this(null, de, "data");
    }

    public DataElementReadException(Throwable t, DataElement de)
    {
        this(t, de, "data");
    }
}