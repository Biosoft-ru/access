package ru.biosoft.access.file;

import java.io.File;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;


//TODO: move from biouml to the core
/**
 * FileDataElement is {@link DataElement} which wraps the {@link java.io.File File} object.
 * This class is used for storing of objects that have file representation.
 */
@PropertyName("file")
@ClassIcon("resources/leaf.gif")
public class FileDataElement extends DataElementSupport implements CloneableDataElement
{
    /** File stored in this FileDataElement */
    protected File file;

    public static String validateFileName(String fileName)
    {
    	return fileName;
    }
    
    /**
     * Constructs  FileDataElement with specified name,parent ru.biosoft.access.core.DataCollection,and parent subdirectory
     *
     * @param name name of ru.biosoft.access.core.DataElement
     * @param origin  parent ru.biosoft.access.core.DataCollection
     * @param file File object to wrap
     */
    public FileDataElement(String name, DataCollection<?> origin, File file)
    {
        super(validateFileName(name),origin);
        this.file = file;
    }

    /**
     * Constructs FileDataElement with the specified name and parent {@link FileDataCollectionOld}.
     *
     * @param name   name of DataElement
     * @param origin parent DataCollection
     */
    public FileDataElement(String name, FileDataCollection origin)
    {
        this(name, origin, origin.getFile(name));
    }
    
    /** 
     * Returns file for this DataElement.
     *
     * @return file for this DataElement
     */
    public @Nonnull File getFile()
    {
        return file;
    }

    /** 
     * Returns file length for this DataElement.
     *
     * @return file length for this DataElement
     */
    public long getContentLength()
    {
        return file.length();
    }

    @Override
    public DataElement clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        FileDataElement result = (FileDataElement)super.clone(origin, name);

        // TODO
        // files.copy file
/*        try
        {
        	// ApplicationUtils.linkOrCopyFile(result.file, file, null);
        }
        catch( IOException e )
        {
        	throw new RuntimeException("Cannot copy file " + file + " to " + result.file, e);
        }
*/
        return result;
    }
}
