package ru.biosoft.access.core;

import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Collection which can be used as folder for different elements
 * @author lan
 * @see GenericDataCollection
 * @see GenericDataCollection2
 */
@PropertyName("folder")
@ClassIcon("resources/collection.gif")
public interface FolderCollection extends DataCollection<DataElement>
{
    DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz);
}
