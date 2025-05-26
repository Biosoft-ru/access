package ru.biosoft.access.file;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileSystemWatcher 
{

    private static final Logger LOG = Logger.getLogger(FileSystemWatcher.class.getName());
    
    public static final FileSystemWatcher INSTANCE = new FileSystemWatcher();
    
    private Map<WatchKey, Watcher> watchers = new HashMap<>();
    private WatchService watchService;

    private FileSystemWatcher()
    {
    	try 
    	{
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
    	 
    	 WatchKey watchKey = folder.toPath().register(watchService, 
                 new WatchEvent.Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW } ); // all possible events

         if( watchers.containsKey( watchKey ) )
             watchers.get( watchKey ).addListener( listener );
         else
         {
             Watcher watcher = new Watcher( watchKey, folder.toPath(), listener );
             watchers.put( watchKey, watcher );
             LOG.info( "Watcher registered, folder: " + watcher.folder + "" );
         }
         return watchKey;
    }
    
    public void stopWatching(WatchKey watchKey, FileSystemListener listener)
    {
        Watcher watcher = watchers.get( watchKey );
        if( watcher != null )
        {
            watcher.removeListener( listener );
            if( watcher.canStop() )
                stopWatching( watchKey );
        }
    }

    public void stopWatching(WatchKey watchKey) 
    {
    	Watcher watcher = watchers.remove(watchKey);
    	watchKey.cancel();

    	if ( watcher != null )
            LOG.info("Watcher unregistered, foder: " + watcher.folder);
    }
    
    private static class Watcher 
    {
		WatchKey key;
    	Path folder;
        List<FileSystemListener> listeners;

    	public Watcher(WatchKey key, Path folder, FileSystemListener listener) 
    	{
			this.key = key;
			this.folder = folder;
            listeners = new ArrayList<>();
            listeners.add( listener );
		}

        public void removeListener(FileSystemListener listener)
        {
            listeners.remove( listener );
        }

        public void addListener(FileSystemListener listener)
        {
            listeners.add( listener );
        }

        public boolean canStop()
        {
            return listeners.size() == 0;
        }
    }

	private void processEvents() 
	{
		while (true) 
		{
			final WatchKey key;
			
			try 
			{
				key = watchService.take(); // wait for a key to be available
			}
			catch (InterruptedException ex) 
			{
				LOG.info("EXIT, interrupted");
				return;
			}

			Watcher watcher = watchers.get(key);
			if (watcher == null) 
			{
				LOG.severe("WatchKey " + key + " not recognized!");
				continue;
			}

            //can not iterate via listeners due to ConcurrentModificationException
            //cache current listeners in array for iteration
            FileSystemListener[] listeners = watcher.listeners.toArray( new FileSystemListener[watcher.listeners.size()] );
            
            for ( WatchEvent<?> event : key.pollEvents() )
            {
                if( event.kind() == OVERFLOW )
                {
                    try
                    {
                        for ( FileSystemListener listener : listeners )
                        {
                            listener.overflow( watcher.folder );
                        }
                    }
                    catch (Exception e)
                    {
                        LOG.log( Level.WARNING, "Error handling file system event for " + watcher.folder, e );
                    }
                    continue;
                }

                Path p = (Path) event.context();
                Path absPath = watcher.folder.resolve( p );
                try
                {
                    if( event.kind() == ENTRY_CREATE )
                        for ( FileSystemListener listener : listeners )
                        {
                            listener.added( absPath );
                        }
                    else if( event.kind() == ENTRY_DELETE )
                        for ( FileSystemListener listener : listeners )
                        {
                            listener.removed( absPath );
                        }
                    else if( event.kind() == ENTRY_MODIFY )
                        for ( FileSystemListener listener : listeners )
                        {
                            listener.modified( absPath );
                        }
                    else
                        LOG.warning( "Unknown event kind: " + event.kind().name() );
                }
                catch (Exception e)
                {
                    LOG.log( Level.WARNING, "Error handling file system event for " + absPath, e );
                }
            }
            key.reset();
        }
	}
}