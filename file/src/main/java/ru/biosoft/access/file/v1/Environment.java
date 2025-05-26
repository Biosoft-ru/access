package ru.biosoft.access.file.v1;

import java.io.File;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;

public abstract class Environment {
	public abstract Class<? extends DataElement> getFileDataElementClass();
	public abstract DataElement createFileDataElement(String name, DataCollection<?> origin, File file);
	public abstract File getFile(DataElement fileDataElement);
	
	public abstract Transformer getTransformerForFile(File file);
	public abstract Transformer getTransformerForDataElement(DataElement de);
	
	/**
     * Obtain transformer class for given input/output classes
     * @param inputClass transformer input class (like FileDataElement)
     * @param outputClass transformer output class (like FileTableDataCollection)
     * @param strict flag, if false then also return transformers can transform to child classes of output
     * @return list of class implementing Transformer interface which is able to transform from input to output class or null if no such transformer found.
     */
    public abstract List<Class<? extends Transformer>> getTransformerForClass(Class<? extends DataElement> inputClass, Class<? extends DataElement> outputClass, boolean strict);

	public static Environment INSTANCE;//SHOULD BE SET BY Framework (BioUML)
}
