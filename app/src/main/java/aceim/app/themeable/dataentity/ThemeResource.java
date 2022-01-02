package aceim.app.themeable.dataentity;

import org.xmlpull.v1.XmlPullParser;

import aceim.api.utils.Logger;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView.LayoutParams;

public abstract class ThemeResource {

	private final Context mContext;
	private final int mId;
	/**
	 * @return the mContext
	 */
	public Context getContext() {
		return mContext;
	}
	/**
	 * @return the mId
	 */
	public int getId() {
		return mId;
	}
	
	protected ThemeResource(Context context, int id) {
		this.mContext = context;
		this.mId = id;
	}
	
	public View getView() {
		XmlPullParser layoutParser = mContext.getResources().getLayout(mId);
		XmlPullParser attrsParser = mContext.getResources().getLayout(mId);
		
		View view = LayoutInflater.from(mContext).inflate(layoutParser, null);
		
		try {
			while (attrsParser.nextToken() != XmlPullParser.START_TAG) {}
		} catch (Exception e) {
			Logger.log(e);
		}
		view.setLayoutParams(new LayoutParams(mContext, (AttributeSet) attrsParser));
		
		return view;
	}
}
