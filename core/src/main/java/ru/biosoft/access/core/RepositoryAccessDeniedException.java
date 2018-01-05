package ru.biosoft.access.core;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * This exception is thrown when access (specified by <code>what</code> parameter) to {@link DataCollection} is denied.
 */
@SuppressWarnings("serial")
public class RepositoryAccessDeniedException extends LoggedException
{
    public static final String KEY_PATH = "path";
    public static final String KEY_USER = "user";
    public static final String KEY_WHAT = "what";
        
    public static final ExceptionDescriptor ED_REPOSITORY_ACCESS = new ExceptionDescriptor("Repository", LoggingLevel.Trace, 
            "$what$ access to $path$ is not allowed for user $user$");
    
    public RepositoryAccessDeniedException(DataElementPath path, String user, String what)
    {
        super(ED_REPOSITORY_ACCESS);
        
        properties.put(KEY_PATH, path);
        properties.put(KEY_USER, user == null ? "anonymous" : user);
        properties.put(KEY_WHAT, what);
    }
}
