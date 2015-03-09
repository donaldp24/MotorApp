package com.Tony.Zakron;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by donal_000 on 3/9/2015.
 */
public class BaseActivity extends Activity {

    protected View mainLayout = null;
    protected boolean bInitialized = false;
    protected int curOrientation;

    public static final boolean USE_RESOLUTIONSET = true;
    public static final boolean USE_GLOBALLISTENER = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        // initialize resolution set
        mainLayout = (View)findViewById(R.id.layout_parent);

        if (USE_RESOLUTIONSET) {
            if (USE_GLOBALLISTENER) {
                mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            public void onGlobalLayout() {
                                if (bInitialized == false) {
                                    int curOrientation = getResources().getConfiguration().orientation;

                                    Rect r = new Rect();
                                    mainLayout.getLocalVisibleRect(r);
                                    ResolutionSet._instance.setResolution(r.width(), r.height(), true);
                                    ResolutionSet._instance.iterateChild(mainLayout);
                                    bInitialized = true;

                                    changedLayoutOrientation(curOrientation);
                                }
                            }
                        }
                );
            }
            else {
                ResolutionSet._instance.iterateChild(mainLayout);
                changedLayoutOrientation(curOrientation);
            }
        }
    }

    public void changedLayoutOrientation(int orientation) {
        curOrientation = orientation;
    }
}