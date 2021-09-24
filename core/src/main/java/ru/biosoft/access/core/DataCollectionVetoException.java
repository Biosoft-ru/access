package ru.biosoft.access.core;

/**
 * Exception for canceling data collection operation.
 * Throws by data collection listener.
 * <ul>
 *  <li>{@link AbstractDataCollection#put(DataElement)}</li>
 *  <li>{@link AbstractDataCollection#remove(String)}</li>
 *  </ul>

 * @see DataCollectionEvent
 * @see DataCollectionListener
 */
@SuppressWarnings("serial")
public  class  DataCollectionVetoException extends Exception
{}
