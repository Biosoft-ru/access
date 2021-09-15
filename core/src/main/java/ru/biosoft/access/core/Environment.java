package ru.biosoft.access.core;

import java.util.Map;

import javax.swing.ImageIcon;

import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.IconUtils;

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

    public static void setClassLoading(ClassLoading newClassLoading)
    {
        classLoading = newClassLoading;
    }

    /**
     * Loads {@link Class} with the specified name.
     * @see ClassLoading
     */
    static public Class<?> loadClass(String className) throws LoggedClassNotFoundException
    {
    	return classLoading.loadClass(className);
    }

    /**
     * Loads {@link Class} with the specified name and necessary plugins.
     * @see ClassLoading
     */
    static public Class<?> loadClass(String className, String pluginNames) throws LoggedClassNotFoundException
    {
    	return classLoading.loadClass(className, pluginNames);
    }

    static public <T> Class<? extends T> loadClass(String className, Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
    	return classLoading.loadClass(className, superClass);
    }
    
    static public <T> Class<? extends T> loadClass(String className, String pluginNames, Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
    	return classLoading.loadClass(className, pluginNames, superClass);
    }
    

    /**
     * Returns absolute resource location by class and location relative to class.
     */
    public static String getResourceLocation(Class<?> clazz, String resource)
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

    ////////////////////////////////////////////////////////////////////////////
    // Functions for icon access
    //

    public static IconManager iconManager;

    public static void setIconManager(IconManager im)
    {
        iconManager = im;
    }
    public static ImageIcon getImageIcon(String path, String name)
    {
        if( iconManager != null )
            return iconManager.getImageIcon( path, name );
        else
            return IconUtils.getImageIcon( path, name );
    }
    public static ImageIcon getImageIcon(String imagename)
    {
        if( iconManager != null )
            return iconManager.getImageIcon( imagename );
        else
            return IconUtils.getImageIcon( imagename );
    }
}
