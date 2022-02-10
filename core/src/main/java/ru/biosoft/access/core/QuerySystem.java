package ru.biosoft.access.core;

/**
 * Top level abstraction to query different {@link DataCollection}s using  {@link Index}es.
 *
 * <p>Conventions:
 * <ul>
 *   <li>QuerySystem should has public constructor with {@link DataCollection} argument
 *   This constructor is used by {@link DataCollectionInfo} to create QuerySystem. 
 *   <br>Example:
 *   <pre>public class SequenceQuerySystem implements QuerySystem
 *   {
 *       public SequenceQuerySystem(DataCollection dc)
 *       {
 *           ...
 *       }
 *       ...
 *   }</pre>
 *
 *   <li>All index files used by QuerySystem are registered by {@link DataCollectionInfo}.
 *   Later this information is used to correctly remove all data collection files.
 *
 *   <li>close all indexes when QuerySystem is closed or finalized.
 * </ul>
 */
public interface QuerySystem extends DataCollectionListener
{
    public static final String QUERY_SYSTEM_CLASS = "querySystem";
    public static final String INDEX_BLOCK_SIZE   = "indexBlockSize";

    /** List of index names delimited by semicolon. */
    public static final String INDEX_LIST = "querySystem.indexes";

    /** @return all indexes supported by this QuerySystem. */
    public Index[] getIndexes();

    /** @return index by its name */
    public Index getIndex( String name );

    /**
     * Closes all indexes and releases any system resources associated with them stream.
     * A closed QuerySystem cannot be reopened.
     */
    public void close();
}
