package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aceim.app.MainActivity;
import aceim.app.dataentity.SmileyResources;
import aceim.app.widgets.adapters.SingleViewAdapter;
import android.content.Context;
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

	public static final TextSmileyAdapter fromTypedArray(MainActivity activity){
		Set<String> list = new HashSet<String>();
		
		for (SmileyResources smr : activity.getAdditionalSmileys()) {
			list.addAll(Arrays.asList(smr.getNames()));
		}
		
		return fromStringList(activity, new ArrayList<String>(list));
	}
	
	public static final TextSmileyAdapter fromStringList(Context context, List<String> list){
		return new TextSmileyAdapter(context, list);
	}
}
