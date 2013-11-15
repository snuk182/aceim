package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.List;

import aceim.app.R;
import aceim.app.widgets.adapters.SingleViewAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageSmileyAdapter extends SingleViewAdapter<Drawable, ImageView> {

	private ImageSmileyAdapter(Context context, List<Drawable> objects) {
		super(context, objects);		
	}

	@Override
	protected void fillView(Drawable item, ImageView view) {
		view.setImageDrawable(item);
		view.setScaleType(ScaleType.CENTER_INSIDE);
	}

	public static final ImageSmileyAdapter fromTypedArray(Context context, int typedArrayId){
		Resources r = context.getResources();
		TypedArray objects = r.obtainTypedArray(typedArrayId);
		
		List<Drawable> list = new ArrayList<Drawable>(objects.length());
		for (int i=0; i<objects.length(); i++) {
			Drawable d = r.getDrawable(objects.getResourceId(i, R.drawable.ic_launcher));
			list.add(d);
		}
		
		objects.recycle();
		
		return fromDrawableList(context, list);
	}
	
	public static final ImageSmileyAdapter fromDrawableList(Context context, List<Drawable> list){
		return new ImageSmileyAdapter(context, list);
	}
}
