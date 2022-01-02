package aceim.app.widgets.adapters;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import aceim.api.utils.Logger;
import aceim.app.R;
import android.content.Context;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

public abstract class SingleViewAdapter<I,V extends View> extends ArrayAdapter<I> {
	
	//private static final LayoutParams ITEM_LAYOUT_PARAMS = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	private static LayoutParams ITEM_LAYOUT_PARAMS;
	
	private final Class<V> mViewClass;

	private OnSingleViewAdapterItemClickListener mOnItemClickListener;
	
	@SuppressWarnings("unchecked")
	public SingleViewAdapter(Context context, List<I> objects) {
		super(context, 0, objects);
		mViewClass = (Class<V>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];		
	}

	@SuppressWarnings("unchecked")
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		V view;
		
		if (convertView != null) {
			view = (V) convertView;
		} else {
			try {
				view = mViewClass.getConstructor(Context.class).newInstance(getContext());
				
				if (ITEM_LAYOUT_PARAMS == null) {
					int defaultItemSize = getContext().getResources().getDimensionPixelSize(R.dimen.top_bottom_bar_height) - getContext().getResources().getDimensionPixelSize(R.dimen.top_bottom_padding);
					ITEM_LAYOUT_PARAMS = new LayoutParams(defaultItemSize, defaultItemSize);
				}
				
				view.setLayoutParams(ITEM_LAYOUT_PARAMS);
				
				//int padding = getContext().getResources().getDimensionPixelSize(R.dimen.default_padding);
				//int sidePadding = padding / 2;
				//view.setPadding(sidePadding, padding, sidePadding, padding);
				view.setBackgroundResource(android.R.drawable.menuitem_background);
			} catch (Exception e) {
				Logger.log(e);
				FrameLayout f = new FrameLayout(getContext());
				f.setLayoutParams(new LayoutParams(0, 0));
				view = (V) f;
			}			
		}
		
		I item = getItem(position);
		
		fillView(item, view);
		
		view.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				performItemClick(v, position, 0);
			}
		});
		
		return view;
	}

	protected abstract void fillView(I item, V view);
	
	private void performItemClick(View view, int position, long id) {
        if (mOnItemClickListener == null) {
            return;
        }

        view.playSoundEffect(SoundEffectConstants.CLICK);
        view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        mOnItemClickListener.onItemClick(this, position);
    }

	public void setOnItemClickListener(OnSingleViewAdapterItemClickListener onItemClickListener) {
		this.mOnItemClickListener = onItemClickListener;
	}
	
	public static abstract class OnSingleViewAdapterItemClickListener implements OnItemClickListener {
		
		public abstract void onItemClick(SingleViewAdapter<?,?> adapter, int position);

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			onItemClick((SingleViewAdapter<?, ?>) parent.getAdapter(), position);
		}		
	}
}
