package ru.biosoft.access.file;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FilePatternFilter
{
    private File rootFile;
    private final List<PathPattern> patterns = new ArrayList<PathPattern>();

    public FilePatternFilter(File rootFile, List<String> filters)
    {
        this.rootFile = rootFile;
        if( filters == null )
            return;
        for ( int i = 0; i < filters.size(); i++ )
        {
            String line = filters.get( i );
            PathPattern pattern = PathPattern.create( line );
            if( pattern.isExclude() )
            {
                patterns.add( pattern );
            }
            else
            {
                patterns.add( 0, pattern );
            }
        }
    }

    public boolean isExcluded(File file)
    {
        if( patterns.isEmpty() )
            return false;

        Path rootPath = rootFile.toPath();
        Path filePath = file.toPath();
        Path relative = rootPath.relativize( filePath );
        String filePathStr = relative.toString();
        StringBuilder pathBuilder = new StringBuilder( filePathStr.length() );
        String rootPathStr = rootFile.toString();

        while ( true )
        {
            int offset = filePathStr.indexOf( '/', pathBuilder.length() + 1 );
            boolean isDirectory = true;

            if( offset == -1 )
            {
                offset = filePathStr.length();
                isDirectory = file.isDirectory();
            }

            pathBuilder.insert( pathBuilder.length(), filePathStr, pathBuilder.length(), offset );
            String currentPath = pathBuilder.toString();

            for ( PathPattern pattern : patterns )
            {
                if( pattern.matches( currentPath, isDirectory, rootPathStr ) )
                {
                    return pattern.isExclude();
                }
            }
            if( !isDirectory || pathBuilder.length() >= filePathStr.length() )
            {
                return false;
            }
        }
    }
}
