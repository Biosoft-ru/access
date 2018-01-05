package ru.biosoft.access.core;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception is thrown when {@link DataElement} has invalid type.
 */
@SuppressWarnings("serial")
public class DataElementInvalidSubTypeException extends RepositoryException
{
    public static final ExceptionDescriptor ED_INVALID_SUB_TYPE = new ExceptionDescriptor("InvalidSubType",
            LoggingLevel.Summary, "Collection must contain elements of type $type$: $path$");

    public DataElementInvalidSubTypeException(DataElementPath path, String type)
    {
        super(null, ED_INVALID_SUB_TYPE, path, type);
    }

    public DataElementInvalidSubTypeException(DataElementPath path, Class<? extends DataElement> clazz)
    {
        this(path, getClassTitle(clazz));
    }

    public DataElementInvalidSubTypeException(DataElement de, Class<? extends DataElement> clazz)
    {
        this(DataElementPath.create(de), clazz);
    }
}