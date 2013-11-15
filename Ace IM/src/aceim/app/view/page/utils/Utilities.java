package aceim.app.view.page.utils;

import java.util.Arrays;
import java.util.List;

import aceim.api.dataentity.FileProgress;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.ActivityResult;
import aceim.app.dataentity.listeners.IHasFilePicker;
import aceim.app.dataentity.listeners.IHasFileProgress;
import aceim.app.view.page.Page;
import aceim.app.widgets.bottombar.BottomBarButton;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Utilities extends Page implements  IHasFilePicker, IHasFileProgress{
	
	private static Util[] UTILS;
	
	private UtilitiesAdapter mAdapter;
	
	public Utilities() {}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_myplaces);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.utils);
	}

	@Override
	protected View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.utils, null);
		
		UTILS = new Util[]{new AccountImporter(getMainActivity())};
		
		BottomBarButton closeBtn = (BottomBarButton) view.findViewById(R.id.cancel);
		closeBtn.setOnClickListener(mRemoveMeClickListener);
		
		mAdapter = new UtilitiesAdapter(getMainActivity(), Arrays.asList(UTILS));
		
		ListView list = (ListView) view.findViewById(R.id.list);
		list.setAdapter(mAdapter);
		
		return view;
	}
	
	interface Util {
		
		View getView(LayoutInflater inflater);
	}

	private class UtilitiesAdapter extends ArrayAdapter<Util> {

		public UtilitiesAdapter(Context context, List<Util> objects) {
			super(context, android.R.id.title, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			
			Util u = getItem(position);
			
			if (convertView != null && convertView.getTag() == u) {
				view = convertView;
			} else {
				view = constructFromUtil(u);
			}
			
			view.setTag(u);
			
			return view;
		}

		private View constructFromUtil(Util u) {
			return u.getView(LayoutInflater.from(getContext()));
		}
	}

	@Override
	public void onFileProgress(FileProgress progress) {
		if (progress.getServiceId() > -1) {
			return;
		}
		
		for (Util u : UTILS) {
			if (u instanceof IHasFileProgress) {
				((IHasFileProgress)u).onFileProgress(progress);
				mAdapter.notifyDataSetInvalidated();
			}
		}
	}

	@Override
	public void onFilePicked(ActivityResult result, MainActivity activity) {
		for (Util u : UTILS) {
			if (u instanceof IHasFilePicker) {
				((IHasFilePicker)u).onFilePicked(result, activity);
				mAdapter.notifyDataSetInvalidated();
			}
		}
	} 
}
