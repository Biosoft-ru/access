package ru.biosoft.access.core;

import java.util.EventListener;

/**
 * Interface for listening data collection events.
 *
 * @see DataCollection
 * @pending high Rename methods like elementWillChange to elementWillBeChanged
 */
public interface DataCollectionListener extends EventListener
{
    /**
     * Called after data element was added.
     * @param e DataCollectionEvent information about added data element.
     * @throws Exception If error occurred.
     */
    void elementAdded(DataCollectionEvent e) throws  Exception;

    /**
     * Called before data element will be added.
     * @param e DataCollectionEvent information about will added data element.
     * @throws DataCollectionVetoException If listener cancel adding of data element.
     * @throws Exception If error occurred.
     */
    void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception;

    /**
     * Called after data element was changed.
     * <code>e</code> contains old data element (which already changed).
     * @param e DataCollectionEvent information about changed data element.
     * @throws Exception If error occurred.
     */
    void elementChanged(DataCollectionEvent e) throws  Exception;

    /**
     * Called before data element will be changed.
     * <code>e</code> contains old data element (which will be changed).
     * @param e DataCollectionEvent information about will change data element.
     * @throws DataCollectionVetoException If listener cancel changing of data element.
     * @throws Exception If error occurred.
     */
    void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception;

    /**
     * Called after data element was removed.
     * @param e DataCollectionEvent information about removed data element.
     * @throws Exception If error occurred.
     */
    void elementRemoved(DataCollectionEvent e) throws  Exception;

    /**
     * Called before data element will be removed.
     * @param e DataCollectionEvent information about will removed data element.
     * @throws DataCollectionVetoException If listener cancel removing of data element.
     * @throws Exception If error occurred.
     */
    void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception;
}

