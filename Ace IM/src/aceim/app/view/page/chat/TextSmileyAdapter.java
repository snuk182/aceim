package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.List;

import aceim.app.widgets.adapters.SingleViewAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.Gravity;
import android.widget.TextView;

public class TextSmileyAdapter extends SingleViewAdapter<String, TextView> {

	private TextSmileyAdapter(Context context, List<String> objects) {
		super(context, objects);
	}

	@Override
	protected void fillView(String item, TextView view) {
		view.setText(item);
		view.setGravity(Gravity.CENTER);
	}

	public static final TextSmileyAdapter fromTypedArray(Context context, int typedArrayId){
		Resources r = context.getResources();
		TypedArray objects = r.obtainTypedArray(typedArrayId);
		
		List<String> list = new ArrayList<String>(objects.length());
		for (int i=0; i<objects.length(); i++) {
			String s = objects.getString(i);
			list.add(s);
		}
		
		objects.recycle();
		
		return fromStringList(context, list);
	}
	
	public static final TextSmileyAdapter fromStringList(Context context, List<String> list){
		return new TextSmileyAdapter(context, list);
	}
}
