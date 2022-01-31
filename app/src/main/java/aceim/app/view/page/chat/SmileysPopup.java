package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import aceim.app.Constants;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.dataentity.SmileyResources;
import aceim.app.utils.ViewUtils;
import aceim.app.widgets.HorizontalListView;
import aceim.app.widgets.adapters.SingleViewAdapter;
import aceim.app.widgets.adapters.SingleViewAdapter.OnSingleViewAdapterItemClickListener;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class SmileysPopup {

	private int mSoftKbHeight = 0;

	private final MainActivity mActivity;

	private PopupWindow mPopupWindow;
	private EditText mEditor;
	private View mSmileysView;

	private final boolean mDontDrawSmileys;

	private final OnSingleViewAdapterItemClickListener mSmileyClicklistener = new OnSingleViewAdapterItemClickListener() {

		@Override
		public void onItemClick(SingleViewAdapter<?, ?> adapter, int position) {
			if (adapter instanceof ImageSmileyAdapter) {
				ViewUtils.insertToEditor(((ImageSmileyAdapter) adapter).getItemName(position), mEditor);
			} else {
				ViewUtils.insertToEditor(adapter.getItem(position).toString(), mEditor);
			}
		}
	};
	
	private final Runnable mShowPopupRunnable = new Runnable() {
		
		@Override
		public void run() {
			Rect rect = new Rect();
			mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

			boolean showInsteadOfKeyboard = showInsteadOfKeyboard();
			if (showInsteadOfKeyboard) {
				mPopupWindow.setHeight(getPossibleKeyboardHeight());
				mPopupWindow.showAtLocation(mActivity.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
				mPopupWindow.getContentView().setPadding(0, 0, 0, 0);
			} else {			
				mSoftKbHeight = rect.height() - mEditor.getHeight();				
				mPopupWindow.setHeight(mSoftKbHeight);			
				mPopupWindow.showAtLocation(mActivity.getWindow().getDecorView(), Gravity.TOP, rect.left, rect.top);
			}

			HorizontalListView tabsView = (HorizontalListView) mSmileysView.findViewById(R.id.tab_selector);

			List<SmileyResources> smileys = new ArrayList<SmileyResources>(mActivity.getSmileysManager().getUnmanagedSmileys());
			Collections.reverse(smileys);
			tabsView.setAdapter(new SmileyResourcesAdapter(mActivity, smileys));
			tabsView.getAdapter().notifyDataSetChanged();

			if (smileys.size() < 2) {
				tabsView.getLayoutParams().height = 0;
			} else {
				tabsView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
			}
			mSmileysView.requestLayout();

			setSelected(smileys.get(0));
		}
	};

	public SmileysPopup(MainActivity activity) {
		this.mActivity = activity;

		mDontDrawSmileys = activity.getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getBoolean(GlobalOptionKeys.TEXT_SMILEYS.name(), Boolean.parseBoolean(activity.getString(R.string.default_text_smilies)));

		init();
	}

	private void init() {
		mSmileysView = LayoutInflater.from(mActivity).inflate(R.layout.smileys_view, null);
	}

	public void show(EditText editText) {
		if (editText == null) {
			if (isShown()) {
				hide();
			}
			return;
		}
		
		mEditor = editText;
		if (mPopupWindow == null) {
			mPopupWindow = new PopupWindow(mActivity);
			mPopupWindow.setContentView(mSmileysView);
			mPopupWindow.setBackgroundDrawable(null);
			mPopupWindow.setWidth(mActivity.getWindow().getDecorView().getWidth());
			mPopupWindow.setHeight(mActivity.getWindow().getDecorView().getHeight() / 2);
		}
		
		mActivity.runOnUiThread(mShowPopupRunnable);
	}

	private boolean showInsteadOfKeyboard() {
		return getPossibleKeyboardHeight() > 100;
	}

	private int getPossibleKeyboardHeight() {
		int viewHeight = mActivity.getScreen().getHeight();		
		int screenHeight = mActivity.getResources().getDisplayMetrics().heightPixels;
		int statusBarHeight = mActivity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
		
		return screenHeight - viewHeight - statusBarHeight;
	}

	private void setSelected(SmileyResources sr) {
		if (sr == null)
			return;

		ViewGroup container = (ViewGroup) mSmileysView.findViewById(R.id.fragment_holder);
		HorizontalListView tabsView = (HorizontalListView) mSmileysView.findViewById(R.id.tab_selector);

		container.removeAllViews();
		container.addView(getContentView(sr));
		tabsView.setSelected(sr);
	}

	public void hide() {
		if (mPopupWindow != null && mPopupWindow.isShowing()) {
			try {
				mPopupWindow.dismiss();
			} catch (Exception e) {
				//Sometimes thrown during orientation changes
			}
			//mPopupWindow = null;
		}
	}

	public boolean isShownForThisEditor(EditText editor) {
		return mEditor == editor && mPopupWindow != null && mPopupWindow.isShowing();
	}

	private View getContentView(SmileyResources resources) {
		GridView grid = new GridView(mActivity);

		grid.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		grid.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
		grid.setNumColumns(GridView.AUTO_FIT);
		grid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
		grid.setColumnWidth(mActivity.getResources().getDimensionPixelSize(R.dimen.default_smiley_size));

		if (grid.getTag() == null || grid.getTag() != resources) {
			SingleViewAdapter<?, ?> adapter = mDontDrawSmileys ? TextSmileyAdapter.fromStringList(mActivity, Arrays.asList(resources.getNames())) : ImageSmileyAdapter.fromSmileyResources(mActivity, resources);
			grid.setAdapter(adapter);
			grid.setTag(resources);
			adapter.setOnItemClickListener(mSmileyClicklistener);
		}

		return grid;
	}

	private class SmileyResourcesAdapter extends ArrayAdapter<SmileyResources> {

		public SmileyResourcesAdapter(Context context, List<SmileyResources> objects) {
			super(context, 0, 0, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			SmileyResources resources = getItem(position);

			TextView text;
			if (convertView == null) {
				text = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.smiley_tab_indicator, null);
			} else {
				text = (TextView) convertView;
			}

			if (text.getTag() == null || text.getTag() != resources) {
				text.setTag(resources);
				text.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						setSelected((SmileyResources) v.getTag());
					}
				});
			}
			
			text.setText(resources.getSmileyPackShortName());
			return text;
		}
	}

	public boolean isShown() {
		return mPopupWindow != null && mPopupWindow.isShowing();
	}
}
