package ru.biosoft.access.core;

import java.util.EventObject;

/**
 * Implements event change in a collection
 */
@SuppressWarnings ( "serial" )
public class DataCollectionEvent
        extends EventObject
{
    /** Indicates, that new element will be added into collection. */
    public static final int ELEMENT_WILL_ADD = 1;

    /** Indicates, that element will be changed. */
    public static final int ELEMENT_WILL_CHANGE = 2;

    /** Indicates, that element will be removed. */
    public static final int ELEMENT_WILL_REMOVE = 3;

    /** Indicates, that in a collection the new element was added. */
    public static final int ELEMENT_ADDED = 4;

    /** Indicates, that the element of a collection was modified. */
    public static final int ELEMENT_CHANGED = 5;

    /** Indicates, that the element of a collection was removed */
    public static final int ELEMENT_REMOVED = 6;

    private final int type;

    private final String dataElementName;

    private final DataCollection<?> owner;

    private final DataCollectionEvent primaryEvent;
    
    private DataElement oldElement;

    /**
     * Gets the type of event. Possible values:
     * 
     * <ul>
     * <li> {@link #ELEMENT_ADDED}
     * <li> {@link #ELEMENT_CHANGED}
     * <li> {@link #ELEMENT_REMOVED}
     * </ul>
     * 
     * @return type of event
     */
    public int getType ( )
    {
        return type;
    }

    /**
     * Gets a data element, which was added,modified or removed.
     * 
     * @return data element
     */
    public String getDataElementName ( )
    {
        return dataElementName;
    }

    public DataCollection<?> getOwner ( )
    {
        return owner;
    }

    /**
     * @throws Exception
     * @pending high error processing
     */
    public DataElement getDataElement ( ) throws Exception
    {
        if ( owner != null )
            return owner.get ( dataElementName );
        return null;
    }
    
    public DataElementPath getDataElementPath ()
    {
        return DataElementPath.create(owner, dataElementName);
    }

    /**
     * @todo comment
     */
    public DataCollectionEvent getPrimaryEvent ( )
    {
        return primaryEvent;
    }
    
    public DataElement getOldElement()
    {
        return oldElement;
    }

    /**
     * Constructs the DataCollectionEvent object.
     * 
     * @param source
     *            The source which has caused events
     * @param type
     *            <ul>
     *            <li>{@link #ELEMENT_CHANGED}</li>
     *            <li>{@link #ELEMENT_ADDED}</li>
     *            <li>{@link #ELEMENT_REMOVED}</li>
     *            </ul>
     * @param dataElement
     *            changed,removed or added element
     */
    public DataCollectionEvent ( Object source, int type, DataCollection owner, String name, DataCollectionEvent primaryEvent )
    {
        super ( source );
        this.type = type;
        this.owner = owner;
        this.dataElementName = name;
        this.primaryEvent = primaryEvent;
    }
    
    public DataCollectionEvent ( Object source, int type, DataCollection owner, String name, DataElement oldElement, DataCollectionEvent primaryEvent )
    {
        this(source, type, owner, name, primaryEvent);
        this.oldElement = oldElement;
    }
}
