package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception indicates error during creation of DataElement.
 */
@SuppressWarnings("serial")
public class DataElementCreateException extends RepositoryException
{
    public static final ExceptionDescriptor ED_CANNOT_CREATE = new ExceptionDescriptor("CannotCreate", LoggingLevel.Summary, "Cannot create $type$ $path$");

    /**
     * @param t - cause
     * @param path - full path to the element which cannot be created
     * @param clazz - class of element you tried to create
     */
    public DataElementCreateException(Throwable t, DataElementPath path, Class<? extends DataElement> clazz)
    {
        super(t, ED_CANNOT_CREATE, path, getClassTitle(clazz));
    }

    public DataElementCreateException(DataElementPath path, Class<? extends DataElement> clazz)
    {
        this(null, path, clazz);
    }

    public DataElementCreateException(DataElementPath path)
    {
        this(null, path, DataElement.class);
    }
}
