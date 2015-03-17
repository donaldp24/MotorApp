package com.Tony.Zakron;

import android.app.Activity;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ResolutionSet {

	public static float _fXpro = 1;
	public static float _fYpro = 1;
	public static float _fPro = 1;

    // designed width & height for landscape mode
	public static int _nDesignWidth = 480;
	public static int _nDesignHeight = 700;

	public static ResolutionSet _instance = new ResolutionSet();

	public ResolutionSet() {

	}

	public void setResolution(int x, int y, boolean isPortrate) {
        if (isPortrate)
		    _fXpro = (float)x / _nDesignWidth;
        else
            _fXpro = (float)x / _nDesignHeight;
        if (isPortrate)
		    _fYpro = (float)y / _nDesignHeight;
        else
            _fYpro = (float)y / _nDesignWidth;
		_fPro = Math.min(_fXpro, _fYpro);
	}


	// Update layouts in the view recursively.
	public void iterateChild(View view) {
		if (view instanceof ViewGroup)
		{
			ViewGroup container = (ViewGroup)view;
			int nCount = container.getChildCount();
			for (int i=0; i<nCount; i++)
			{
				iterateChild(container.getChildAt(i));
			}
		}
		UpdateLayout(view);
	}

	void UpdateLayout(View view)
	{
		LayoutParams lp;
		lp = (LayoutParams) view.getLayoutParams();
		if ( lp == null )
			return;
		if(lp.width > 0)
			lp.width = (int)(lp.width * _fXpro + 0.50001);
		if(lp.height > 0)
			lp.height = (int)(lp.height * _fYpro + 0.50001);

		//Padding.....
		int leftPadding = (int)( _fXpro * view.getPaddingLeft() );
		int rightPadding = (int)(_fXpro * view.getPaddingRight());
		int bottomPadding = (int)(_fYpro * view.getPaddingBottom());
		int topPadding = (int)(_fYpro * view.getPaddingTop());

		view.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

		if(lp instanceof ViewGroup.MarginLayoutParams)
		{
			ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)lp;

			//if(mlp.leftMargin > 0)
				mlp.leftMargin = (int)(mlp.leftMargin * _fXpro + 0.50001 );
			//if(mlp.rightMargin > 0)
				mlp.rightMargin = (int)(mlp.rightMargin * _fXpro + 0.50001);
			//if(mlp.topMargin > 0)
				mlp.topMargin = (int)(mlp.topMargin * _fYpro + 0.50001);
			//if(mlp.bottomMargin > 0)
				mlp.bottomMargin = (int)(mlp.bottomMargin * _fYpro + 0.50001);
		}

		if(view instanceof TextView)
		{
			TextView lblView = (TextView)view;
			//float txtSize = (float) (Math.sqrt((_fXpro+_fYpro)/2) * lblView.getTextSize());
			//float txtSize = (float) ((_fXpro+_fYpro)/2) * lblView.getTextSize();
			float txtSize = (float) (_fPro * lblView.getTextSize());
			lblView.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtSize);
		}
	}

    public static Point getScreenSize(Activity context, boolean isContainNavBar, boolean isPortrait)
    {
        int width = 0, height = 0;
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = context.getWindowManager().getDefaultDisplay();
        Method mGetRawH = null, mGetRawW = null;

        if (isContainNavBar == false)
        {
            int nWidth = display.getWidth(), nHeight = display.getHeight();
            if (!isPortrait)
            {
                if (nWidth > nHeight)
                    return new Point(nWidth, nHeight);
                else
                    return new Point(nHeight, nWidth);
            }
            else
            {
                if (nWidth > nHeight)
                    return new Point(nHeight, nWidth);
                else
                    return new Point(nWidth, nHeight);
            }
        }

        try {
            // For JellyBean 4.2 (API 17) and onward
            /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(metrics);

                width = metrics.widthPixels;
                height = metrics.heightPixels;
            } else { */
            mGetRawH = Display.class.getMethod("getRawHeight");
            mGetRawW = Display.class.getMethod("getRawWidth");

            try {
                width = (Integer) mGetRawW.invoke(display);
                height = (Integer) mGetRawH.invoke(display);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //}
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
        }

        if (width != 0 || height != 0)
        {
            if (!isPortrait)
            {
                if (width > height)
                    return new Point(width, height);
                else
                    return new Point(height, width);
            }
            else
            {
                if (width > height)
                    return new Point(height, width);
                else
                    return new Point(width, height);
            }
        }
        else
        {
            WindowManager winManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
            context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int nWidth = metrics.widthPixels, nHeight = metrics.heightPixels;
            if (!isPortrait)
            {
                if (nWidth > nHeight)
                    return new Point(nWidth, nHeight);
                else
                    return new Point(nHeight, nWidth);
            }
            else
            {
                if (nWidth > nHeight)
                    return new Point(nHeight, nWidth);
                else
                    return new Point(nWidth, nHeight);
            }
        }
    }
}
