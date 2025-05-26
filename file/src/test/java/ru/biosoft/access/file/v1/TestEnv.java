package ru.biosoft.access.file.v1;

import java.io.File;
import java.util.Collections;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Transformer;

public class TestEnv extends Environment{
	static {
		INSTANCE = new TestEnv();
	}

	@Override
	public Class<? extends DataElement> getFileDataElementClass() {
		return FDE.class;
	}

	@Override
	public DataElement createFileDataElement(String name, DataCollection<?> origin, File file) {
		return new FDE(name, origin, file);
	}

	@Override
	public File getFile(DataElement fileDataElement) {
		return ((FDE)fileDataElement).getFile();
	}

	@Override
	public Transformer getTransformerForFile(File file) {
		return null;
	}

	@Override
	public Transformer getTransformerForDataElement(DataElement de) {
		return null;
	}

	
	public static class FDE implements DataElement
	{
		private String name;
		private DataCollection<?> origin;
		private File file;
		

		public FDE(String name, DataCollection<?> origin, File file) {
			this.name = name;
			this.origin = origin;
			this.file = file;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public DataCollection<?> getOrigin() {
			return origin;
		}

		public File getFile() {
			return file;
		}
		
	}

    @Override
    public List<Class<? extends Transformer>> getTransformerForClass(Class<? extends DataElement> inputClass, Class<? extends DataElement> outputClass, boolean strict)
    {
        return Collections.emptyList();
    }
}
