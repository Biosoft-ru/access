package ru.biosoft.access.core;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * Interface for storing/extracting indexes.
 * 
 * Map keys to index entries.
 */
public interface Index<T> extends Map<String, T>
{
    String DEFAULT_INDEX_NAME = "id";

    /**
     * Return this index name
     * @return
     */
    String getName();
    
    /**
     * @throws Exception
     */
    Iterator nodeIterator(Key key);

    /**
     * Release all resources.
     * After call this method all operations on this object are invalid.
     */
    void close() throws Exception;
    
    default void flush() throws Exception {}

    /**
     * Check is this index valid.
     * @return <code>true</code> if index valid, <code>false</code> otherwise.
     */
    boolean isValid();

    /**
     * Returns index file. This information is essential to remove index files.
     * Can return null if file is not used.
     *
     * @return index file.
     */
    File getIndexFile();

    ////////////////////////////////////////////////////////////////////////////
    // Inner classes
    //
    
    public static class StringIndexEntry extends IndexEntry
    {
        public StringIndexEntry( String initValue )
        {
            super( 0,initValue.length() );
            this.value = initValue;
        }

        public String value = null;

        @Override
        public int length()
        {
            if( value!=null )
                return value.length()+1;
            return 1;
        }
    }

    /**
     * Store information about entry location.
     */
    public static class IndexEntry
    {
        /**
         * Entry offset in file.
         */
        public long from = 0;

        /**
         * Entry length.
         */
        public long len = 0;

        /**
         * Construct new Index.
         *
         * @param from the entry offset
         * @param len the entry length
         */
        public IndexEntry(long index_from, long index_length)
        {
            from = index_from;
            len  = index_length;
        }

        public int length()
        {
            return 0;
        }
        /**
         * Return string, that describes the index.
         * Format: (from,len)
         * @todo LOW temporary!!!
         */
        @Override
        public String toString()
        {
            return "(" + from + ',' + len + ')';
        }
        
    }// end of IndexEntry
    
}