package ru.biosoft.access.core;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Preferences;
import ru.biosoft.jobcontrol.FunctionJobControl;

public interface DataElementImporter
{
    public static final int ACCEPT_HIGHEST_PRIORITY = 30;
    public static final int ACCEPT_HIGH_PRIORITY = 20;
    public static final int ACCEPT_MEDIUM_PRIORITY = 10;
    public static final int ACCEPT_BELOW_MEDIUM_PRIORITY = 7;
    public static final int ACCEPT_LOW_PRIORITY = 5;
    public static final int ACCEPT_LOWEST_PRIORITY = 1;
    public static final int ACCEPT_UNSUPPORTED = 0;

    public static final String PREFERENCES_IMPORT_DIRECTORY = Preferences.DIALOGS + "/" + "importDialog.importDirectory";
    public static final String PN_PREFERENCES_IMPORT_DIRECTORY = "Default import directory";
    public static final String PD_PREFERENCES_IMPORT_DIRECTORY = "Directory for file chooser to specify file for import.";

    public static final String SUFFIX = "suffix";
    
    /**
     * Returns accept priority if the specified file can be imported into specified DataCollection or ACCEPT_UNSUPPORTED otherwise
     * @param parent - parent DataCollection (or Module) to import data to
     * @param file - file with data that will be imported
     * if file is null then method should check parent only and return true if check passed
     */
    public int accept(DataCollection<?> parent, File file);

    /**
     * Imports element from the specified file into the specified collection.
     * @param parent - parent DataCollection (or Module) to import data to
     * @param file - data file to be imported
     * @param elementName - name of the element to be created. In some rare case
     * @param jobControl - job control
     * @param log - logger the importer (must be not null)
     * @return created DataElement
     */
    public DataElement doImport(@Nonnull DataCollection<?> parent, @Nonnull File file, String elementName, FunctionJobControl jobControl, Logger log) throws Exception;

    /**
     * Initializes importer
     * @param properties - properties from <export> block in plugin.xml
     * @return true if everything is ok and importer can be registered; false otherwise
     */
    public boolean init(Properties properties);

    /**
     * Importer properties
     * @return properties bean or null if properties not defined
     */
    public Object getProperties(DataCollection<?> parent, File file, String elementName);
    
    /**
     * @return type of resulting element which will be created by this importer
     */
    public Class<? extends DataElement> getResultType();
}
