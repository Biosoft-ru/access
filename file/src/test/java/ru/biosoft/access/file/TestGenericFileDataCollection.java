package ru.biosoft.access.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Environment;

public class TestGenericFileDataCollection
{
    static
    {
        Environment.setClassLoading(new TestClassLoading());
    }

    @Test
    public void test1() throws Exception
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.CLASS_PROPERTY, GenericFileDataCollection.class.getName());
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, "GenericCollection");
        properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, "src/test/resources/GenericFDC");
        DataCollection<DataElement> dc = new GenericFileDataCollection(null, properties);
        assertNotNull(dc);
        System.out.println(dc.getNameList());
        assertEquals(3, dc.getSize());
        for ( DataElement de : dc )
        {
            System.out.println(de.getName() + " " + de.getClass());
        }

    }
}
