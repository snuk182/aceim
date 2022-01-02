package aceim.app.view.page.other;

import aceim.app.Constants;
import aceim.app.R;
import aceim.app.dataentity.GlobalOptionKeys;
import aceim.app.view.page.Page;
import aceim.app.widgets.bottombar.BottomBarButton;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public final class MasterPassword extends Page {
	
	private TextView mTitle;
	private BottomBarButton mProceedButton;
	private EditText mEditor;
	
	private final OnClickListener mProceedClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String pw = mEditor.getText().toString();
			
			if (TextUtils.isEmpty(pw)){
				mTitle.setText(getMainActivity().getString(R.string.X_cannot_be_empty, getMainActivity().getString(R.string.password)));
				mTitle.setBackgroundResource(R.drawable.criteria_bad);
				return;
			}
			
			String masterPw = getMainActivity().getSharedPreferences(Constants.SHARED_PREFERENCES_GLOBAL, 0).getString(GlobalOptionKeys.MASTER_PASSWORD.name(), null);
			
			if (!masterPw.equals(pw)){
				mTitle.setText(R.string.wrong_pw);
				mTitle.setBackgroundResource(R.drawable.criteria_bad);
				return;
			}
			
			Page.addSplash(getMainActivity().getScreen());
			removeMe();
			
			getMainActivity().proceedInitScreen();
		}
	};
	
	@Override
	public View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View v = inflater.inflate(R.layout.master_password, group, false);
		
		mTitle = (TextView) v.findViewById(R.id.title);
		mProceedButton = (BottomBarButton) v.findViewById(R.id.proceed);
		mEditor = (EditText) v.findViewById(R.id.editor);
		
		mProceedButton.setOnClickListener(mProceedClickListener);
		
		return v;
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(R.drawable.ic_menu_login);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.master_password);
	}
}
