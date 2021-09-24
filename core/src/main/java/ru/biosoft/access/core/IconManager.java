package ru.biosoft.access.core;

import javax.swing.ImageIcon;

public interface IconManager
{
    public ImageIcon getImageIcon(String imagename);
    public ImageIcon getImageIcon(String basePath, String name);
    public String getClassIconId(Class<?> clazz);
    public String getDescriptorIconId(DataElementDescriptor descr);
    public ImageIcon getIconById(String imagename);
}
