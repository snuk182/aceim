package aceim.app.widgets.bottombar;

import aceim.api.dataentity.ConnectionState;
import aceim.app.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

public class ContactListBottomBar extends BottomBar {

	public ContactListBottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.contact_list_bottom_bar, this);
	}
	
	public void onConnectionStateChanged(ConnectionState connState, int progress) {
		ProgressBar pBar = (ProgressBar) findViewById(R.id.progress);
		
		if (connState == ConnectionState.CONNECTING) {
			for (int i = 0; i < getChildCount(); i++) {
				View v = getChildAt(i);
				
				v.setVisibility((v == pBar) ? View.VISIBLE : View.GONE);
			}
			
			pBar.setProgress(progress);
		} else {
			for (int i = 0; i < getChildCount(); i++) {
				View v = getChildAt(i);
				
				v.setVisibility((v != pBar) ? View.VISIBLE : View.GONE);
			}
		}
	}
}
