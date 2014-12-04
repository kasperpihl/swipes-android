package com.swipesapp.android.evernote;

/**
 * Created by Stanimir Karoserov on 11/20/14.
 */
public class EvernoteToDo {
    private String title;
    private boolean checked;
    private int position;

    public EvernoteToDo(String title, boolean checked, int position)
    {
        this.title = title;
        this.checked = checked;
        this.position = position;
    }

    public String getTitle()
    {
        return this.title;
    }

    public boolean isChecked()
    {
        return this.checked;
    }

    public int getPosition()
    {
        return this.position;
    }

    @Override
    public String toString()
    {
        return "EvernoteToDo{title='" + title + '\'' + ", checked=" + checked + ", position=" + position + '}';
    }
}
