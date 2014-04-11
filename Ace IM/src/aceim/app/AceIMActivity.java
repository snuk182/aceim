package aceim.app;

import aceim.api.utils.Logger;
import aceim.app.themeable.ThemesManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;

public abstract class AceIMActivity extends FragmentActivity {
	
	private static final int top_bar_background = 0;
	private static final int bottom_bar_background = 1;
	private static final int screen_background = 2;
	private static final int bottom_bar_button_background = 3;
	private static final int accent_background = 4;
	private static final int grid_item_size = 5;
	private static final int list_item_height = 6;
	
	public static final int ARTIFICIAL_LAYOUT_MARKER = -8;
	
	private ThemesManager mThemesManager;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		
		mThemesManager = new ThemesManager(this);
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		mThemesManager.onExit();
	}
	
	/**
	 * @return the mThemesManager
	 */
	public ThemesManager getThemesManager() {
		return mThemesManager;
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void setStyle(View view, AttributeSet attrs) {	
		TypedArray styleables = getBaseContext().obtainStyledAttributes(attrs, R.styleable.StyleableView, 0, 0);
		Context themeContext = mThemesManager.getCurrentThemeContext();
		
		int N = styleables.getIndexCount();
		for (int i=0; i<N; i++) {
			
			int res = styleables.getIndex(i);			
			int id;
			
			switch(styleables.getInt(res, -1)) {
			case bottom_bar_background:
				id = mThemesManager.getViewResources().getBottomBarBackgroundId();
				break;
			case top_bar_background:
				id = mThemesManager.getViewResources().getTopBarBackgroundId();
				break;
			case screen_background:
				id = mThemesManager.getViewResources().getScreenBackgroundId();
				break;
			case bottom_bar_button_background:
				id = mThemesManager.getViewResources().getBottomBarButtonBackgroundId();
				break;
			case grid_item_size:
				id = mThemesManager.getViewResources().getGridItemSizeId();
				break;
			case list_item_height:
				id = mThemesManager.getViewResources().getListItemHeightId();
				break;
			case accent_background:
				id = mThemesManager.getViewResources().getAccentBackgroundId();
				break;
			default:
				styleables.recycle();
				return;
			}
			
			try {
				switch (res) {
				case R.styleable.StyleableView_styleableBackground:	
					Drawable d = themeContext.getResources().getDrawable(id);					
					
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
						view.setBackgroundDrawable(d);
					} else {
						view.setBackground(d);
					}
					break;
				case R.styleable.StyleableView_styleableLayoutWidth:
					int width = themeContext.getResources().getDimensionPixelSize(id);
					if (view.getLayoutParams() != null) {
						view.getLayoutParams().width = width;
					} else {
						view.setLayoutParams(new ViewGroup.LayoutParams(width, ARTIFICIAL_LAYOUT_MARKER));
					}
					break;
				case R.styleable.StyleableView_styleableLayoutHeight:
					int height = themeContext.getResources().getDimensionPixelSize(id);
					if (view.getLayoutParams() != null) {
						view.getLayoutParams().height = height;
					} else {
						view.setLayoutParams(new ViewGroup.LayoutParams(ARTIFICIAL_LAYOUT_MARKER, height));
					}
					break;
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}		
		
		styleables.recycle();
	}
	
	public void fillLayoutParams(LayoutParams lp, AttributeSet attrs) {
		TypedArray styleables = getBaseContext().obtainStyledAttributes(attrs, R.styleable.StyleableView, 0, 0);
		Context themeContext = mThemesManager.getCurrentThemeContext();
		
		int N = styleables.getIndexCount();
		for (int i=0; i<N; i++) {
			
			int res = styleables.getIndex(i);			
			int id;
			
			switch(styleables.getInt(res, -1)) {
			case grid_item_size:
				id = mThemesManager.getViewResources().getGridItemSizeId();
				break;
			case list_item_height:
				id = mThemesManager.getViewResources().getListItemHeightId();
				break;
			default:
				styleables.recycle();
				return;
			}
			
			try {
				switch (res) {
				case R.styleable.StyleableView_styleableLayoutWidth:
					lp.width = themeContext.getResources().getDimensionPixelSize(id);
					break;
				case R.styleable.StyleableView_styleableLayoutHeight:
					lp.height = themeContext.getResources().getDimensionPixelSize(id);
					break;
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}		
		
		styleables.recycle();
	}
}
