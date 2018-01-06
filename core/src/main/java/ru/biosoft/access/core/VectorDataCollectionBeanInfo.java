package ru.biosoft.access.core;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * 
 */
public class VectorDataCollectionBeanInfo extends BeanInfoEx
{
    protected VectorDataCollectionBeanInfo(Class<? extends DataCollection<?>> c, String messageBundle)
    {
        super(c, messageBundle );
    }

    public VectorDataCollectionBeanInfo()
    {
        super(VectorDataCollection.class, MessageBundle.class.getName() );
        
        initResources(MessageBundle.class.getName());
        
        beanDescriptor.setDisplayName(      getResourceString("CN_VECTOR_DC") );
        beanDescriptor.setShortDescription( getResourceString("CD_VECTOR_DC") );
    }

    @Override
    public void initProperties() throws Exception
    {
        // HtmlPropertyInspector.setHtmlGeneratorMethod(beanDescriptor, beanClass.getMethod("getDescription", null));

        add(new PropertyDescriptorEx("name", beanClass, "getName", null),
                getResourceString("PN_VECTOR_DC_NAME"),
                getResourceString("PD_VECTOR_DC_NAME"));

        add(new PropertyDescriptorEx("size", beanClass, "getSize", null),
                getResourceString("PN_VECTOR_DC_SIZE"),
                getResourceString("PD_VECTOR_DC_SIZE"));

        add(new PropertyDescriptorEx("description", beanClass, "getDescription", null),
                getResourceString("PN_VECTOR_DC_DESCRIPTION"),
                getResourceString("PD_VECTOR_DC_DESCRIPTION"));
        
        addHidden(new PropertyDescriptorEx("properties", beanClass, "getDynamicProperties", null));
    }
}
