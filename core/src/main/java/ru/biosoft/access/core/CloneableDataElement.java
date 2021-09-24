package ru.biosoft.access.core;

/**
 * Marker interface to specify that element can be cloned (like java.lang.Cloneable).

 * @see DataElementSupport#clone(DataCollection, String)
 */
public interface CloneableDataElement extends DataElement
{
    public DataElement clone(DataCollection<?> newOrigin, String newName) throws CloneNotSupportedException;
}
