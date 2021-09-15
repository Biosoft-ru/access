package ru.biosoft.access.core.filter;

import java.beans.PropertyChangeEvent;
import java.util.stream.Stream;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataElement;

/**
 * TODO comments
 */
@SuppressWarnings("serial")
public class CompositeFilter<T extends DataElement> extends MutableFilter<T>
{
    public CompositeFilter()
    {}

    public CompositeFilter(Option parent)
    {
        super(parent);
    }

    /**
     * To satisfy this filter <code>DataElement</code> should satisfy <b>all</b>
     * all subfilters. <code>null</code> is not accepted.
     */
    @Override
    public boolean isAcceptable(T de)
    {
        if( !isEnabled() )
            return true;
        if(de == null)
            return false;

        return Stream.of( filterList ).allMatch( filter -> filter.isAcceptable( de ) );
    }

    @Override
    protected void firePropertyChange(PropertyChangeEvent evt)
    {
        //ignore changes in disabled filters
        Filter filter = (Filter)evt.getSource();
        if(filter != this && !filter.isEnabled() && !evt.getPropertyName().equals("enabled"))
            return;

        boolean toCheck = evt.getPropertyName().equals("enabled") ||
                          evt.getPropertyName().equals("filterList");
//        if(toCheck && processIsEnabled())
//            return;

        super.firePropertyChange(evt);
    }

    protected boolean processIsEnabled()
    {
        boolean toEnable = false;
        for( Filter<? super T> filter : filterList )
        {
            if(filter.isEnabled())
            {
                toEnable = true;
                break;
            }
        }

        if( toEnable == isEnabled())
            return false;

        setEnabled(toEnable);
        return true;
    }

    ////////////////////////////////////////
    // FilterList properties
    //
    protected Filter<? super T>[]  filterList = new Filter[0];

    public Filter<? super T>[] getFilter()
    {
        return filterList;
    }

    public Filter<? super T> getFilter(int i)
    {
        return filterList[i];
    }

    public void setFilter(Filter<? super T>[] filterList)
    {
        Filter<? super T>[] oldValue = filterList;
        this.filterList = filterList;

        firePropertyChange("filterList", oldValue, filterList);
    }

    public void setFilter(int i, Filter<? super T> filter)
    {
        Filter<? super T> oldValue = filter;
        filterList[i]   = filter;
        if(filter instanceof MutableFilter)
            ((MutableFilter<? super T>)filter).setParent(this);

        firePropertyChange("filterList", oldValue, filter);
    }

    /**
     * Add new filter to the list.
     *
     * If this filter is <code>MutableFilter</code>, then set up itself as a parent.
     */
    public void add(Filter<? super T> filter)
    {
        int length = filterList.length;
        Filter<? super T>[]  newList = new Filter[length + 1];
        System.arraycopy(filterList, 0, newList, 0, length);

        newList[length] = filter;

        if(filter instanceof MutableFilter)
            ((MutableFilter<? super T>)filter).setParent(this);

        setFilter(newList);
    }

    public boolean isExists( Filter<? super T> filter )
    {
        return Stream.of( filterList ).anyMatch( f -> ( filter == null ? f == null : filter.equals( f ) ) );
    }


    /**
     * TODO implement
     */
    public void remove(Filter<? super T> filter)
    {
    }
}


