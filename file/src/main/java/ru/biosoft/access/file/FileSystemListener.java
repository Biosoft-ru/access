package ru.biosoft.access.file;

import java.nio.file.Path;

public interface FileSystemListener {
	void added(Path path) throws Exception;
	void removed(Path path) throws Exception;
	void modified(Path path) throws Exception;
	
	//Indicates that direct childs of dir were added/removed or modified, but events were lost.
	void overflow(Path dir) throws Exception;
}
