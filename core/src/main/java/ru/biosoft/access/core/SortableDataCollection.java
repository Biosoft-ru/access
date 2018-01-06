package ru.biosoft.access.core;

import java.util.Iterator;
import java.util.List;

/**
 * Data collection which supports sorting.
 */
public interface SortableDataCollection<T extends DataElement> extends DataCollection<T>
{
    /**
     * @return false if sorting is not supported
     */
    public boolean isSortingSupported();
    
    /**
     * @return list of field names for which sorting is supported
     */
    public String[] getSortableFields();
    
    /**
     * @param field one of fields previously returned by getSortableFields
     * @param direction sorting direction (true = ascending)
     * @return List of sorted names
     */
    public List<String> getSortedNameList(String field, boolean direction);
    
    /**
     * Returns iterator iterating over elements in specified order
     * @param field one of fields previously returned by getSortableFields
     * @param direction sorting direction (true = ascending)
     * @param from from which element to iterate
     * @param to to which element to iterate
     * @return
     */
    public Iterator<T> getSortedIterator(String field, boolean direction, int from, int to);
}
