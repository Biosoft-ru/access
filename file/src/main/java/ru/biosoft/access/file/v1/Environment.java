package ru.biosoft.access.file;

import java.io.File;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;

public abstract class Environment {
	public abstract Class<? extends DataElement> getFileDataElementClass();
	public abstract DataElement createFileDataElement(String name, DataCollection<?> origin, File file);
	public abstract File getFile(DataElement fileDataElement);
	
	public abstract Transformer getTransformerForFile(File file);
	public abstract Transformer getTransformerForDataElement(DataElement de);
	
	public static Environment INSTANCE;//SHOULD BE SET BY Framework (BioUML)
}
