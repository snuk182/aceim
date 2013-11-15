package aceim.app.widgets;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class HorizontalListView extends HorizontalScrollView {

	private final Handler mHandler = new Handler();

	private LinearLayout mLayout;

	private ArrayAdapter<?> mAdapter;

	private final DataSetObserver mObserver = new DataSetObserver() {

		@Override
		public void onChanged() {

			for (int i = 0; i < mAdapter.getCount(); i++) {
				View view;
				if ((mLayout.getChildCount() - 1) < i) {
					view = null;
				} else {
					view = mLayout.getChildAt(i);
				}

				View newView = mAdapter.getView(i, view, mLayout);
				if (view == null) {
					mLayout.addView(newView);
				} else if (view != newView) {
					mLayout.removeViewAt(i);
					mLayout.addView(newView, i);
				}
			}

			if (mLayout.getChildCount() > mAdapter.getCount()) {
				for (int i = mAdapter.getCount(); i < mLayout.getChildCount(); i++) {
					mLayout.removeViewAt(i);
				}
			}
		}

		@Override
		public void onInvalidated() {
			mLayout.removeAllViews();

			for (int i = 0; i < mAdapter.getCount(); i++) {
				mLayout.addView(mAdapter.getView(i, null, mLayout));
			}
		}
	};

	public HorizontalListView(Context context) {
		super(context);
		init(context);
	}

	public HorizontalListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public HorizontalListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mLayout = new LinearLayout(context);
		mLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		addView(mLayout);
	}

	public void setAdapter(ArrayAdapter<?> adapter) {
		this.mAdapter = adapter;
		this.mAdapter.registerDataSetObserver(mObserver);
	}

	public ArrayAdapter<?> getAdapter() {
		return mAdapter;
	}

	public void setSelected(Object tag) {
		if (tag == null) {
			return;
		}

		for (int i = 0; i < mLayout.getChildCount(); i++) {
			final View view = mLayout.getChildAt(i);
			view.setSelected(view.getTag() == tag);

			if (view.getTag() == tag) {
				mHandler.post(new Runnable() {

					@Override
					public void run() {
						scrollTo(view);
					}
				});
			}
		}
	}

	private void scrollTo(View child) {
		Rect rect = new Rect();
		getDrawingRect(rect);

		int leftBound = child.getLeft();
		int rightBound = child.getRight();

		if (rect.left > leftBound) {
			smoothScrollTo(leftBound, 0);
			return;
		}
		if (rect.right < rightBound) {
			smoothScrollTo(rightBound, 0);
			return;
		}
	}
}
