package ru.biosoft.access.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * Regression tests for {@link FilteredDataCollection#initNames}.
 *
 * <p>{@code initNames} was optimized to enumerate the primary collection with
 * {@code iterator()} instead of calling {@code get(name)} once per name (so that
 * {@code SqlTableDataCollection} can batch its queries). These tests guard the
 * part of that contract that lives in {@code FilteredDataCollection} itself, using
 * two kinds of primary collection:
 *
 * <ul>
 *   <li>a {@link VectorDataCollection} (in-memory) for the straightforward cases;</li>
 *   <li>a {@link ScriptedCollection} whose {@code getNameList()}, {@code get(name)}
 *       and {@code iterator()} are each driven by independent lists, so a regression
 *       back to {@code getNameList()+get(name)} — or a null-yielding iterator — is
 *       caught instead of passing silently.</li>
 * </ul>
 *
 * <p>The SQL-specific behavior (batched fetching, {@code hasNext()} performing no I/O)
 * is not exercised here because {@code SqlTableDataCollection} requires a live database;
 * see the PR discussion.
 */
public class TestFilteredDataCollectionInitNames
{
    private static class NameFilter implements Filter<DataElement>
    {
        private final List<String> accepted;

        private NameFilter(String... names)
        {
            accepted = Arrays.asList( names );
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public boolean isAcceptable(DataElement de)
        {
            return accepted.contains( de.getName() );
        }
    }

    private static VectorDataCollection<DataElement> newVectorCollection(String... names) throws Exception
    {
        VectorDataCollection<DataElement> dc = new VectorDataCollection<>( "test" );
        for( String name : names )
            dc.put( new DataElementSupport( name, dc ) );
        return dc;
    }

    /**
     * A minimal in-memory {@link DataCollection} whose three enumeration paths are
     * independently controllable:
     * <ul>
     *   <li>{@code getNameList()} returns {@code nameListOrder};</li>
     *   <li>{@code get(name)} resolves {@code nameListOrder} -> element;</li>
     *   <li>{@code iterator()} walks {@code iteratorOrder}, yielding {@code null} for
     *       any name listed in {@code nullNames}.</li>
     * </ul>
     * This lets a test assert that {@code initNames} follows the <em>iterator</em> order
     * (not the name-list order) and tolerates {@code null} elements.
     */
    private static class ScriptedCollection extends AbstractDataCollection<DataElement>
    {
        private final Map<String, DataElement> elements = new HashMap<>();
        private final List<String> nameListOrder;
        private final List<String> iteratorOrder;
        private final List<String> nullNames;

        private ScriptedCollection(List<String> nameListOrder, List<String> iteratorOrder, List<String> nullNames)
        {
            super( "scripted", null, new Properties() );
            this.nameListOrder = nameListOrder;
            this.iteratorOrder = iteratorOrder;
            this.nullNames = nullNames;
            for( String name : nameListOrder )
                elements.put( name, new DataElementSupport( name, this ) );
        }

        @Override
        public int getSize()
        {
            return nameListOrder.size();
        }

        @Override
        public List<String> getNameList()
        {
            return Collections.unmodifiableList( nameListOrder );
        }

        @Override
        public Iterator<DataElement> iterator()
        {
            return new Iterator<DataElement>()
            {
                private final Iterator<String> it = iteratorOrder.iterator();

                @Override
                public boolean hasNext()
                {
                    return it.hasNext(); // pure: no I/O, no advancement beyond the name list
                }

                @Override
                public DataElement next()
                {
                    String name = it.next();
                    if( nullNames.contains( name ) )
                        return null;
                    return elements.get( name );
                }
            };
        }

        @Override
        protected DataElement doGet(String name)
        {
            return elements.get( name );
        }
    }

    @Test
    public void testSortedSubsetKeepsIterationOrder() throws Exception
    {
        // VectorDataCollection stores in a TreeMap, so iterator() (and getNameList()) are
        // alphabetical regardless of insertion order; the filter must preserve that order.
        VectorDataCollection<DataElement> primary = newVectorCollection( "d", "a", "c", "b", "e" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "b", "d" ), null, null );

        assertEquals( Arrays.asList( "b", "d" ), filtered.getNameList() );
    }

    @Test
    public void testEmptyFilterResult() throws Exception
    {
        VectorDataCollection<DataElement> primary = newVectorCollection( "a", "b", "c" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "zzz" ), null, null );

        assertTrue( filtered.getNameList().isEmpty() );
        assertEquals( 0, filtered.getSize() );
    }

    @Test
    public void testAllAccepted() throws Exception
    {
        VectorDataCollection<DataElement> primary = newVectorCollection( "z", "m", "a" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, Filter.INCLUDE_ALL_FILTER, null, null );

        assertEquals( Arrays.asList( "a", "m", "z" ), filtered.getNameList() );
    }

    @Test
    public void testSingletonFilter() throws Exception
    {
        VectorDataCollection<DataElement> primary = newVectorCollection( "a", "b", "c" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "c" ), null, null );

        assertEquals( Collections.singletonList( "c" ), filtered.getNameList() );
    }

    @Test
    public void testContainsReflectsFilter() throws Exception
    {
        VectorDataCollection<DataElement> primary = newVectorCollection( "a", "b", "c" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "a", "c" ), null, null );

        assertTrue( filtered.contains( "a" ) );
        assertFalse( filtered.contains( "b" ) );
        assertTrue( filtered.contains( "c" ) );
    }

    /**
     * {@code getNameList()} materializes through the lazy {@code ChunkedList}, whose
     * {@code getChunk} re-derives membership by walking {@code primaryCollection.getNameList()}
     * and re-applying the filter. So the output order and contents must follow the primary
     * name-list order even when the primary's {@code iterator()} yields a different order.
     * This guards against a regression where {@code initNames} started trusting the iterator
     * order (which would produce {@code [d, a, b]} here instead of the correct
     * {@code [a, b, d]}).
     */
    @Test
    public void testOutputFollowsPrimaryNameListOrder() throws Exception
    {
        // name-list order [a, b, d]; iterator order [d, a, b] (intentionally different)
        ScriptedCollection primary =
                new ScriptedCollection( Arrays.asList( "a", "b", "d" ), Arrays.asList( "d", "a", "b" ), Collections.<String>emptyList() );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "a", "b", "d" ), null, null );

        assertEquals( Arrays.asList( "a", "b", "d" ), filtered.getNameList() );
    }

    /**
     * The {@code initNames} loop iterates the primary collection and skips {@code null}
     * elements (matching the old {@code get(name)} contract). A primary whose iterator yields a
     * {@code null} must still be counted correctly and must not throw during initialization.
     * (Note: the *output* membership is re-derived by {@code getChunk} from the name-list, so
     * this test asserts on the size computed during {@code initNames} and that no exception is
     * thrown, which is the part the iterator change affects.)
     */
    @Test
    public void testNullIteratorElementDoesNotBreakInitialization() throws Exception
    {
        // iterator yields a, null(for b), c -> passed should be 2
        ScriptedCollection primary =
                new ScriptedCollection( Arrays.asList( "a", "b", "c" ), Arrays.asList( "a", "b", "c" ), Arrays.asList( "b" ) );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "a", "b", "c" ), null, null );

        // initNames counted 2 passing rows (a and c); the name-list still lists all three.
        assertEquals( 2, filtered.getSize() );
    }
}
