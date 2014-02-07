package aceim.app.themeable.widgets;

import aceim.app.AceIMActivity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class StyleableImageView extends ImageView {

	public StyleableImageView(Context context) {
		super(context);
	}

	public StyleableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (context instanceof AceIMActivity) {
			((AceIMActivity)context).setStyle(this, attrs);
		}
	}

	public StyleableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (context instanceof AceIMActivity) {
			((AceIMActivity)context).setStyle(this, attrs);
		}
	}

}
