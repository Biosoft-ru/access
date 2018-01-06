package ru.biosoft.access.core.filter;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;

/**
 * Used for filtering data elements.
 */
public interface Filter<T extends DataElement>
{
    /**
     * Default filter that accept any data element.
     */
    @Nonnull Filter<DataElement> INCLUDE_ALL_FILTER = new IncludeAllFilter();

    /**
     * Default filter that accept nothing.
     */
    @Nonnull Filter<DataElement> INCLUDE_NONE_FILTER = new IncludeNoneFilter();

    /** Indicates whether a filter should be used. */
    boolean isEnabled();

    /**
     * Return is data element accepted by filter.
     * @param de Data element.
     * @return <code>true</code> - if filter accept specified data element, <code>false</code> otherwise.
     * @see DataElement
     */
    boolean isAcceptable(T de);

    /**
     * Implementation of filter that accept any data element.
     */
    static class IncludeAllFilter implements Filter<DataElement>
    {
        /**
         * This constructor explicitly declared for preventing creation of this class.
         */
        private IncludeAllFilter()
        {}

        /** Always return false. */
        @Override
        public boolean isEnabled()
        {
            return false;
        }

        /**
         * Always return <code>true</code>.
         * @return Always <code>true</code>
         */
        @Override
        public boolean isAcceptable(DataElement de)
        {
            return true;
        }
    }// end of class IncludeAllFilter

    /**
     * Implementation of filter that accept nothing.
     */
    static class IncludeNoneFilter implements Filter<DataElement>
    {
        /**
         * This constructor explicitly declared for preventing creation of this class.
         */
        private IncludeNoneFilter()
        {}

        /** Always return true. */
        @Override
        public boolean isEnabled()
        {
            return true;
        }

        /**
         * Always return <code>false</code>.
         * @return Always <code>false</code>
         */
        @Override
        public boolean isAcceptable(DataElement de)
        {
            return false;
        }
    }// end of class IncludeNoneFilter
}
