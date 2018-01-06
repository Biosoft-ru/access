package ru.biosoft.access.core;

import ru.biosoft.exception.InternalException;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public abstract class MutableDataElementSupport extends Option implements MutableDataElement, Cloneable
{
    private static final long serialVersionUID = 8426451333625171211L;
    protected String name;
    private DataCollection origin;
    
    public MutableDataElementSupport(DataCollection origin, String name)
    {
        this.name = name;
        this.origin = origin;
        if( origin instanceof Option )
        {
            setParent((Option)origin);
        }
    }

    @Override
    @PropertyName("Name")
    public String getName()
    {
        return name;
    }

    public void setOrigin(DataCollection origin)
    {
        this.origin = origin;
        if( origin instanceof Option )
        {
            setParent((Option)origin);
        }
    }
    @Override
    public DataCollection getOrigin()
    {
        return origin;
    }

    @Override
    public String toString()
    {
        return "DataElement[" + getName() + "] class=" + getClass();
    }


    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    protected DataElement internalClone(DataCollection<?> origin, String name)
    {
        try
        {
            MutableDataElementSupport clone = (MutableDataElementSupport)super.clone();
            clone.name = name;
            clone.origin = origin;
            return clone;
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalException("Clone error");
        }
    }
}
