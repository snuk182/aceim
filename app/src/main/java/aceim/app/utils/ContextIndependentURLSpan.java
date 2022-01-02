package aceim.app.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.style.URLSpan;
import android.view.View;

public class ContextIndependentURLSpan extends URLSpan {
	
	public ContextIndependentURLSpan(String url) {
		super(url);
	}

	public ContextIndependentURLSpan(Parcel src) {
		super(src);
	}

	@Override
    public void onClick(View widget) {
		Uri uri = Uri.parse(getURL());
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        intent.setFlags(intent.getFlags()|Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
