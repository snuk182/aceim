package aceim.app.utils;

import aceim.app.MainActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.style.URLSpan;
import android.view.View;

public class ContextIndependentURLSpan extends URLSpan {
	
	private final MainActivity mActivity;

	public ContextIndependentURLSpan(MainActivity activity, String url) {
		super(url);
		this.mActivity = activity;
	}

	public ContextIndependentURLSpan(MainActivity activity, Parcel src) {
		super(src);
		this.mActivity = activity;
	}

	@Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getURL());
        
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mActivity.getPackageName());
        mActivity.startActivity(intent);
    }
}
