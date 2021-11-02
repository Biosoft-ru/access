package ru.biosoft.access.core;

import java.util.Map;

import javax.swing.ImageIcon;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
    public static Class<?> loadClass(@Nonnull String className) throws LoggedClassNotFoundException
    {
    	return classLoading.loadClass(className);
    }

    /**
     * Loads {@link Class} with the specified name and necessary plugins.
     * @see ClassLoading
     */
    public static Class<?> loadClass(@Nonnull String className, @CheckForNull String pluginNames) throws LoggedClassNotFoundException
    {
    	return classLoading.loadClass(className, pluginNames);
    }

    public static @Nonnull <T> Class<? extends T> loadClass(@Nonnull String className, @Nonnull Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
    	return classLoading.loadClass(className, superClass);
    }
    
    public static @Nonnull <T> Class<? extends T> loadClass(@Nonnull String className, String pluginNames, @Nonnull Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
    	return classLoading.loadClass(className, pluginNames, superClass);
    }
    
    public static String getClassTitle(Class<? extends DataElement> clazz)
    {
        return classLoading.getClassTitle( clazz );
    }


    /**
     * Returns absolute resource location by class and location relative to class.
     */
    public static @Nonnull String getResourceLocation(Class<?> clazz, String resource)
    {
    	return classLoading.getResourceLocation(clazz, resource);
    }

    /**
     * Get plugin ID for loaded class
     */
    public static String getPluginForClass(Class<?> clazz)
    {
        return classLoading.getPluginForClass( clazz.getName() );
    }

    /**
     * Get plugin ID for class by given class name
     */
    public static String getPluginForClass(String className)
    {
        return classLoading.getPluginForClass( className );
    }

	////////////////////////////////////////////////////////////////////////////
	// Extension registry
	//

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
    public static String getClassIconId(Class<?> clazz)
    {
        if( iconManager != null )
            return iconManager.getClassIconId( clazz );
        else
            return null;
    }
    public static String getDescriptorIconId(DataElementDescriptor descr)
    {
        if( iconManager != null )
            return iconManager.getDescriptorIconId( descr );
        else
            return null;
    }
}
