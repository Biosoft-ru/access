package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception indicates that {@link DataElement} specified by {@link DataElementPath} can not be found.
 */
@SuppressWarnings("serial")
public class DataElementNotFoundException extends RepositoryException
{
    public static final ExceptionDescriptor ED_NOT_FOUND = new ExceptionDescriptor("NotFound", LoggingLevel.Summary, "Element not found: $path$");
    
    public DataElementNotFoundException(Throwable t, DataElementPath path)
    {
        super(t, ED_NOT_FOUND, path);
    }
    
    public DataElementNotFoundException(DataElementPath path)
    {
        this(null, path);
    }
}
