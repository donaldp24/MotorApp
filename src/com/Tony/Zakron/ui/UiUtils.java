package com.Tony.Zakron.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.Tony.Zakron.helper.OSDate;

import java.util.Date;

/**
 * Created by donal_000 on 1/21/2015.
 */
public class UiUtils {
    public static void showKeyboard(Context context, EditText editText) {
        InputMethodManager mgr = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (editText == null) {
            return;
        }
        else {
            mgr.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
        }
    }

    public static void showKeyboard(Context context, View view) {
        InputMethodManager mgr = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.toggleSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
    }

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void hideKeyboard(Context context, EditText editText)
    {
        InputMethodManager mgr = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static String getSyncingDateStringWithDate(Date dt) {
        if (dt == null)
            return "";
        OSDate date = new OSDate(dt);
        return date.toStringWithFormat("LLL d"); // "Jan 12";
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
