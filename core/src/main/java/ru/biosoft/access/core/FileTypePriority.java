package ru.biosoft.access.core;

public enum FileTypePriority
{
    EXCLUSIVE_PRIORITY (100),
    HIGHEST_PRIORITY (30),
    HIGH_PRIORITY (20),
    MEDIUM_PRIORITY (10),
    BELOW_MEDIUM_PRIORITY (7),
    LOW_PRIORITY (5),
    LOWEST_PRIORITY (1),
    ZERO_PROPRITY (0);
    
    private int priority;
    
    FileTypePriority(int p)
    {
        this.priority = p;
    }

    public int getPriorityValue()
    {
        return priority;
    }

    public boolean isHigher(FileTypePriority other)
    {
        return priority > other.getPriorityValue();
    }
}
