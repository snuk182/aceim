package aceim.app.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class ResizeableRelativeLayout extends RelativeLayout {
	
	private OnResizeListener mResizeListener;

	public ResizeableRelativeLayout(Context context) {
		super(context);
	}

	public ResizeableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ResizeableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		if (mResizeListener != null) {
			mResizeListener.onSizeChanged(w, h, oldw, oldh);
		}
	}

	public interface OnResizeListener {
		void onSizeChanged(int w, int h, int oldw, int oldh);
	}

	/**
	 * @return the mResizeListener
	 */
	public OnResizeListener getResizeListener() {
		return mResizeListener;
	}

	/**
	 * @param mResizeListener the mResizeListener to set
	 */
	public void setResizeListener(OnResizeListener mResizeListener) {
		this.mResizeListener = mResizeListener;
	} 
}
