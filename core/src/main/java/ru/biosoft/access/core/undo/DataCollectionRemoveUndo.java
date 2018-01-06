package ru.biosoft.access.core.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class DataCollectionRemoveUndo extends AbstractUndoableEdit
{
    protected DataElement element = null;
    protected DataCollection parent = null;
    public DataCollectionRemoveUndo(DataElement elementWillRemove, DataCollection parent)
    {
        this.element = elementWillRemove;

        // this trick will fix problem with incorrect parent, bugfix for #1003
        if( elementWillRemove.getOrigin() != null )
            this.parent = elementWillRemove.getOrigin();
        else
            this.parent = parent;
    }

    @Override
    public void undo() throws CannotUndoException
    {
        try
        {
            super.undo();
            //System.out.println("UNDO, parent=" + parent + ", element=" + element);
            parent.put(element);
        }
        catch( Exception e )
        {
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() throws CannotRedoException
    {
        try
        {
            super.redo();
            parent.remove(element.getName());
        }
        catch( Exception e )
        {
            throw new CannotRedoException();
        }
    }

    @Override
    public boolean canUndo()
    {
        return !parent.contains(element);
    }

    @Override
    public boolean canRedo()
    {
        return parent.contains(element);
    }

    @Override
    public String getPresentationName()
    {
        return "Remove " + element.getName() + " from " + parent.getName();
    }

    public DataElement getDataElement()
    {
        return element;
    }
    
    public void setUndone()
    {
        try
        {
            super.undo();
        }
        catch( CannotUndoException e )
        {
        }
    }
}
