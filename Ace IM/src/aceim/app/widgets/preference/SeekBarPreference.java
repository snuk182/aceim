package aceim.app.widgets.preference;

import aceim.api.utils.Logger;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
	
	private static final int[] ATTR_SET = new int[]{android.R.attr.max, android.R.attr.stepSize, android.R.attr.defaultValue};

	public int maximum = 100;
	public int stepSize = 5;

	private int oldValue = 50;
	private TextView monitorBox;
	private SeekBar bar;

	public SeekBarPreference(Context context) {
		super(context);
	}

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		TypedArray ta = getContext().obtainStyledAttributes(attrs, ATTR_SET);
		maximum = ta.getInt(0, 100);
		stepSize = (int) ta.getFloat(1, 5);
		oldValue = ta.getInt(2, 50);
		ta.recycle();
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		
		LinearLayout layout;
		
		View old = super.onCreateView(parent);
		if (old instanceof LinearLayout) {
			layout = (LinearLayout) old;
		} else {
			layout = new LinearLayout(getContext());

			LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params1.gravity = Gravity.LEFT;
			params1.weight = 1.0f;

			layout.setPadding(15, 5, 10, 5);
			layout.setOrientation(LinearLayout.HORIZONTAL);

			TextView view = new TextView(getContext());
			view.setText(getTitle());
			view.setTextSize(18);
			view.setGravity(Gravity.LEFT);
			view.setLayoutParams(params1);

			layout.addView(view);
			layout.setId(android.R.id.widget_frame);
		}
		
		DisplayMetrics display = getContext().getResources().getDisplayMetrics();

		LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams((int) (display.widthPixels * 0.4), LinearLayout.LayoutParams.WRAP_CONTENT);
		params2.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;

		LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params3.gravity = Gravity.CENTER;

		bar = new SeekBar(getContext());
		bar.setMax(maximum);
		bar.setProgress(oldValue);
		bar.setLayoutParams(params2);
		bar.setOnSeekBarChangeListener(this);
		
		this.monitorBox = new TextView(getContext());
		this.monitorBox.setLayoutParams(params3);
		this.monitorBox.setPadding(2, 5, 0, 0);
		this.monitorBox.setText(bar.getProgress() + "");

		layout.addView(bar);
		layout.addView(this.monitorBox);
		
		return layout;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		progress = Math.round(((float) progress) / stepSize) * stepSize;

		if (!callChangeListener(progress)) {
			seekBar.setProgress((int) this.oldValue);
			return;
		}

		seekBar.setProgress(progress);
		this.monitorBox.setText(progress + "");
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		seekBar.setSecondaryProgress(oldValue);	
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		oldValue = seekBar.getProgress();
		notifyChanged();
		updatePreference(seekBar.getProgress());		
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {

		int dValue = (int) ta.getInt(index, 50);

		return validateValue(dValue);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		int temp = restoreValue ? getPersistedInt(50) : (Integer) defaultValue;

		if (!restoreValue)
			persistInt(temp);

		this.oldValue = temp;
	}

	private int validateValue(int value) {

		if (value > maximum)
			value = maximum;
		else if (value < 0)
			value = 0;
		else if (value % stepSize != 0)
			value = Math.round(((float) value) / stepSize) * stepSize;

		return value;
	}

	private void updatePreference(int newValue) {
		Logger.log("Saving seekbar value "+newValue);
		SharedPreferences.Editor editor = getEditor();
		editor.putInt(getKey(), newValue);
		editor.commit();
	}
}
