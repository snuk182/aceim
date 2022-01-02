package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aceim.app.MainActivity;
import aceim.app.dataentity.SmileyResources;
import aceim.app.utils.ViewUtils;
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
		Set<String> set = new HashSet<String>();
		
		for (SmileyResources smr : activity.getSmileysManager().getUnmanagedSmileys()) {
			set.addAll(Arrays.asList(smr.getNames()));
		}
		
		List<String> list = new ArrayList<String>(set);
		
		for (int i=0; i<list.size(); i++) {
			String smiley = list.get(i);
			
			if (!ViewUtils.isSmileyReadOnly(smiley)) {
				list.set(i, smiley);
			}
		}
		
		return fromStringList(activity, list);
	}
	
	public static final TextSmileyAdapter fromStringList(Context context, List<String> list){
		return new TextSmileyAdapter(context, list);
	}
}
