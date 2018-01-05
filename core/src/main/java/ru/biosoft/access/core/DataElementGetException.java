package ru.biosoft.access.core;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception is thrown when {@link DataElement} can not be retrieved from corresponding {@link DataCollection}.
 */
@SuppressWarnings("serial")
public class DataElementGetException extends RepositoryException
{
    public static final ExceptionDescriptor ED_CANNOT_GET = new ExceptionDescriptor("CannotGet",
            LoggingLevel.Summary, "Cannot retrieve $type$: $path$");

    public DataElementGetException(Throwable t, DataElementPath path)
    {
        super(t, ED_CANNOT_GET, path, getClassTitle(DataElement.class));
    }

    public DataElementGetException(Throwable t, DataElementPath path, Class<? extends DataElement> clazz)
    {
        super(t, ED_CANNOT_GET, path, getClassTitle(clazz));
    }
   
}
