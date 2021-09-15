package ru.biosoft.access.core;

import javax.swing.ImageIcon;

public interface IconManager
{
    public ImageIcon getImageIcon(String imagename);
    public ImageIcon getImageIcon(String basePath, String name);
}
