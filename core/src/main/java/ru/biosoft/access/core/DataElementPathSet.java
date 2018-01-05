package ru.biosoft.access.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.stream.Stream;

import ru.biosoft.exception.LoggedException;

/**
 * Set of DataElementPath's.
 * 
 * Although it can contain different paths, it's best suitable for keeping siblings.
 */
@SuppressWarnings ( "serial" )
public class DataElementPathSet extends TreeSet<DataElementPath>
{
    private DataElementPath defaultPath = DataElementPath.create("data");
    
    public DataElementPathSet()
    {}
    
    /**
     * Constructs from String (for serializing -- the same String as returned by toString())
     */
    public DataElementPathSet(String from)
    {
        if( !from.isEmpty() )
        {
        	Arrays.stream(LoggedException.split(from, ';'))
                  .map( pathStr -> pathStr.indexOf( "/" ) > -1 ? DataElementPath.create( pathStr ) : getPath().getChildPath( pathStr ) )
                  .forEach( this::add );
        }
    }
    
    public DataElementPathSet(Collection<DataElementPath> from)
    {
        addAll( from );
    }
    
    public DataElementPathSet(DataElementPath collection, Iterable<String> names)
    {
        for(String name: names)
        {
            add(collection.getChildPath(name));
        }
    }
    
    public DataElementPathSet(DataElementPath collection, String... names)
    {
        this(collection, Arrays.asList(names));
    }
    
    public String[] getNames()
    {
        return stream().map( DataElementPath::getName ).toArray( String[]::new );
    }
    
    @Override
    public DataElementPath first()
    {
        return isEmpty()?null:iterator().next();
    }
    
    @Override
    public String toString()
    {
        if(isEmpty()) return "";
        Iterator<DataElementPath> iterator = iterator();
        DataElementPath basePath = getPath();
        StringBuilder result = new StringBuilder(iterator.next().toString());
        while(iterator.hasNext())
        {
            DataElementPath path = iterator.next();
            result.append(";");
            result.append(basePath.equals(path.getParentPath())?path.getName():path.toString());
        }
        return result.toString();
    }

    public DataElementPath getPath()
    {
        return isEmpty()?defaultPath:first().getParentPath();
    }

    public void setDefaultPath(DataElementPath defaultPath)
    {
        this.defaultPath = defaultPath;
    }
    
    public <T extends DataElement> Stream<T> elements(Class<T> clazz)
    {
        return stream().map( path -> path.getDataElement(clazz) );
    }

}
