package ru.biosoft.access.core;


/**
 * Transforms DataElement from one type to another.
 */
public interface Transformer<I extends DataElement, O extends DataElement>
{
    /**
     * Return class of input data element.
     * Input data element stored in primary data collection.
     * 
     * @return Class of input data element.
     */
    Class getInputType();

    /**
     * Return class of output data element.
     * Output data element stored in transformed data collection.
     * 
     * @return Class of output data element.
     */
    Class getOutputType();

    /**
     * Returns is specified class accepted as output type.
     * 
     * @param type Class for checking.
     * @return <code>true</code> - if type is output type, <code>false</code> otherwise.
     */
    boolean isOutputType(Class<?> type);

    /**
     * Returns is specified class accepted as input type.
     * 
     * @param type Class for checking.
     * @return <code>true</code> - if type is input type, <code>false</code> otherwise.
     */
    boolean isInputType(Class<?> type);

    /**
     * Transform input data element to output data element.
     * 
     * @return Transformed data element.
     * @throws Exception If error occurred.
     */
    O transformInput(I input) throws Exception;

    /**
     * Transform output data element to input data element.
     * 
     * @return Primary data element.
     * @throws Exception If error occurred.
     */
    I transformOutput(O output) throws Exception;

    /**
     * Initialize transformer for support optional methods.
     * 
     * @see #getPrimaryCollection()
     * @see #getTransformedCollection()
     */
    void init(DataCollection<I> dcInput, DataCollection<O> dcOutput);

    /**
     * Return data collection which contain input data element.
     * This optional operation. If this operation not supported then null always returns.
     * 
     * @return Data collection which contain input data element or null.
     * @see #init(DataCollection,DataCollection)
     */
    DataCollection<O> getTransformedCollection();

    /**
     * Return data collection which contain output data element.
     * This optional operation. If this operation not supported then null always returns.
     * 
     * @return Data collection which contain output data element or null.
     * @see #init(DataCollection,DataCollection)
     */
    DataCollection<I> getPrimaryCollection();
    
    /**
     * @param input input element
     * @return the confidence level that such input element can be properly transformed by this transformer
     * @see DataElementImporter accept levels for details
     */
    int accept(I input);
}
