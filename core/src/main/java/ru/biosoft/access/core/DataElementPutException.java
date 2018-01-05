package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception is thrown when {@link DataElement} can not be put into {@link DataCollection} specified by {@link DataElementPath}.
 */
@SuppressWarnings("serial")
public class DataElementPutException extends RepositoryException
{
    public static final ExceptionDescriptor ED_CANNOT_PUT = new ExceptionDescriptor("CannotPut",
            LoggingLevel.Summary, "Cannot save element: $path$");

    public DataElementPutException(Throwable t, DataElementPath path)
    {
        super(t, ED_CANNOT_PUT, path);
    }
}
