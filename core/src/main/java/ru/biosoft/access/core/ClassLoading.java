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

    public String getClassTitle(Class<?> clazz);

    /**
     * Get plugin ID for loaded class
     */
    public default String getPluginForClass(Class<?> clazz)
    {
        return getPluginForClass( clazz.getName() );
    }
    
    /**
     * Get plugin ID for class by given class name
     */
    public String getPluginForClass(String className);

    /**
     * Resolves path to the plugin file resource
     * @param pluginPath path like "ru.biosoft.access:resource"
     * @return File object pointing to the resource
     */
    public default PluginEntry resolvePluginPath(String pluginPath)
    {
        return resolvePluginPath( pluginPath, "" );
    }

    public PluginEntry resolvePluginPath(String pluginPath, String parentPath);

    /**
     * Get class loader for class
     */
    public default ClassLoader getClassLoader(Class<?> clazz)
    {
        return getClassLoader();
    }

    public default ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }

}
