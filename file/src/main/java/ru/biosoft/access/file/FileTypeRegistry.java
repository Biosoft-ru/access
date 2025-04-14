package ru.biosoft.access.file;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.stream.Stream;

import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.FileTypePriority;

public class FileTypeRegistry {
    /** Key for class in Environment properties. */
    public static final String FILE_TYPE_REGISTRY_CLASS = "FILE_TYPE_REGISTRY_CLASS";

	protected static final Logger log = Logger.getLogger(FileTypeRegistry.class.getName());
	
    public static final FileType FILE_TYPE_TEXT = new FileType( "Text", new String[] { "", "txt" },
            "ru.biosoft.access.file.FileTextTransformer", FileTypePriority.LOW_PRIORITY, "Text file.");

    public static final FileType FILE_TYPE_BINARY = new FileType( "Binary", new String[] { "" }, null,
            FileTypePriority.LOWEST_PRIORITY, "Binary file.");

    private static FileTypeRegistryImpl registry;

	public static void register(FileType fileType) {
        checkRegistry();
        registry.register( fileType );
	}

	public static FileType getFileType(String name) {
        checkRegistry();
        return registry.getFileType( name );
	}
	
	public static FileType getFileTypeByExtension(String extension) {
        checkRegistry();
        return registry.getFileTypeByExtension( extension );
	}

    public static FileType getFileTypeByTransformer(String transformerClass)
    {
        checkRegistry();
        return registry.getFileTypeByTransformer( transformerClass );
    }

	public static FileType detectFileType(File file) {
        checkRegistry();
        String fileName = file.getName();
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0)
			extension = fileName.substring(i + 1);

		FileType ft = getFileTypeByExtension(extension);
		if (ft != null)
			return ft;

        if( isTextFile(file) )
			return FILE_TYPE_TEXT;

		return FILE_TYPE_BINARY;
	}

    public static boolean isTextFile(File file)
    {
        StringBuilder sb = new StringBuilder("");
        int limit = 255;
        try (Stream<String> lines = Files.lines(file.toPath()))
        {
            lines.takeWhile(s -> {
                return sb.length() < limit;
            }).forEachOrdered(s -> {
                sb.append(s);
            });
        }
        catch (Exception e)
        {
            return false;
        }

        String header = sb.toString();
        double probBinary = header.chars().filter(ch -> !Character.isLetter(ch) && !(ch >= 32 && ch <= 127) && ch != '\n' && ch != '\r' && ch != '\t').count();
        return (probBinary / header.length() < 0.3);
    }

    protected static void checkRegistry()
    {
        if( registry == null )
        {
            if( Environment.getValue( FILE_TYPE_REGISTRY_CLASS ) == null )
            {
                registry = new FileTypeRegistryImpl();
                log.fine( "FileTypeRegistry uses default FileTypeRegistryImpl." );
            }
            else
            {
                try
                {
                    Class<? extends FileTypeRegistryImpl> registryClass = (Class<? extends FileTypeRegistryImpl>) Environment.getValue( FILE_TYPE_REGISTRY_CLASS );
                    registry = registryClass.getDeclaredConstructor().newInstance();
                }
                catch (Throwable t)
                {
                    log.severe( "FileTypeRegistry can not instantiate FileTypeRegistryImpl, class=" + Environment.getValue( FILE_TYPE_REGISTRY_CLASS ) + ", error: " + t );
                }
            }
        }
    }

    public static Stream<FileType> fileTypes()
    {
        checkRegistry();
        return registry.fileTypes();
    }

}
