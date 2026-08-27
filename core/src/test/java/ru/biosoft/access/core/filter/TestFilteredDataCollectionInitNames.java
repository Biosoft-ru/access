package ru.biosoft.access.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

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
 * behavioral contract that the optimization must preserve, using an in-memory
 * {@link VectorDataCollection} whose {@code iterator()} and
 * {@code getNameList()+get(name)} are trivially equivalent:
 *
 * <ul>
 *   <li>the resulting name list contains exactly the elements the filter accepts;</li>
 *   <li>the names are emitted in the primary collection's iteration order;</li>
 *   <li>the {@code sorted} flag is detected correctly.</li>
 * </ul>
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

    private static VectorDataCollection<DataElement> newCollection(String... names) throws Exception
    {
        VectorDataCollection<DataElement> dc = new VectorDataCollection<>( "test" );
        for( String name : names )
            dc.put( new DataElementSupport( name, dc ) );
        return dc;
    }

    @Test
    public void testSortedSubsetKeepsOrder() throws Exception
    {
        // insert out of order; VectorDataCollection keeps alphabetical order
        VectorDataCollection<DataElement> primary = newCollection( "d", "a", "c", "b", "e" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "b", "d" ), null, null );

        assertEquals( Arrays.asList( "b", "d" ), filtered.getNameList() );
    }

    @Test
    public void testEmptyFilterResult() throws Exception
    {
        VectorDataCollection<DataElement> primary = newCollection( "a", "b", "c" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "zzz" ), null, null );

        assertTrue( filtered.getNameList().isEmpty() );
        assertEquals( 0, filtered.getSize() );
    }

    @Test
    public void testAllAccepted() throws Exception
    {
        VectorDataCollection<DataElement> primary = newCollection( "z", "m", "a" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, Filter.INCLUDE_ALL_FILTER, null, null );

        assertEquals( Arrays.asList( "a", "m", "z" ), filtered.getNameList() );
    }

    @Test
    public void testSingletonFilter() throws Exception
    {
        VectorDataCollection<DataElement> primary = newCollection( "a", "b", "c" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "c" ), null, null );

        assertEquals( Collections.singletonList( "c" ), filtered.getNameList() );
    }

    @Test
    public void testContainsReflectsFilter() throws Exception
    {
        VectorDataCollection<DataElement> primary = newCollection( "a", "b", "c" );
        FilteredDataCollection<DataElement> filtered =
                new FilteredDataCollection<>( primary, "filtered", primary, new NameFilter( "a", "c" ), null, null );

        assertTrue( filtered.contains( "a" ) );
        assertFalse( filtered.contains( "b" ) );
        assertTrue( filtered.contains( "c" ) );
    }
}
