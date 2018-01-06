package ru.biosoft.access.core;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.MissingParameterException;

/**
 * Represent paths to DataElement in the repository.
 * 
 * <p>This object is read-only, any changes will generate new object.
 * 
 * Note that DataElement represented by path may not exist.
 * To construct the path object use <code>DataElementPath.create</code> method.</p>
 * 
 * <p>If element name contains /, it will be replaced in path with \s
 * <br/>If element name contains \, it will be replaced in path with \\</p>
 * 
 * <p>Use escapeName/unescapeName static methods for these transformations. </p>
 */
public class DataElementPath implements Comparable<DataElementPath>, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final DataElementDescriptor COLLECTION_DESCRIPTOR = new DataElementDescriptor(DataCollection.class, false);

    /** Path delimiter for complete names of data collections. */
    public static final char PATH_SEPARATOR_CHAR = '/';
    public static final String PATH_SEPARATOR = "/";
    
    public static final @Nonnull DataElementPath EMPTY_PATH = new DataElementPath("", null);

    protected final String path;
    
    transient protected String name;
    transient protected DataElementPath parentPath;

    /**
     * Constructs <code>DataElementPath</code> from path string.
     * 
     * <p>Use DataElementPath.create to create paths.</p>
     * 
     * @param path - complete path to <code>DataElement</code>
     * @throws InvalidParameterException if path is incorrect
     */
    private DataElementPath(String path, DataElementPath parentPath)
    {
        this.path = path;
        this.parentPath = parentPath;
        validatePath();
    }
    
    public boolean isEmpty()
    {
        return path.length() == 0;
    }
    
    /**
     * Returns true if element specified by this path actually exists.
     * 
     * <p>Note that this can be faster than <code>getDataElement() != null</code>.</p>
     */
    public boolean exists()
    {
        if( getName().equals("") )
        	return false;
        
        DataCollection<?> parent = optParentCollection();
        if(parent == null)
            return CollectionFactory.getDataElement(path) != null;
        
        return parent.contains(getName());
    }

    /**
     * Returns itself of not exists. Otherwise generates new path in the same collection which not exists.
     * It is useful to generate path to result without overwriting already existing data.
     */
    public @Nonnull DataElementPath uniq() throws RepositoryException
    {
        if(!exists()) return this;
        DataCollection<?> parent = getParentCollection();
        String baseName = getName();
        int i=1;
        String name;
        do
        {
            name = baseName + " (" + ( i++ ) + ")";
        }
        while( parent.contains(name) );
        return getSiblingPath(name);
    }

    /**
     * Tests whether passed DataElementPath is an ancestor for current one.
     * 
     * @param ancestor DataElementPath to test
     * @return true if ancestor is actually an ancestor or elements are equal; false otherwise
     */
    public boolean isDescendantOf(@Nonnull DataElementPath ancestor)
    {
        if(ancestor.equals( EMPTY_PATH ))
            return true;

        String[] fields1 = getPathComponents();
        String[] fields2 = ancestor.getPathComponents();
        if(fields2.length > fields1.length) 
        	return false;
        
        for(int i=0; i<fields2.length; i++)
        {
            if( !fields1[i].equals(fields2[i]) ) 
            	return false;
        }
        
        return true;
    }

    /**
     * Tests whether passed DataElementPath is a child for current one (not necessarily immediate child).
     * 
     * @param descendant DataElementPath to test
     * @return true if descendant is actually a descendant or elements are equal; false otherwise
     */
    public boolean isAncestorOf(@Nonnull DataElementPath descendant)
    {
        return descendant.isDescendantOf(this);
    }

    /**
     * Tests whether supplied path is the sibling to current one.
     * 
     * @param sibling - path to test
     * @return true if sibling is sibling for current path or equals to current path; false otherwise
     */
    public boolean isSibling(@Nonnull DataElementPath sibling)
    {
        String[] fields1 = getPathComponents();
        String[] fields2 = sibling.getPathComponents();
        if(fields1.length != fields2.length) 
        	return false;
        
        for(int i=0; i<fields1.length-1; i++)
        {
            if( !fields1[i].equals(fields2[i]) ) 
            	return false;
        }
        
        return true;
    }
    
    /**
     * Creates relative path string so that ancestor.getRelativePath(path.getPathDifference(ancestor)) equals to path.
     * 
     * @param ancestor path to some ancestor element
     * @return relative path string
     */
    public @Nonnull String getPathDifference(@Nonnull DataElementPath ancestor)
    {
        String[] myComponents = getPathComponents();
        int depth = ancestor.getDepth();
        StringBuilder result = new StringBuilder();
        for(int i = depth; i<myComponents.length; i++)
        {
            if(result.length() > 0) result.append(PATH_SEPARATOR);
            result.append(escapeName(myComponents[i]));
        }
        
        return result.toString();
    }
    
    public @Nonnull DataElementPath getCommonPrefix(@Nonnull DataElementPath other)
    {
        if(this.equals( EMPTY_PATH ) || other.equals( EMPTY_PATH ) || this.equals( other ))
            return this;
        
        String[] myComponents = getPathComponents();
        String[] otherComponents = other.getPathComponents();
        DataElementPath result = EMPTY_PATH;
        for(int i=0; i<Math.min( myComponents.length, otherComponents.length ); i++)
        {
            if(!myComponents[i].equals( otherComponents[i] ))
                break;
            result = result.getChildPath( myComponents[i] );
        }
        
        return result;
    }

    /**
     * Converts path relative to current to absolute path and returns it. 
     * Handy replacement for series of getChildPath/getSiblingPath/getParentPath.
     * 
     * @param relativePath - relative path. May contain ".." to go up or "." to stay.
     * @return created path
     */
    @Nonnull
    public DataElementPath getRelativePath(String relativePath)
    {
        if(relativePath.isEmpty())
            return this;
        
        String[] elements = relativePath.split(PATH_SEPARATOR);
        DataElementPath path = this;
        for(String element: elements)
        {
            if(element.equals("."))
                continue;
            if(element.equals(".."))
            {
                path = path.getParentPath();
                continue;
            }
            path = path.getChildPath(unescapeName(element));
        }

        return path;
    }
    
    /**
     * @return DataElementDescriptor provided by parent collection for current path.
     */
    public @CheckForNull DataElementDescriptor getDescriptor()
    {
        DataElementPath parent = getParentPath();
        if(parent.isEmpty())
        {
            return COLLECTION_DESCRIPTOR;
        }
        
        DataCollection<? extends DataElement> parentCollection = parent.optDataCollection();
        return parentCollection == null ? null : parentCollection.getDescriptor(getName());
    }

    /**
     * Fetches DataElement from repository.
     * 
     * @return fetched DataElement or null if it doesn't exist or some error occurs
     * @TODO rename method
     */
    public @CheckForNull DataElement optDataElement()
    {
        try
        {
            return getName().equals("")?null:CollectionFactory.getDataElement(path);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public @CheckForNull <T extends DataElement> T optDataElement(@Nonnull Class<T> clazz)
    {
        if(getName().equals( "" ))
            return null;
        
        try
        {
            return getDataElement( clazz );
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }
    
    /**
     * Fetches DataElement from repository.
     * 
     * @return fetched DataElement
     * @throws RepositoryException if element cannot be fetched
     */
    public @Nonnull DataElement getDataElement() throws RepositoryException
    {
        return getDataElement(DataElement.class);
    }
    
    /**
     * Fetches DataElement from repository.
     * 
     * @param clazz wanted element class
     * @return fetched DataElement
     * @throws RepositoryException if element cannot be fetched or has invalid class
     */
    public @Nonnull <T extends DataElement> T getDataElement(@Nonnull Class<T> clazz) throws RepositoryException
    {
        DataElement de;
        try
        {
            de = CollectionFactory.getDataElementChecked(path, false);
        }
        catch( RepositoryException e )
        {
            if(e.getProperty("path").equals(this))
                throw e;
            throw new DataElementGetException(e, this);
        }
       
        return de.cast(clazz);
    }
    
    /**
     * Fetches DataCollection from repository.
     * 
     * @return fetched DataCollection or null if it doesn't exist, not a DataCollection or some error occurs.
     */
    public @CheckForNull DataCollection<?> optDataCollection()
    {
        return optDataElement(DataCollection.class);
    }

    /**
     * Fetches DataCollection from repository.
     * 
     * @return fetched DataCollection or null if it doesn't exist, not a DataCollection or some error occurs.
     */
    public @CheckForNull <T extends DataElement> DataCollection<T> optDataCollection(Class<T> clazz) throws RepositoryException
    {
        try
        {
            return getDataCollection( clazz );
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }
    /**
     * Fetches DataCollection from repository.
     * 
     * @return fetched DataCollection
     * @throws RepositoryException if element cannot be fetched
     */
    public @Nonnull DataCollection<DataElement> getDataCollection() throws RepositoryException
    {
        return getDataCollection(DataElement.class);
    }

    /**
     * Fetches DataCollection from repository.
     * 
     * @param clazz class of DataCollection element (not the class of the DataCollection itself!)
     * note that all elements in the collection must be instance of specified class
     * @return fetched DataCollection
     * @throws RepositoryException if element cannot be fetched or is not a collection or doesn't contain specified elements
     */
    public @Nonnull <T extends DataElement> DataCollection<T> getDataCollection(Class<T> clazz) throws RepositoryException
    {
        @SuppressWarnings("unchecked")
		DataCollection<T> dc = getDataElement(DataCollection.class);
       
        if(!clazz.isAssignableFrom(dc.getDataElementType()))
            throw new DataElementInvalidSubTypeException(dc, clazz);
        
        return dc;
    }

    /**
     * Fetches parent DataCollection for current path.
     * 
     * Note that current element may not exist, but it's parent should.
     * 
     * @return DataCollection or null if it doesn't exist or some other error occurs
     */
    public @CheckForNull DataCollection<?> optParentCollection()
    {
        return getParentPath().optDataCollection();
    }

    public @Nonnull DataCollection<DataElement> getParentCollection() throws RepositoryException
    {
        return getParentPath().getDataCollection(DataElement.class);
    }

    /**
     * Fetches parent DataCollection for current path checking that it contains the elements of specified type.
     * Note that current element may not exist, but it's parent should.
     * 
     * @return DataCollection
     * @throws RepositoryException if collection not found, cannot be fetched or has invalid type
     */
    public @Nonnull <T extends DataElement> DataCollection<T> getParentCollection(Class<T> clazz) throws RepositoryException
    {
        return getParentPath().getDataCollection(clazz);
    }

    /**
     * Converts path to target path following any symLinks on the way.
     * 
     * @return target DataElementPath
     */
    public DataElementPath getTargetPath()
    {
        if(isEmpty()) return this;
        DataElement targetElement = getTargetElement();
        return targetElement == null ? getParentPath().getTargetPath().getChildPath(getName()) : DataElementPath.create(targetElement);
    }

    /**
     * Converts path to target element following any symLinks on the way.
     * 
     * @return target DataElement
     */
    public DataElement getTargetElement()
    {
        return getName().equals("")?null:CollectionFactory.getDataElement(path, true);
    }

    /**
     * Creates path for child item to current path.
     * 
     * @param name - name of child item (may not exist). Null name is considered as empty name
     * @return created DataElementPath
     */
    public @Nonnull DataElementPath getChildPath(String... names)
    {
        DataElementPath result = this;
        for(String name: names)
        {
            if( name == null )
                name = "";
       
            if( result.path.isEmpty() )
            {
                if( name.isEmpty() )
                    result = EMPTY_PATH;
                
                result = new DataElementPath(escapeName(name), EMPTY_PATH);
            } 
            else
                result = new DataElementPath(result.path + PATH_SEPARATOR + escapeName(name), result);
        }
        
        return result;
    }
    
    /**
     * @return array of Strings containing path components ("data/Example/element" -> {"data", "Example", "element"})
     */
    public @Nonnull String[] getPathComponents()
    {
        if(equals(EMPTY_PATH)) 
        	return new String[0];
        
        String[] result = LoggedException.split(path, PATH_SEPARATOR_CHAR);
        for(int i=0; i<result.length; i++)
        {
        	result[i] = unescapeName(result[i]);
        }
        
        return result;
    }
    
    /**
     * @return number of path components
     */
    public int getDepth()
    {
        if(equals(EMPTY_PATH)) 
        	return 0;
        
        int depth = 1;
        for(int i=0; i<path.length(); i++)
        {
            if(path.charAt(i) == '/') depth++;
        }
        
        return depth;
    }

    /**
     * Returns @link{DataElementPathSet} object which contains all the children of current object.
     * 
     * <p>Warning: it's alphabetically sorted, not collection-specific sorted.</p>
     */
    public @Nonnull DataElementPathSet getChildren() throws RepositoryException
    {
        DataElementPathSet result = getDataCollection().names().map( this::getChildPath ).
        		collect( Collectors.toCollection(DataElementPathSet::new));
        
        result.setDefaultPath(this);
        
        return result;
    }
    
    /**
     * Returns array which contains all the children of current object or empty array in case of any errors.
     * 
     * <p>Warning: it's alphabetically sorted, not collection-specific sorted.</p>
     */
    public @Nonnull DataElementPath[] getChildrenArray() throws RepositoryException
    {
        DataElementPathSet children = getChildren();
        return children.toArray(new DataElementPath[children.size()]);
    }

    /**
     * Creates path for sibling item to current path.
     * 
     * @param name - name of sibling item (may not exist)
     * @return created DataElementPath
     */
    public @Nonnull DataElementPath getSiblingPath(String name)
    {
        return getParentPath().getChildPath(name);
    }

    /**
     * Creates path for parent item to current path.
     * 
     * <p>Note that neither current path nor parent path should actually exist.</p>
     * 
     * @return created DataElementPath
     */
    public @Nonnull DataElementPath getParentPath()
    {
        DataElementPath _parentPath = parentPath;
        if(_parentPath == null)
        {
            int pos = path.lastIndexOf(PATH_SEPARATOR);
            _parentPath = pos <= -1 ? EMPTY_PATH : new DataElementPath(path.substring(0, pos), null);
            parentPath = _parentPath;
        }
        
        return _parentPath;
    }
    
    /**
     * Returns string representation of the path.
     */
    @Override
    public String toString()
    {
        return path;
    }

    /**
     * Validates path.
     */
    protected void validatePath()
    {
        // TODO: check whether path is valid
    }

    @Override
    public int hashCode()
    {
        return ( path == null ) ? 0 : path.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        DataElementPath other = (DataElementPath)obj;
        if( path == null )
        {
            if( other.path != null )
                return false;
        }
        else if( !path.equals(other.path) )
            return false;
        return true;
    }

    /**
     * Returns last name component of current path
     */
    public @Nonnull String getName()
    {
        if(name == null)
        {
            int pos = path.lastIndexOf(PATH_SEPARATOR);
            if(pos != -1)
                name = unescapeName(path.substring(pos+1));
            else
                name = unescapeName(path);
        }
        
        return name;
    }
    
    @Override
    public int compareTo(DataElementPath elem)
    {
        return path.compareTo(elem.path);
    }

    /**
     * Removes element to which points current DataElementPath.
     * 
     * Does nothing if element doesn't exists (you can call .exists() first to check).
     * 
     * @throws DataElementRemoveException if element exists and cannot be removed for some reason (error during removal)
     */
    public void remove()
    {
        DataCollection<?> parent = optParentCollection();
        if(parent != null && parent.contains(getName()))
        {
            try
            {
                parent.remove(getName());
            }
            catch( DataElementRemoveException e )
            {
                throw e;
            }
            catch( Exception e )
            {
                throw new DataElementRemoveException( e, this );
            }
        }
    }
    
    /**
     * Saves given element under this path.
     * 
     * @param de element to save. Element name must be equal to the path getName()!
     * @throws DataElementPutException if save failed
     */
    @SuppressWarnings ( "unchecked" )
    public void save(DataElement de) throws DataElementPutException
    {
        try
        {
            if(!de.getName().equals(getName()))
                throw new Exception("Element name is wrong");
            DataElementPath parent = this.getParentPath();
       
            if(parent.isEmpty())
                throw new MissingParameterException("Parent path");
            
            parent.getDataElement(DataCollection.class).put(de);
        }
        catch( DataElementPutException e )
        {
            throw e;
        }
        catch( Throwable t )
        {
            throw new DataElementPutException(t, this);
        }
    }
    
    /**
     * Create and return DataElementPath if argument is not null (otherwise return null)
     * TODO: cache
     */
    public static DataElementPath create(@CheckForNull String path)
    {
        if(path == null) 
        	return null;
        
        if(path.isEmpty()) 
        	return EMPTY_PATH;
        
        return new DataElementPath(path, null);
    }
    
    /**
     * The same as create. Special for TextUtil.fromString
     */
    public static DataElementPath createInstance(String path)
    {
        return create(path);
    }
    
    /**
     * Returns DataElementPath constructed by existing DataElement if argument is not null (otherwise return null).
     * 
     * @param element - element to construct from
     */
    public static DataElementPath create(@CheckForNull DataElement element)
    {
        if(element == null)
            return null;
        if(element instanceof DataCollection)
        {
            return ((DataCollection<?>)element).getCompletePath();
        }
        if(element.getName() == null)
            throw new InvalidParameterException("Cannot obtain element path: element has no name");
        if(element.getOrigin() == null)
        {
            return create(escapeName(element.getName()));
        }
        return element.getOrigin().getCompletePath().getChildPath(element.getName());
    }
    
    /**
     * Returns DataElementPath constructed by existing DataCollection and its child name if argument is not null (otherwise return null).
     * 
     * @param dc - parent DataCollection
     * @param childName - name of the child item (child may not exist)
     */
    public static DataElementPath create(DataCollection<?> dc, String childName)
    {
        if(dc==null || childName==null) return null;
        return dc.getCompletePath().getChildPath(childName);
    }
    
    /**
     * Construct from array of paths. Equivalent to new DataElementPath(basePath).getRelativePath(path[0]).getRelativePath(path[1])...
     * 
     * @param basePath - first slice. If null, then null will be returned
     * @param path - list of path slices. Note that it's path slices, not names, thus they should be escaped even if they contain only one path component
     *
    public static DataElementPath create(String basePath, String... path)
    {
        if( basePath == null )
            return null;
       //TODO
        return StreamEx.of(path).foldLeft( create( basePath ), DataElementPath::getRelativePath );
    }*/

    /**
     * Escapes special chars in element name.
     * 
     * @param name unescaped name
     * @return escaped name
     */
    public static @Nonnull String escapeName(@Nonnull String name)
    {
        char[] result = null;
        int j=0;
        for(int i=0; i<name.length(); i++)
        {
            char curChar = name.charAt(i);
            if(curChar == '\\' || curChar == '/')
            {
                if(result == null)
                {
                    result = new char[name.length()*2-i];
                    for(j=0; j<i; j++) result[j] = name.charAt(j);
                }
                result[j++] = '\\';
                result[j++] = curChar == '/'?'s':'\\';
            } else if(result != null) result[j++] = curChar;
        }
        return result == null ? name : new String(result, 0, j);
    }
    
    /**
     * Unescapes special chars in element name.
     * 
     * @param escapedName escaped name
     * @return unescaped name
     */
    public static @Nonnull String unescapeName(@Nonnull String escapedName)
    {
        char[] result = null;
        int j=0;
        for(int i=0; i<escapedName.length(); i++)
        {
            char curChar = escapedName.charAt(i);
            if(curChar == '\\')
            {
                if(result == null)
                {
                    result = new char[escapedName.length()-1];
                    for(j=0; j<i; j++) result[j] = escapedName.charAt(j);
                }
                i++;
                if(i < escapedName.length())
                {
                    char nextChar = escapedName.charAt(i);
                    result[j++] = nextChar == 's'?'/':nextChar;
                }
            } else if(result != null) result[j++] = curChar;
        }
        return result == null ? escapedName : new String(result, 0, j);
    }
}
