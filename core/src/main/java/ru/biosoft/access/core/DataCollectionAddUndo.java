package ru.biosoft.access.core;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class DataCollectionAddUndo extends AbstractUndoableEdit
{
    protected DataElement element = null;
    protected DataCollection parent = null;

    public DataCollectionAddUndo(DataElement elementWillAdd, DataCollection parent)
    {
        this.element = elementWillAdd;

        // this trick will fix problem with incorrect parent, bugfix for #1003
        if( elementWillAdd.getOrigin() != null )
            this.parent = elementWillAdd.getOrigin();
        else
            this.parent = parent;
    }

    @Override
    public void undo() throws CannotUndoException
    {
        try
        {
            super.undo();
            parent.remove(element.getName());
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
            parent.put(element);
        }
        catch( Exception e )
        {
            throw new CannotRedoException();
        }
    }

    @Override
    public boolean canUndo()
    {
        return parent != null && parent.contains(element);
    }

    @Override
    public boolean canRedo()
    {
        return parent != null && !parent.contains(element);
    }

    @Override
    public String getPresentationName()
    {
        return "Add " + element.getName() + " to " + parent.getName();
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
