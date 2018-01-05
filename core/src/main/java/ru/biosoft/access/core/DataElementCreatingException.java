package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception indicates access to the {@link DataElement} while it's being created.
 * If you cannot create element, use {@link DataElementCreateException} instead.
 */
@SuppressWarnings("serial")
public class DataElementCreatingException extends RepositoryException
{
    public static final ExceptionDescriptor ED_CREATING = new ExceptionDescriptor("Creating",
            LoggingLevel.Trace, "Trying to access element which is under construction: $path$");

    public DataElementCreatingException(DataElementPath path)
    {
        super(ED_CREATING, path);
    }
}
