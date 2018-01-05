package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception is thrown when {@link DataElement} can not be removed from {@link DataCollection} specified by {@link DataElementPath}.
 */
@SuppressWarnings("serial")
public class DataElementRemoveException extends RepositoryException
{
    public static final ExceptionDescriptor ED_CANNOT_REMOVE = new ExceptionDescriptor("CannotRemove",
            LoggingLevel.Summary, "Cannot remove element: $path$");

    public DataElementRemoveException(Throwable t, DataElementPath path)
    {
        super(t, ED_CANNOT_REMOVE, path);
    }
}
