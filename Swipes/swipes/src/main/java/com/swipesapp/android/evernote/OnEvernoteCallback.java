package com.swipesapp.android.evernote;

/**
 * Created by Stanimir Karoserov on 11/25/14.
 */

public abstract class OnEvernoteCallback<T> {

    /**
     * @param data sent to callback when the async operation has completed positively
     */
    public abstract void onSuccess(final T data);

    /**
     * @param e is the error sent to the callback when the async operation has completed negatively
     */
    public abstract void onException(final Exception e);
}
