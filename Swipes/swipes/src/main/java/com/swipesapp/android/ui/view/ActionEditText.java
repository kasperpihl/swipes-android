package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import com.swipesapp.android.ui.listener.KeyboardBackListener;

/**
 * An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits.
 * It also informs listeners when the back button has been pressed.
 *
 * @author Fernanda Bari
 */
public class ActionEditText extends EditText {

    private KeyboardBackListener mListener;

    public ActionEditText(Context context) {
        super(context);
    }

    public ActionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActionEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return conn;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP) {
            // Back button has been pressed. Inform listeners.
            if (mListener != null) {
                mListener.onKeyboardBackPressed();
            }
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    public void setListener(KeyboardBackListener listener) {
        mListener = listener;
    }

}
