package ru.biosoft.access.file;

/**
 * Describes file type and corresponding transformer.
 * 
 * File type can have several extensions.
 * For example .fa, .fasta, .ffn for fasta format.
 * 
 * File type should have only one transformer.
 * If there are several versions of file format (for example SBML)
 * the transformer should use reader or writer corresponding to version of the format.   
 *
 * DataElementImporter priorities are used.
 *
 * We use class instead of record because ObjectAid does not support record on UML diagrams.
 */
public class FileType 
{
    protected String[] extensions;
    public String[] getExtensions()     
    { return extensions; }
    
    protected String transformerClassName;
    public String getTransformerClassName() 
    { return transformerClassName; }
    
    protected int priority;
    public int getPriority()
    { return priority; } 

    protected String description;
    public String getDescription()
    { return description; } 
    
    public FileType(String[] extensions, String transformerClassName)
    {
        this(extensions, transformerClassName, 10, null);
    }

    /**
     * @param extensions array of extensions corresponding to this file type
     * @param transformerClassName that converts FileDataElement into other DataElementType.
     * If null then transformation is not applied.
     * @param priority {@link ru.biosoft.access.core.DataElementImporter} priorities are used.
     */
    public FileType(String[] extensions, String transformerClassName,
                    int priority, String description)
    {
        this.extensions  = extensions;
        this.priority    = priority;
        this.description = description;
        this.transformerClassName = transformerClassName;
    }
}
