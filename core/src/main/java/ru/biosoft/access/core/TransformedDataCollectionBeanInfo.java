package ru.biosoft.access.core;


/**
 * TODO Document
 */
public class TransformedDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{
    public TransformedDataCollectionBeanInfo()
    {
        super(TransformedDataCollection.class, MessageBundle.class.getName());
        
        initResources(MessageBundle.class.getName());
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_TRANSFORMED_DC") );
        beanDescriptor.setShortDescription( getResourceString("CD_TRANSFORMED_DC") );
    }
}
