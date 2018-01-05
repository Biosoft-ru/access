package ru.biosoft.access.core;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Minimal interface for storing object in the data collection.
 */
@PropertyName ( "element" )
public interface DataElement
{
    /**
     * Returns a unique name of the data element.
     */
    String getName();

    /**
     * Returns the collection this element belongs to.
     * Since the element can be contained in several collections
     * this should return application-specific <i>main</i> parent collection.
     * 
     * @see DataCollection
     */
    DataCollection<?> getOrigin();

    default DataElementPath getCompletePath()
    {
        return DataElementPath.create(this); 
    }

    @SuppressWarnings ( "unchecked" )
    default @Nonnull <T extends DataElement> T cast(@Nonnull Class<T> clazz)
    {
        if( !clazz.isInstance( this ) )
        {
            if( this instanceof InvalidElement )
            {
                throw ( (InvalidElement)this ).getException();
            }
            throw new DataElementInvalidTypeException(this, clazz);
        }
        
        return (T)this;
    }

    /**
     * @return StreamEx of parents starting from immediate parent, then grandparent and so on
     *
    default StreamEx<DataCollection<?>> parents()
    {
        return StreamEx.<DataCollection<?>> iterate( getOrigin(), DataElement::getOrigin ).takeWhile( Objects::nonNull );
    }
*/
}
