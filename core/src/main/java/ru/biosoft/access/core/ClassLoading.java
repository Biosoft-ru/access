package ru.biosoft.access.core;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
    public @Nonnull Class<?> loadClass(@Nonnull String className) throws LoggedClassNotFoundException;

    /**
     * Loads {@link Class} with the specified name and necessary plugins.
     */
    public @Nonnull Class<?> loadClass(String className, @CheckForNull String pluginNames) throws LoggedClassNotFoundException;

    public @Nonnull <T> Class<? extends T> loadClass(@Nonnull String className, @Nonnull Class<T> superClass) throws LoggedClassNotFoundException, LoggedClassCastException;

    public @Nonnull <T> Class<? extends T> loadClass(@Nonnull String className, String pluginNames, @Nonnull Class<T> superClass) throws LoggedClassNotFoundException, LoggedClassCastException;
    
    /**
     * Returns absolute resource location by class and location relative to class
     * 
     */
    public @Nonnull String getResourceLocation(Class<?> clazz, String resource);

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
//    public @Nonnull <T> Class<? extends T> loadSubClass(@Nonnull String className, String pluginNames, @Nonnull Class<T> superClass) throws BiosoftNoClassException, LoggedClassCastException;
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
