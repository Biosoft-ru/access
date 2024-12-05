package ru.biosoft.access.file;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.nio.file.SensitivityWatchEventModifier;

public class FileSystemWatcher {

    private static final Logger LOG = Logger.getLogger(FileSystemWatcher.class.getName());
    
    public static final FileSystemWatcher INSTANCE = new FileSystemWatcher();
    
    private Map<WatchKey, Watcher> watchers = new HashMap<>();
    private WatchService watchService;

    private FileSystemWatcher()
    {
    	try {
			watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	
        Thread t = new Thread(()->processEvents(), "FileSystemWatcher");
        t.setDaemon(true);
		t.start();
    }
    
    public WatchKey watchFolder(File folder, FileSystemListener listener) throws IOException
    {
    	 if (!folder.exists() || !folder.isDirectory())
             throw new RuntimeException("folder " + folder.getAbsolutePath() + " does not exist or is not a directory");
    	 WatchKey watchKey = folder.toPath().register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW}, SensitivityWatchEventModifier.HIGH);
    	 Watcher watcher = new Watcher(watchKey, folder.toPath(), listener);
    	 watchers.put(watchKey, watcher);
         LOG.info("registered " + watcher.folder + " in watcher service");
         return watchKey;
    	 
    }
    
    public void stopWatching(WatchKey watchKey) {
    	Watcher watcher = watchers.remove(watchKey);
    	watchKey.cancel();
        if ( watcher != null )
            LOG.info("Unregistered " + watcher.folder + " in watcher service");
    }
    
    private static class Watcher {
		WatchKey key;
    	Path folder;
    	FileSystemListener listener;
    	public Watcher(WatchKey key, Path folder, FileSystemListener listener) {
			this.key = key;
			this.folder = folder;
			this.listener = listener;
		}
    }

	private void processEvents() {
		while (true) {
			final WatchKey key;
			try {
				key = watchService.take(); // wait for a key to be available
			} catch (InterruptedException ex) {
				LOG.info("EXIT, interrupted");
				return;
			}

			Watcher watcher = watchers.get(key);
			if (watcher == null) {
				LOG.severe("WatchKey " + key + " not recognized!");
				continue;
			}

			FileSystemListener listener = watcher.listener;
			
			for (WatchEvent<?> event : key.pollEvents()) {
				if(event.kind() == OVERFLOW) {
					try {
						listener.overflow(watcher.folder);
					} catch(Exception e)
					{
						LOG.log(Level.WARNING, "Error handling file system event for " + watcher.folder, e);
					}
					continue;
				} 
				Path p = (Path) event.context();
				Path absPath = watcher.folder.resolve(p);
				try {
					if (event.kind() == ENTRY_CREATE) {
						listener.added(absPath);
					} else if (event.kind() == ENTRY_DELETE) {
						listener.removed(absPath);
					} else if (event.kind() == ENTRY_MODIFY) {
						listener.modified(absPath);
					} else {
						LOG.warning("Unknown event kind: " + event.kind().name());
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Error handling file system event for " + absPath, e);
				}
			}
			
			key.reset();
		}
	}
}