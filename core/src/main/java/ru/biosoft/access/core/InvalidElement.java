package ru.biosoft.access.core;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon("resources/invalid.gif")
@PropertyName("invalid element")
public class InvalidElement extends DataElementSupport
{
    private String reason;
    private DataElementGetException exception;
    
    public InvalidElement(String name, DataCollection<? extends DataElement> origin, DataElementGetException exception)
    {
        super( name, origin );
        this.reason = exception.log();
        this.exception = exception;
    }
    
    @PropertyName("Description")
    @PropertyDescription("Reason why element is invalid")
    public String getDescription()
    {
        return reason;
    }
    
    public DataElementGetException getException()
    {
        return exception;
    }
}
