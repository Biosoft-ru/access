package ru.biosoft.access.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.TextDataElement;

public class TestGenericFileDataCollection
{
    static
    {
        Environment.setClassLoading(new TestClassLoading());
    }

    //Instantiate collection
    @Test
    public void testCreateGFDC() throws Exception
    {
        GenericFileDataCollection dc = getCleanCollection();
        assertEquals( "Wrong number of elements", 3, dc.getSize() );
        for ( DataElement de : dc )
        {
            System.out.println( de.getName() + " " + de.getClass() );
        }
        dc.close();
    }

    //Add file to Collection externally
    @Test
    public void testAdd() throws Exception
    {
        GenericFileDataCollection dc = getCleanCollection();
        Path pathInStorage = Paths.get( "src/test/resources/ExtraFiles/file4.txt" );
        Path pathInDC = Paths.get( "src/test/resources/GenericFDC/file4.txt" );
        Files.copy( pathInStorage, pathInDC, StandardCopyOption.REPLACE_EXISTING );
        Thread.sleep( 1000 );
        assertEquals( "Wrong number of elements", 4, dc.getSize() );
        dc.close();
    }

    //Remove file from Collection externally
    @Test
    public void testRemove() throws Exception
    {
        GenericFileDataCollection dc = getCleanCollection();
        Path pathInDC = Paths.get( "src/test/resources/GenericFDC/file2.txt" );
        Files.delete( pathInDC );
        Thread.sleep( 1000 );
        assertEquals( "Wrong number of elements", 2, dc.getSize() );
        dc.close();
    }

    //Change .info file externally
    @Test
    public void testChangeYAML() throws Exception
    {
        GenericFileDataCollection dc = getCleanCollection();
        DataElement de = dc.get( "file1.txt" );
        assertEquals( "Element file1.txt is not Binary before changing .info", FileDataElement.class.getName(), de.getClass().getName() );
        Path pathInStorageT = Paths.get( "src/test/resources/ExtraFiles/info_file1_text" );
        Path pathInDC = Paths.get( "src/test/resources/GenericFDC/.info" );
        Files.copy( pathInStorageT, pathInDC, StandardCopyOption.REPLACE_EXISTING );
        Thread.sleep( 2000 );
        de = dc.get( "file1.txt" );
        assertEquals( "Element file1.txt is not Text after changing .info", TextDataElement.class.getName(), de.getClass().getName() );
        dc.close();
    }

    //Change element properties (should be reflected in .info)
    @Test
    public void testChangeElement() throws Exception
    {
        GenericFileDataCollection dc = getCleanCollection();
        File fileInStorage = new File( "src/test/resources/ExtraFiles/info_file1_text" );
        File fileInDC = new File( "src/test/resources/GenericFDC/.info" );
        assertFalse( ".info is the same as changed one", FileUtils.contentEquals( fileInStorage, fileInDC ) );
        String fileName = "file1.txt";
        DataElement de = dc.get( fileName );
        assertEquals( "Element file1.txt is not Binary before changing file type", FileDataElement.class.getName(), de.getClass().getName() );
        Map<String, Object> fi = dc.getFileInfo( fileName );
        String oldType = (String) fi.get( "type" );
        assertEquals( FileTypeRegistry.FILE_TYPE_BINARY.getName(), oldType );
        fi.put( "type", FileTypeRegistry.FILE_TYPE_TEXT.getName() );
        dc.setFileInfo( fi );
        de = dc.get( "file1.txt" );
        assertEquals( "Element file1.txt is not Text after changing file type", TextDataElement.class.getName(), de.getClass().getName() );
        Thread.sleep( 1000 );
        assertTrue( ".info file not changed after element type changed", FileUtils.contentEquals( fileInStorage, fileInDC ) );
        dc.close();
    }

    //File filter *.log is in original collection, element test.log should be null. After .info without filter is used, element test.log should exist in collection.
    @Test
    public void testFilter() throws Exception
    {
        GenericFileDataCollection dc = getCleanCollection();
        assertEquals( "Element not filtered by path, but should", 3, dc.getSize() );
        String filterElement = "test.log";
        assertNull( "Element not filtered", dc.get( filterElement ) );
        dc.close();

        Path pathInStorageT = Paths.get( "src/test/resources/ExtraFiles/info_nofilter" );
        Path pathInDC = Paths.get( "src/test/resources/GenericFDC/.info" );
        Files.copy( pathInStorageT, pathInDC, StandardCopyOption.REPLACE_EXISTING );
        dc = getCollection();
        assertEquals( "Wrong number of elements", 4, dc.getSize() );
        assertNotNull( "Element missed", dc.get( filterElement ) );
        dc.close();
    }

    private GenericFileDataCollection getCleanCollection() throws IOException
    {
        Path pathInStorage = Paths.get( "src/test/resources/GenericFDCOriginal" );
        Path pathInDC = Paths.get( "src/test/resources/GenericFDC" );
        removeInDir( pathInDC.toFile() );
        FileUtils.copyDirectory( pathInStorage.toFile(), pathInDC.toFile() );
        return getCollection();
    }

    private GenericFileDataCollection getCollection() throws IOException
    {
        Properties properties = new Properties();
        properties.put( DataCollectionConfigConstants.CLASS_PROPERTY, GenericFileDataCollection.class.getName() );
        properties.put( DataCollectionConfigConstants.NAME_PROPERTY, "GenericCollection" );
        properties.put( DataCollectionConfigConstants.FILE_PATH_PROPERTY, "src/test/resources/GenericFDC" );
        GenericFileDataCollection dc = new GenericFileDataCollection( null, properties );
        assertNotNull( dc );
        return dc;
    }

    public static void removeInDir(File dir)
    {
        if( dir == null || !dir.isDirectory() )
            return;
        File[] files = dir.listFiles();
        if( files != null )
        {
            for ( File file : files )
            {
                if( file.isDirectory() )
                {
                    removeInDir( file );
                }
                if( !file.delete() )
                {
                    file.deleteOnExit();
                }
            }
        }
    }
}
