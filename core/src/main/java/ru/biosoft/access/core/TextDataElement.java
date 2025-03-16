package ru.biosoft.access.core;

import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon ( "resources/txt.gif" )
@PropertyName("text")
public class TextDataElement extends DataElementSupport implements CloneableDataElement
{
    public TextDataElement(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }
    
    public TextDataElement(String name, DataCollection<?> origin, String content)
    {
        super(name, origin);
        this.content = content;
    }
    
    protected String content;

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }
    
    public long getContentLength()
    {
        return content.length();
    }
    
    public String getContentType()
    {
        return "text/plain";
    }
}
