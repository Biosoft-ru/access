package ru.biosoft.access.core;

/**
 * Transform data element of one type to other type.
 * Implement Transformer interface for easy of use in derived classes.
 * 
 * @see TransformedDataCollection
 */
abstract public class AbstractTransformer<I extends DataElement, O extends DataElement> implements Transformer<I, O>
{
    protected final static String lineSep = System.getProperty("line.separator");

    /**
     * Initialize transformer for support optional methods.
     * 
     * @see #getPrimaryCollection()
     * @see #getTransformedCollection()
     */
    @Override
    public void init(DataCollection<I> primaryCollection, DataCollection<O> transformedCollection)
    {
        this.primaryCollection = primaryCollection;
        this.transformedCollection = transformedCollection;
    }

    /**
     * Gets primary data collection connected with transformer.
     * 
     * @return primary data collection.
     */
    @Override
    public DataCollection<I> getPrimaryCollection()
    {
        return primaryCollection;
    }

    /**
     * Gets transformed data collection connected with transformer.
     * 
     * @return  transformed data collection.
     */
    @Override
    public DataCollection<O> getTransformedCollection()
    {
        return transformedCollection;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return getOutputType() == type;
    }

    @Override
    public boolean isInputType(Class<?> type)
    {
        return getInputType() == type;
    }

    @Override
    public int accept(I input)
    {
        return DataElementImporter.ACCEPT_LOW_PRIORITY;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Protected

    /** Data collection of input data elements */
    protected DataCollection<I> primaryCollection;

    /** Data collection of output data elements */
    protected DataCollection<O> transformedCollection;

}
