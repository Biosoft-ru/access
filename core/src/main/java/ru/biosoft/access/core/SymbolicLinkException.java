package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * This exception indicates that {@link DataElement} specified by {@link DataElementPath} can not be found in  {@link SymbolicLinkDataCollection}.
 */
@SuppressWarnings("serial")
public class SymbolicLinkException extends RepositoryException
{
    public static String KEY_TARGET = "target";

    public static final ExceptionDescriptor ED_SYMLINK = new ExceptionDescriptor("SymLink", 
    		LoggingLevel.Summary, "Cannot resolve link: $path$ -> $target$");

    public SymbolicLinkException(SymbolicLinkDataCollection sdc)
    {
        super(ED_SYMLINK, sdc.getCompletePath());
        
        properties.put(KEY_TARGET, sdc.getTarget());
    }
}