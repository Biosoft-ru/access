package ru.biosoft.access.core.filter;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataElement;

/**
 * <code>MutableFilter</code> is a filter whose selection condition can be changed in runtime.
 *
 * This class provides skeleton for mutable filter with one changeable option <code>enabled</code>.
 */
abstract public class MutableFilter<T extends DataElement> extends Option implements Filter<T>
{
    public MutableFilter()
    {}

    public MutableFilter(Option parent)
    {
        super(parent);
    }

    private boolean enabled = true;
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }
}
