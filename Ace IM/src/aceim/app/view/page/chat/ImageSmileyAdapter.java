package aceim.app.view.page.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import aceim.api.utils.Logger;
import aceim.app.MainActivity;
import aceim.app.dataentity.SmileyResources;
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

	public static final ImageSmileyAdapter fromActivity(MainActivity activity){
		List<Drawable> dlist = new ArrayList<Drawable>();
		List<String> slist = new ArrayList<String>();
		
		List<SmileyResources> smileys = new ArrayList<SmileyResources>(activity.getAdditionalSmileys());
		for (int k=smileys.size()-1; k>=0; k--) {
			SmileyResources smr = smileys.get(k);
			try {
				Resources res = smr.getNativeResourcesForProtocol(activity.getPackageManager());
				for (int i = 0; i < smr.getDrawableIDs().length; i++) {
					
					boolean found = false;
					for (int j=0; j<slist.size(); j++) {
						if (smr.getNames()[i].equals(slist.get(j))) {
							found = true;
							break;
						}
					}
					
					if (!found) {
						dlist.add(res.getDrawable(smr.getDrawableIDs()[i]));
						slist.add(smr.getNames()[i]);
					}
				}
			} catch (Exception e) {
				Logger.log(e);
			}
		}
		
		return new ImageSmileyAdapter(activity, dlist, slist);
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
