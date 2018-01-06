package ru.biosoft.access.core.filter;

import java.util.regex.Pattern;

import ru.biosoft.access.core.DataElement;


/**
 * Abstract filter to check some <code>DataElement</code> property
 * to correspond regular expression.
 */
@SuppressWarnings("serial")
abstract public class PatternFilter<T extends DataElement> extends MutableFilter<T>
{
    public PatternFilter()
    {
        setEnabled(false);
    }

    /**
     *This method should be implemented in subclasses to return
     * property value that should match the pattern.
     *
     * @param de expected <code>DataElement</code>
     */
    abstract public String getCheckedProperty(T de);

    protected String pattern = ".*";
    protected String match = ".*";
    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        String oldValue = this.pattern;
        this.pattern = pattern;
        match = pattern;
        firePropertyChange("pattern", oldValue, pattern);
    }

    private boolean acceptEmpty;
    public boolean isAcceptEmpty()
    {
        return acceptEmpty;
    }

    public void setAcceptEmpty(boolean acceptEmpty)
    {
        boolean oldValue = this.acceptEmpty;
        this.acceptEmpty = acceptEmpty;
        firePropertyChange("acceptEmpty", oldValue, acceptEmpty);
    }

    @Override
    public boolean isAcceptable(T de)
    {
        if(de == null)
            return acceptEmpty;

        String value = getCheckedProperty(de);
        if(value == null)
            return acceptEmpty;

        return Pattern.compile(match, Pattern.CASE_INSENSITIVE).matcher(value).find();
    }
}



