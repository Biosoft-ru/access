package ru.biosoft.access.core;

import java.io.File;

public interface DataCollectionHelper
{
    public File getChildFile(DataCollection<?> collection, String name);
}
