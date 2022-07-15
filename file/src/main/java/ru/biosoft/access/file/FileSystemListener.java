package ru.biosoft.access.file;

import java.nio.file.Path;

public interface FileSystemListener {
	void added(Path path);
	void removed(Path path);
	void modified(Path path);
	
	//Indicates that direct childs of dir were added/removed or modified, but events were lost.
	void overflow(Path dir);
}
