package ru.biosoft.access.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.InternalException;

/**
 */
public abstract class AbstractFileTransformer<O extends DataElement> extends AbstractTransformer<FileDataElement, O>
{
    protected Logger log = Logger.getLogger(AbstractFileTransformer.class.getName());
    
    /**
     * Load an element from the specified file
     * @param input input File object
     * @param name name of the element to create. Currently it's the same as input.getName()
     * @param origin parent collection for newly-created element
     * @return created element (not null)
     * @throws Exception if any problem occurs
     */
    abstract public O load(File input, String name, DataCollection<O> origin) throws Exception;

    /**
     * Saves an element into the specified file
     * @param output output File object
     * @param element element to save
     * @throws Exception if any problem occurs
     */
    abstract public void save(File output, O element) throws Exception;

    @Override
    public Class<? extends FileDataElement> getInputType()
    {
        return FileDataElement.class;
    }

    @Override
    public O transformInput(FileDataElement input) throws Exception
    {        
        O element = load(input.getFile(), input.getName(), getTransformedCollection());
        // TODO: this actually should not happen: load method must never return null
        if(element == null)
            throw new InternalException("Reading error");
        return element;
    }

    @Override
    public FileDataElement transformOutput(O output) throws Exception
    {

        File dir = Files.createTempDirectory( "transform" ).toFile();
        try
        {
            File file = new File(dir, output.getName());
            save(file, output);
            if(!file.exists())
                throw new FileNotFoundException(file.toString());
            FileDataElement fde = new FileDataElement(output.getName(), getPrimaryCollection().cast( FileBasedCollection.class ));
            File result = fde.getFile();
            if( !result.equals( file ) )
            {
                try
                {
                    if( !result.getAbsolutePath().equals( file.getAbsolutePath() ) )
                    {
                        try
                        {
                            result.delete();
                            Files.createLink( result.toPath(), file.toPath() );
                        }
                        catch (Throwable e)
                        {
                            Files.copy( file.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING );
                        }
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException( "Cannot copy file " + file + " to " + result, e );
                }
            }

            return fde;
        }
        finally
        {
            File[] files = dir.listFiles();
            if( files != null )
            {
                for ( File file : files )
                {
                    if( !file.delete() )
                    {
                        file.deleteOnExit();
                    }
                }
            }
            if( !dir.delete() )
            {
                dir.deleteOnExit();
            }
        }

    }
}
