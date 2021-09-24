package ru.biosoft.access.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DataElement information entity which allows you to get some basic information about element without instantiating it.
 */
public class DataElementDescriptor
{
    private final Map<String, String> properties;
    private final Class<? extends DataElement> elementType;
    private final boolean leaf;
    
    public DataElementDescriptor(Class<? extends DataElement> elementType, boolean leaf, Map<String, String> properties)
    {
        this.elementType = elementType;
        this.leaf = leaf;
        
        if(properties == null || properties.isEmpty())
        {
            this.properties = null;
        }
        else
        {
            if( properties.size() == 1 )    // Save some memory when only one value is stored
            {
                Entry<String, String> entry = properties.entrySet().iterator().next();
                this.properties = Collections.singletonMap(entry.getKey(), entry.getValue());
            }
            else
            {
                this.properties = new HashMap<>(properties);
            }
        }
    }
    
    public DataElementDescriptor(Class<? extends DataElement> elementType, String iconId, boolean leaf)
    {
        this.elementType = elementType;
        this.leaf = leaf;

        if( iconId != null )
        {
            this.properties = Collections.singletonMap(DataCollectionConfigConstants.NODE_IMAGE, iconId);
        }
        else
        {
            this.properties = null;
        }
    }
    
    public DataElementDescriptor(Class<? extends DataElement> elementType, boolean leaf)
    {
        this(elementType, leaf, null);
    }
    
    public DataElementDescriptor(Class<? extends DataElement> elementType)
    {
        this(elementType, true);
    }
    
    public boolean isLeaf()
    {
        return leaf;
    }
    
    public Class<? extends DataElement> getType()
    {
        return elementType;
    }
    
    public String getValue(String key)
    {
        return properties == null ? null : properties.get(key);
    }

    
    public String getIconId()
    {
        return Environment.getDescriptorIconId( this );
    }

    public Map<String, String> cloneProperties()
    {
        return new HashMap<String, String>( properties );
    }

}
