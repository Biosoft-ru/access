package ru.biosoft.access.core;

import java.beans.PropertyChangeListener;

/**
 * Definition of DataElement that can be changed.
 */
public interface MutableDataElement extends DataElement
{
    public void addPropertyChangeListener(PropertyChangeListener pcl);
    public void removePropertyChangeListener(PropertyChangeListener pcl);
}
