package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import aceim.api.utils.Logger;
import aceim.app.dataentity.SmileyResources;
import aceim.app.utils.ViewUtils;
import aceim.app.widgets.adapters.SingleViewAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageSmileyAdapter extends SingleViewAdapter<Drawable, ImageView> {
	
	private final List<String> mNames;
	
	private ImageSmileyAdapter(Context context, List<Drawable> objects, List<String> slist) {
		super(context, objects);	
		this.mNames = Collections.unmodifiableList(slist);
	}

	@Override
	protected void fillView(Drawable item, ImageView view) {
		view.setImageDrawable(item);
		view.setScaleType(ScaleType.CENTER_INSIDE);
	}

	public static final ImageSmileyAdapter fromSmileyResources(Context context, SmileyResources resources){
		
		try {
			Resources r = resources.getNativeResourcesForProtocol(context.getPackageManager());
			
			List<Drawable> dlist = new ArrayList<Drawable>();
			List<String> slist = new ArrayList<String>(Arrays.asList(resources.getNames()));
			
			int index = 0;
			for (Iterator<String> i = slist.iterator(); i.hasNext();) {
				String smiley = i.next();
				
				if (ViewUtils.isSmileyReadOnly(smiley)) {
					i.remove();
				} else {
					dlist.add(r.getDrawable(resources.getDrawableIDs()[index]));
				}
				
				index++;
			}
			
			return new ImageSmileyAdapter(context, dlist, slist);
		} catch (Exception e) {
			Logger.log(e);
		}
		
		return null;
	}
	
	@Override
	public void add(Drawable object) {
        throw new UnsupportedOperationException();
    }

	@Override
	public void addAll(Collection<? extends Drawable> collection) {
    	throw new UnsupportedOperationException();
    }

	@Override
	public void addAll(Drawable ... items) {
    	throw new UnsupportedOperationException();
    }
	
	@Override
	public void insert(Drawable object, int index) {
		throw new UnsupportedOperationException();
    }

	@Override
	public void remove(Drawable object) {
    	throw new UnsupportedOperationException();
    }

	@Override
	public void clear() {
    	throw new UnsupportedOperationException();
    }
	
	public String getItemName(int location){
		return mNames.get(location);
	}
}
