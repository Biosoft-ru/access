package ru.biosoft.access.core;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;

public class Environment 
{
	private static ClassLoading classLoading;
	public static ClassLoading getClassLoading()
	{
		return classLoading;
	}

    /**
     * Loads {@link Class} with the specified name.
     * @see ClassLoading
     */
    static public Class<?> loadClass(@Nonnull String className)  throws LoggedClassNotFoundException
    {
    	return classLoading.loadClass(className);
    }

    /**
     * Loads {@link Class} with the specified name and necessary plugins.
     * @see ClassLoading
     */
    static public Class<?> loadClass(@Nonnull String className, @CheckForNull String pluginNames) throws LoggedClassNotFoundException
    {
    	return classLoading.loadClass(className, pluginNames);
    }

    static public @Nonnull <T> Class<? extends T> loadClass(@Nonnull String className, @Nonnull Class<T> superClass) throws LoggedClassNotFoundException, LoggedClassCastException
    {
    	return classLoading.loadClass(className, superClass);
    }

    /**
     * Returns absolute resource location by class and location relative to class.
     */
    public static @Nonnull String getResourceLocation(Class<?> clazz, String resource)
    {
    	return classLoading.getResourceLocation(clazz, resource);
    }
    
}
