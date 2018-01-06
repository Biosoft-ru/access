package ru.biosoft.access.core;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;

public class Environment 
{
	////////////////////////////////////////////////////////////////////////////
	// Classes and resources loading
	//
	
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
    
    static public @Nonnull <T> Class<? extends T> loadClass(@Nonnull String className, String pluginNames, @Nonnull Class<T> superClass) throws LoggedClassNotFoundException, LoggedClassCastException
    {
    	return classLoading.loadClass(className, pluginNames, superClass);
    }
    

    /**
     * Returns absolute resource location by class and location relative to class.
     */
    public static @Nonnull String getResourceLocation(Class<?> clazz, String resource)
    {
    	return classLoading.getResourceLocation(clazz, resource);
    }

	////////////////////////////////////////////////////////////////////////////
	// Extension registry
	//

    // TODO - check whether it is used
    
    private static Map<String, Class<? extends DataCollectionListener>> dataCollectionListenersRegistry;
    public static void setDataCollectionListenersRegistry(Map<String, Class<? extends DataCollectionListener>> map)
    {
    	dataCollectionListenersRegistry = map;
    }
    
    public static Class<? extends DataCollectionListener> getListenerClassFromRegistry(String className)
    {
    	if( dataCollectionListenersRegistry != null )
    		return dataCollectionListenersRegistry.get(className);
    	
    	return null;
    }
    
    
    private static Map<String, Class<? extends QuerySystem>> querySystemRegistry;
    public static void setQuerySystemRegistry(Map<String, Class<? extends QuerySystem>> map)
    {
    	querySystemRegistry = map;
    }
    
    public static Class<? extends QuerySystem> getQuerySystemClassFromRegistry(String className)
    {
    	if( querySystemRegistry != null )
    		return querySystemRegistry.get(className);
    	
    	return null;
    }

}
