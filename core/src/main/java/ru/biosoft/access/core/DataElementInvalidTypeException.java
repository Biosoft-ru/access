package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception is thrown when {@link DataElement} has invalid type.
 */
@SuppressWarnings("serial")
public class DataElementInvalidTypeException extends RepositoryException
{
    public static final ExceptionDescriptor ED_INVALID_TYPE = new ExceptionDescriptor("InvalidType",
            LoggingLevel.Summary, "Element is not a valid $type$: $path$");

    public DataElementInvalidTypeException(DataElementPath path, String type)
    {
        super(null, ED_INVALID_TYPE, path, type);
    }

    public DataElementInvalidTypeException(DataElementPath path, Class<? extends DataElement> clazz)
    {
        this(path, getClassTitle(clazz));
    }

    public DataElementInvalidTypeException(DataElement de, Class<? extends DataElement> clazz)
    {
        this(DataElementPath.create(de), clazz);
    }
}