package ru.biosoft.access.core;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
    
    private final static Object[][] contents =
    {
        //--- VectorDataCollection constants ----------------------------------/
        { "CN_VECTOR_DC",             "Data collection"},
        { "CD_VECTOR_DC",             "Data collection."},

        { "PN_VECTOR_DC_NAME",        "Name"},
        { "PD_VECTOR_DC_NAME",        "Data collection name."},
        { "PN_VECTOR_DC_SIZE",        "Size"},
        { "PD_VECTOR_DC_SIZE",        "Number of data elements in data collection."},
        { "PN_VECTOR_DC_DESCRIPTION", "Description"},
        { "PD_VECTOR_DC_DESCRIPTION", "Data collection description."},

        //--- LocalRepository constants ---------------------------------------/
        { "CN_LOCAL_REPOSITORY",        "Data collection"},
        { "CD_LOCAL_REPOSITORY",        "Data collection."},

        { "PN_LOCAL_REPOSITORY_ABSOLUTE_PATH",  "Path"},
        { "PD_LOCAL_REPOSITORY_ABSOLUTE_PATH",  "Path to root directory of local repository."},

        //--- TransformedDataCollection constants -----------------------------/
        { "CN_TRANSFORMED_DC",          "Data collection"},
        { "CD_TRANSFORMED_DC",          "Data collection."},

        //--- SqlDataCollection constants ---------------------------------------/
        { "CN_SQL_DC",                  "Data collection (SQL)"},
        { "CD_SQL_DC",                  "Data collection."},

        //--- ImagesDataCollection constants ---------------------------------------/
        { "CN_IMAGES_DC",               "Images Data collection"},
        { "CD_IMAGES_DC",               "Images Data collection."},
        
        //--- FileImporter constants -----------------------------------------------/
        { "PN_FILE_IMPORT_PROPERTIES",  "File import properties"},
        { "PD_FILE_IMPORT_PROPERTIES",  "File import properties"},
        { "PN_PRESERVE_EXTENSION",      "Preserve extension"},
        { "PD_PRESERVE_EXTENSION",      "Preserve file extension"}
    };
}
