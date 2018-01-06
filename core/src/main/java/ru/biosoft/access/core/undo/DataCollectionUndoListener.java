package ru.biosoft.access.core.undo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.developmentontheedge.beans.undo.PropertyChangeUndo;
import com.developmentontheedge.beans.undo.TransactionListener;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;

/**
 * These class listen all changes inn DataCollection, converts them to UndoableEdit
 * and add them to UndoManager.
 */
public class DataCollectionUndoListener implements PropertyChangeListener, DataCollectionListener
{
    private TransactionListener transactionListener;

    public DataCollectionUndoListener(TransactionListener transactionListener)
    {
        this.transactionListener = transactionListener;
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        transactionListener.addEdit(new PropertyChangeUndo(pce));
    }


    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception { }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        DataElement element = e.getDataElement();
        if (element != null)
        {
            DataCollectionAddUndo undo = new DataCollectionAddUndo(element, e.getOwner());
            transactionListener.addEdit(undo);
        }
    }

    protected DataElement elementToRemove = null;

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        elementToRemove = e.getDataElement();
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if (elementToRemove != null )
        {
            DataCollectionRemoveUndo undo = new DataCollectionRemoveUndo(elementToRemove, e.getOwner());
            transactionListener.addEdit(undo);
        }
    }

    // elementChange event we should get as PropertyChangeEvent
    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        while (e.getPrimaryEvent() != null)
            e = e.getPrimaryEvent();

        if (e.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE)
            elementWillRemove(e);
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        while (e.getPrimaryEvent() != null)
            e = e.getPrimaryEvent();

        if (e.getType() == DataCollectionEvent.ELEMENT_ADDED)
            elementAdded(e);
        if (e.getType() == DataCollectionEvent.ELEMENT_REMOVED)
            elementRemoved(e);
    }

    public TransactionListener getTransactionListener()
    {
        return transactionListener;
    }
}
