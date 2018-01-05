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

    /* TODO
    public String getIconId()
    {
        String value = getValue(DataCollection.NODE_IMAGE);
        if( value == null )
        {
            ReferenceType subType = getReferenceType();
            if( subType != null )
                value = subType.getIconId();
        }
        if( value == null )
            value = IconFactory.getClassIconId(getType());
        String customImageLoaderClass = getValue( CustomImageLoader.DATA_COLLECTION_PROPERTY );
        if(customImageLoaderClass != null && value != null)
        {
            int colonIdx = value.indexOf( ':' );
            if(colonIdx > -1)
            {
                String prefix = value.substring( 0, colonIdx );
                String suffix = value.substring( colonIdx + 1 );
                value = prefix + ":" + customImageLoaderClass + "?" + suffix;
            }
            else
            {
                value = customImageLoaderClass + "?" + value;    
            }
        }
        return value;
    }*/
    
}
