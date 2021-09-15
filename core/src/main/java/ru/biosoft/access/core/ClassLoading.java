package ru.biosoft.access.core;


import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;

/**
 * Interface for class loading.
 * 
 * <p>It can be quite tricky task when OSGI is used. 
 * Special class OsgiClassLoading is used for this purpose.</p>
 */
public interface ClassLoading
{
    /**
     * Loads {@link Class} with the specified name.
     */
    public Class<?> loadClass(String className) throws LoggedClassNotFoundException;

    /**
     * Loads {@link Class} with the specified name and necessary plugins.
     */
    public Class<?> loadClass(String className, String pluginNames) throws LoggedClassNotFoundException;

    public <T> Class<? extends T> loadClass(String className, Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException;

    public <T> Class<? extends T> loadClass(String className, String pluginNames, Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException;
    
    /**
     * Returns absolute resource location by class and location relative to class
     * 
     */
    public String getResourceLocation(Class<?> clazz, String resource);

    /////////////////////////////////////////////////////////////	
//    /**
//     *  Get ClassLoader for class
//     */
//    public ClassLoader getClassLoader(Class<?> clazz);
//    
//    /**
//     * @return the classLoader suitable to load any class from any plugin (via loadClass(String clazz))
//     */
//    public ClassLoader getClassLoader()
//    {
//        return classLoader;
//    }
//
//
//    /**
//     * Returns URL pointing to the resource. Warning: this URL shouldn't be saved between launches, as it may contain temporary bundle ID
//     */
//    public static URL getResourceURL(Class<?> baseClass, String resource)
//    {
//        URL url = null;
//        try
//        {
//            long id = Platform.getBundle(getPluginForClass(baseClass.getName())).getBundleId();
//            url = new URL("bundleresource", String.valueOf(id), 0, resource.replaceFirst("\\/[^\\/]+$", "/"));
//        }
//        catch( Exception e )
//        {
//        }
//        if( url == null )
//            url = getClassLoader(baseClass).getResource(resource.replaceFirst("\\/[^\\/]+$", "/"));
//        return url;
//    }
//
//    // ////////////////////////////////////////////////////////////////////////
//    // Plugins
//    // 
//    
//    public  <T> Class<? extends T> loadSubClass( String className, String pluginNames,  Class<T> superClass) throws BiosoftNoClassException, LoggedClassCastException;
//    
//
//
//    /**
//     * Get plugin ID for loaded class
//     */
//    public String getPluginForClass(Class<?> clazz)
//    {
//        return getPluginForClass(clazz.getName());
//    }
//    
//    /**
//     * Get plugin ID for class by given class name
//     */
//    public String getPluginForClass(String className)
//
}
