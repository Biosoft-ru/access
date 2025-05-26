package ru.biosoft.access.file;
import ru.biosoft.access.core.ClassLoading;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;

public class TestClassLoading implements ClassLoading
{

    @Override
    public Class<?> loadClass(String className) throws LoggedClassNotFoundException
    {
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw new LoggedClassNotFoundException(e);
        }
    }

    @Override
    public Class<?> loadClass(String className, String pluginNames) throws LoggedClassNotFoundException
    {
        return loadClass(className);
    }

    @Override
    public <T> Class<? extends T> loadClass(String className, Class<T> superClass) throws LoggedClassNotFoundException, LoggedClassCastException
    {
        return (Class<? extends T>) loadClass(className);
    }

    @Override
    public <T> Class<? extends T> loadClass(String className, String pluginNames, Class<T> superClass) throws LoggedClassNotFoundException, LoggedClassCastException
    {
        return loadClass(className, superClass);
    }

    @Override
    public String getResourceLocation(Class<?> clazz, String resource)
    {
        return null;
    }

    @Override
    public String getClassTitle(Class<?> clazz)
    {
        return clazz.getSimpleName();
    }

    @Override
    public String getPluginForClass(Class<?> clazz)
    {
        return null;
    }

    @Override
    public String getPluginForClass(String className)
    {
        return null;
    }
    @Override
    public PluginEntry resolvePluginPath(String pluginPath, String parentPath)
    {
        return null;
    }
}
