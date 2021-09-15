package ru.biosoft.access.core;

import ru.biosoft.exception.InternalException;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Implements DataElement interface by common way.
 */
public class DataElementSupport implements DataElement, Comparable<DataElement>, Cloneable
{
    /**
     *  Constructs data element.
     *
     *  @param name Name of the data element.
     *  @param origin Origin data collection.
     */
    public DataElementSupport(String name, DataCollection<?> origin)
    {
        this.name   = name;
        this.origin = origin;
    }

    /**
     * Returns name of the data element.
     *
     * @todo final specifier needed.
     */
    @Override
    @PropertyName("Name")
    @PropertyDescription("Name of the element")
    public String getName()
    {
        return name;
    }

    /**
     * Returns origin data collection for this data element.
     *
     * @todo final specifier needed.
     */
    @Override
    public DataCollection getOrigin()
    {
        return origin;
    }

    /**
     * @todo LOW Temporary method.
     */
    @Override
    public String toString()
    {
        return "DataElement["+getName()+"] class="+getClass();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private section

    /**
     * Data element name.
     *
     * It is set up in constructor and is declared <code>private</code>
     * to warranty that it can not be changed.
     *
     * @todo Getter for this member is not final, and so may be overridden.
     */
    private String name;

    /**
     * Origin data collection.
     */
    private DataCollection<?> origin;

    @Override
    public int compareTo(DataElement de)
    {
        return getName().compareTo((de).getName());
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * Clones data element overriding its name and origin to the specified values
     * Implementation class must implement {@link CloneableDataElement} interface to signal that clone is supported.
     *
     * @param name new name for the element
     * @param origin new origin for the element
     * @return cloned element
     * @throws CloneNotSupportedException if implementation class doesn't implement CloneableDataElement
     */
    public DataElement clone(DataCollection<?> origin, String name) throws CloneNotSupportedException
    {
        if( !(this instanceof CloneableDataElement) ) throw new CloneNotSupportedException();
        try
        {
            DataElementSupport clone = (DataElementSupport)super.clone();
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
