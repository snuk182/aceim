package aceim.app.widgets.preference;

import aceim.app.R;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class EditablePasswordPreference extends EditTextPreference {

	private EditText oldPwBox = null;

	public EditablePasswordPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);		
	}

	@Override
	protected View onCreateDialogView() {
		
		
		String oldPw = getText();
		if (oldPw == null || oldPw.length()<1) {
			return super.onCreateDialogView();
		} else {
			LinearLayout layout = new LinearLayout(getContext());
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setPadding(6, 6, 6, 6);

			TextView oldPwLabel = new TextView(getContext());
			oldPwLabel.setText(R.string.old_pw);
			TextView newPwLabel = new TextView(getContext());
			newPwLabel.setText(R.string.new_pw);

			ViewParent oldParent = getEditText().getParent();
			if (oldParent != null) {
				((ViewGroup) oldParent).removeView(getEditText());
			}

			oldPwBox = new EditText(getContext());
			oldPwBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			
			layout.addView(oldPwLabel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			layout.addView(oldPwBox, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			layout.addView(newPwLabel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			layout.addView(getEditText(), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

			return layout;
		}
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		getEditText().setText("");
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {

		if (positiveResult) {
			if (oldPwBox != null && !oldPwBox.getText().toString().equals(getText())) {
				Toast.makeText(getContext(), R.string.wrong_pw, Toast.LENGTH_LONG).show();
				positiveResult = false;
			} else {
				Toast.makeText(getContext(), android.R.string.ok, Toast.LENGTH_LONG).show();
			}
		}

		super.onDialogClosed(positiveResult);
	}
}
