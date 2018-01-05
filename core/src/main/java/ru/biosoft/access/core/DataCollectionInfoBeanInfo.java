package ru.biosoft.access.core;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * Draft bean info (it's sometimes loaded from some code).
 */
public class DataCollectionInfoBeanInfo extends BeanInfoEx
{
    public DataCollectionInfoBeanInfo()
    {
        super(DataCollectionInfo.class, MessageBundle.class.getName());
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("displayName", beanClass, "getDisplayName", null));
        add(new PropertyDescriptorEx("description", beanClass, "getDescription", null));
    }
}
