package aceim.app;

import aceim.app.themeable.ThemesManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;

public abstract class AceIMActivity extends FragmentActivity {

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
	
	public void setStyle(View view, AttributeSet attrs) {	
		TypedArray a = mThemesManager.getCurrentTheme().obtainStyledAttributes(attrs, R.styleable.StyleableView, 0, 0);
		
		int N = a.getIndexCount();
		for (int i=0; i<N; i++) {
			
			int res = a.getIndex(i);
			
			switch (res) {
			case R.styleable.StyleableView_styleableBackground:	
				Drawable d = a.getDrawable(i);
				
				view.setBackgroundDrawable(d);
				break;
			case R.styleable.StyleableView_styleableLayoutWidth:
				if (view.getLayoutParams() != null) {
					view.getLayoutParams().width = a.getDimensionPixelSize(i, ViewGroup.LayoutParams.MATCH_PARENT);
				}
				break;
			case R.styleable.StyleableView_styleableLayoutHeight:
				if (view.getLayoutParams() != null) {
					view.getLayoutParams().height = a.getDimensionPixelSize(i, ViewGroup.LayoutParams.WRAP_CONTENT);
				}
				break;
			}
		}		
		
		a.recycle();
	}
	
	public void fillLayoutParams(LayoutParams lp, AttributeSet attrs) {
		TypedArray a = mThemesManager.getCurrentTheme().obtainStyledAttributes(attrs, R.styleable.StyleableView, 0, 0);
		
		int N = a.getIndexCount();
		for (int i=0; i<N; i++) {
			
			int res = a.getIndex(i);
			
			switch (res) {
			case R.styleable.StyleableView_styleableLayoutWidth:
				lp.width = a.getDimensionPixelSize(i, ViewGroup.LayoutParams.MATCH_PARENT);
				break;
			case R.styleable.StyleableView_styleableLayoutHeight:
				lp.height = a.getDimensionPixelSize(i, ViewGroup.LayoutParams.WRAP_CONTENT);
				break;
			}
		}		
		
		a.recycle();
	}
}
