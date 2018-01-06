package ru.biosoft.access.core.filter;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * TODO comments
 */
public interface QueryFilter<T extends DataElement> extends Filter<T>
{
    List<String> doQuery( DataCollection<? extends DataElement> dc );
}

