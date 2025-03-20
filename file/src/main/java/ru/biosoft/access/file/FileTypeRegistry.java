package ru.biosoft.access.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementImporter;

public class FileTypeRegistry {

	protected static final Logger log = Logger.getLogger(FileTypeRegistry.class.getName());
	
	public static final FileType FILE_TYPE_TEXT = new FileType("text", new String[] { "", "txt" },
			"ru.biosoft.access.file.FileTextTransformer", DataElementImporter.ACCEPT_LOW_PRIORITY, "Text file.");

	public static final FileType FILE_TYPE_BINARY = new FileType("binary", new String[] { "" }, null,
			DataElementImporter.ACCEPT_LOWEST_PRIORITY, "Binary file.");

	protected static Map<String, FileType> byName = new HashMap<>();
	protected static Map<String, FileType> byExtension = new HashMap<>();

	public static void register(FileType fileType) {
		byName.put(fileType.name, fileType);
		
		for (String extension : fileType.getExtensions()) {
			if (!byExtension.containsKey(extension))
				byExtension.put(extension, fileType);
			else {
				FileType ft = byExtension.get(extension);

				if (ft.getPriority() > fileType.getPriority())
					continue;
				else if (ft.getPriority() < fileType.getPriority())
					byExtension.put(extension, fileType);

				else // ft.getPriority() == fileType.getPriority()
					log.warning("FileTypeRegistry: extension '" + extension + "'"
							+ "corresponds to 2 file types with the same priority " + ft.getPriority()
							+ System.lineSeparator() + "FileType (used):    " + ft + System.lineSeparator()
							+ "FileType (skipped): " + fileType);
			}
		}
	}

	public static FileType getFileType(String name) {
		return byName.get(name);
	}
	
	public static FileType getFileTypeByExtension(String extension) {
		return byExtension.get(extension);
	}

	public static FileType detectFileType(File file) {
		String fileName = file.getName();
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0)
			extension = fileName.substring(i + 1);

		FileType ft = getFileTypeByExtension(extension);
		if (ft != null)
			return ft;

		if (true)// todo FileImporter.isTextFile(file) )
			return FILE_TYPE_TEXT;

		return FILE_TYPE_BINARY;
	}

}
