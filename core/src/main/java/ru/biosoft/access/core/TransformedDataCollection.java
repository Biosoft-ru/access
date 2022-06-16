package ru.biosoft.access.core;

import java.util.Iterator;
import java.util.Properties;

import javax.annotation.Nonnull;

/**
 *
 * Implementation note: we are not propagate DataCollectionEvents, because this work is done
 * by primary DataCollection (it has the same parent).
 *
 * TODO check DataElement type for Transformer
 */
public class TransformedDataCollection<T1 extends DataElement, T2 extends DataElement> extends DerivedDataCollection<T2, T1> implements DataCollectionListener
{
    /**
     * Constructs transformed data collection with parent.
     *
     * @param parent     Parent data collection.
     * @param properties Properties for creating data collection (may be changed).
     *                   <ul>Obligatory properties:
     *                   <li>{@link DataCollectionConfigConstants#NAME_PROPERTY}</li>
     *                   <li>{@link DataCollectionConfigConstants#TRANSFORMER_CLASS}</li>
     *                   <li>{@link DataCollectionConfigConstants#NEXT_CONFIG}</li>
     *
     *                   </ul>
     *                   <ul>Optional properties:
     *                   <li>{@link DataCollectionConfigConstants#NODE_IMAGE}</li>
     *                   <li>{@link DataCollectionConfigConstants#CHILDREN_NODE_IMAGE}</li>
     *                   </ul>
     * @exception Exception any errors
     */
    public TransformedDataCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);

        Class<? extends Transformer<T1, T2>> c = (Class<? extends Transformer<T1, T2>>)getInfo().getPropertyClass(DataCollectionConfigConstants.TRANSFORMER_CLASS, Transformer.class);

        transformer = c.newInstance();
        transformer.init(primaryCollection, this);
        
        try
        {
            inputType = transformer.getInputType();
        }
        catch( Exception e )
        {
            inputType = DataElement.class;
        }
        try
        {
            outputType = transformer.getOutputType();
        }
        catch( Exception e )
        {
            outputType = DataElement.class;
        }

        primaryCollection.addDataCollectionListener(this);
        primaryCollection.setPropagationEnabled(false);
    }

    /**
     * TransformedDataCollection should not throw notification (fire methods)
     * during put() operation. This method makes it.
     *
     * @param de
     * @param element
     * @exception Exception
     * @exception DataCollectionVetoException
     */
    //protected void doAddPreNotify(String name, boolean bNew) throws Exception, DataCollectionVetoException {}
    //protected void doRemovePreNotify(String name) throws Exception, DataCollectionVetoException {}
    /**
     * TransformedDataCollection should not throw notification (fire methods)
     * during put() operation. This method makes it.
     *
     * @param de
     * @param element
     * @exception Exception
     * @exception DataCollectionVetoException
     */
    //protected void doAddPostNotify(String name,boolean bNew) throws Exception , DataCollectionVetoException{}
    //protected void doRemovePostNotify(String name) throws Exception , DataCollectionVetoException{}
    /**
     * Gets class of output data element.
     * @return transformer output element
     */
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return outputType;
    }

    /**
     * Implements specific put action for transformed data collection
     * to put the specified data element into the primary collection.
     *
     * @param element The put DataElement
     * @exception Exception If any errors.
     * @see AbstractDataCollection#put(DataElement)
     */
    @Override
    public void doPut(T2 element, boolean isNew) throws Exception
    {
        T1 tde = transformer.transformOutput((T2)element.cast( getDataElementType() ));
        doGetPrimaryCollection().put(tde);
        synchronized( nameLock )
        {
            sortedNames = null;
        }
    }

    /**
     * Implements specific get action for transformed data collection
     * to get the specified by name data element from the collection.
     * @param name The name of data element
     * @exception Exception If any errors.
     * @see AbstractDataCollection#put(DataElement)
     */
    @Override
    public T2 doGet(String name) throws Exception
    {
        T1 de;
        try
        {
            de = doGetPrimaryCollection().get(name);
        }
        catch( Throwable t )
        {
            throw new DataElementGetException(t, getCompletePath().getChildPath(name), inputType);
        }
        if( de == null )
            return null;
        try
        {
            return transformer.transformInput((T1)de.cast( inputType ));
        }
        catch( Throwable t )
        {
            throw new DataElementGetException(t, getCompletePath().getChildPath(name), outputType);
        }
    }

    @Override
    public boolean isAcceptable(Class clazz)
    {
        return getDataElementType().isAssignableFrom(clazz);
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public @Nonnull Iterator<T2> iterator()
    {
        return new MIterator();
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        if(!isValid())
            return null;
        
        return dataElementDescriptor.get();
    }

    final private class MIterator implements Iterator<T2>
    {
   	    private final Iterator<? extends T1> iterator;
    	    
        private MIterator()
        {
        	iterator = primaryCollection.iterator();
        }

   	    @Override
   	    public boolean hasNext()
   	    {
   	        return iterator.hasNext();
   	    }

   	    @Override
   	    public T2 next()
   	    {
   	        return transform(iterator.next());
   	    }

   	    @Override
   	    public void remove()
   	    {
   	        iterator.remove();
   	    }
    	
        protected T2 transform(T1 de)
        {
            try
            {
                T2 cachedDE = v_cache.get(de.getName());
                if( cachedDE != null )
                    return cachedDE;
                
                cachedDE = transformer.transformInput((T1)de.cast( inputType ));
                cachePut(cachedDE);
                
                return cachedDE;
            }
            catch( Exception exc )
            {
                throw new DataElementGetException(exc, DataElementPath.create(de), transformer.getOutputType());
            }
        }
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        if( e.getPrimaryEvent() != null )
            return;

        fireElementWillAdd(this, e.getDataElementName()); // reference not equal real added object !!!
    }
    @Override
    public void elementAdded(DataCollectionEvent dce) throws Exception
    {
        if( dce.getPrimaryEvent() != null )
            return;

        fireElementAdded(this, dce.getDataElementName());
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        if( e.getPrimaryEvent() != null )
            return;

        fireElementWillChange(this, e.getOwner(), e.getDataElementName(), null);
    }


    @Override
    public void elementChanged(DataCollectionEvent dce) throws Exception
    {
        if( dce.getPrimaryEvent() != null )
            return;

        fireElementChanged(this, dce.getOwner(), dce.getDataElementName(), null, null);
    }


    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        if( e.getPrimaryEvent() != null )
            return;

        fireElementWillRemove(this, e.getDataElementName());
    }

    @Override
    public void elementRemoved(DataCollectionEvent dce) throws Exception
    {
        if( dce.getPrimaryEvent() != null )
            return;

        fireElementRemoved(this, dce.getDataElementName(), null);
    }

    final public Transformer<T1, T2> getTransformer()
    {
        return transformer;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected
    /** @todo Temp implementation */
    @Override
    public String toString()
    {
        return super.toString() + ",transformer = " + transformer;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private
    private Transformer<T1, T2> transformer;
    private Class<? extends DataElement> inputType;
    private Class<? extends DataElement> outputType;
}
