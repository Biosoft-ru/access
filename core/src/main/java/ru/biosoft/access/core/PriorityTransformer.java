package ru.biosoft.access.core;

/**
 * Adds getPriority functionality to {@link Transformer} implementations
 */
public interface PriorityTransformer
{
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output);
    public int getOutputPriority(String name);
}
