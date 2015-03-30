package com.Tony.Zakron;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

/**
 * Created by donal_000 on 1/8/2015.
 */
public class UIManager {
    private static UIManager _instance;
    protected Context mContext;

    public static UIManager initialize(Context context) {
        if (_instance == null)
            _instance = new UIManager(context);
        return _instance;
    }

    public static UIManager sharedInstance() {
        return _instance;
    }

    private UIManager(Context context) {
        mContext = context;
    }

    public Object showProgressDialog(Context context, String title, String message, boolean indeterminate) {
        ProgressDialog dlg = ProgressDialog.show((context == null ? mContext : context), title, message, indeterminate);
        return dlg;
    }

    public void dismissProgressDialog(Object dlg) {
        try {
            ProgressDialog progressDialog = (ProgressDialog) dlg;
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Toast gToast = null;
    public void showToastMessage(Context context, String message) {
        if (gToast == null || gToast.getView().getWindowVisibility() != View.VISIBLE) {
            gToast = Toast.makeText((context == null) ? mContext : context, message, Toast.LENGTH_LONG);
            gToast.setGravity(Gravity.CENTER, 0, 0);
            gToast.show();
        }
    }
}
