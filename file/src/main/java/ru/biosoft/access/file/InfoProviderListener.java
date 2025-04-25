package ru.biosoft.access.file;

//TODO: more granular events
public interface InfoProviderListener {
	void infoChanged() throws Exception;

    void infoChanged(Object changed) throws Exception;
}
