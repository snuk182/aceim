package aceim.app.view.page.about;

import java.util.ArrayList;
import java.util.List;

import aceim.api.utils.Logger;
import aceim.app.R;
import aceim.app.dataentity.PluginResources;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.view.page.Page;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.androidquery.AQuery;

public class About extends Page {

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_agenda);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.about);
	}

	@Override
	protected View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View v = inflater.inflate(R.layout.about, null);
		try {
			List<PluginResources> resources = new ArrayList<PluginResources>();
			
			resources.addAll(getMainActivity().getCoreService().getAllProtocolResources(true));
			resources.addAll(getMainActivity().getSmileysManager().getThirdPartySmileys());
			
			AQuery aq = new AQuery(v);
			aq.id(R.id.list).adapter(new ProtocolAdapter(getMainActivity(), resources));
			aq.id(R.id.version).text(getMainActivity().getString(R.string.version_X, getMainActivity().getString(R.string.version)));
			aq.id(R.id.apiVersion).text(getMainActivity().getString(R.string.api_version_X, getMainActivity().getString(aceim.api.R.string.api_version)));
			
		} catch (RemoteException e) {
			Logger.log(e);
		}
		
		return v;
	}

	private final class ProtocolAdapter extends ArrayAdapter<PluginResources> {

		public ProtocolAdapter(Context context, List<PluginResources> objects) {
			super(context, R.layout.about_plugin_item, R.id.label, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			
			PluginResources r = getItem(position);
			
			AQuery aq = new AQuery(view);
			aq.id(R.id.label).text(r.toString());
			aq.id(R.id.id).text(r.getPackageId());
			
			String info = r.getInfo(getContext());
			
			if (TextUtils.isEmpty(info)) {
				aq.id(R.id.info).gone();
			} else {
				aq.id(R.id.info).text(info);
			}
			
			if (r instanceof ProtocolResources) {
				ProtocolResources rr = (ProtocolResources) r;
				aq.id(R.id.version).text(getMainActivity().getString(R.string.version_X, rr.getProtocolVersion()));
				aq.id(R.id.apiVersion).text(getMainActivity().getString(R.string.api_version_X, rr.getApiVersion()));
			} else {
				aq.id(R.id.version).gone();
				aq.id(R.id.apiVersion).gone();
			}
			
			return view;
		}
	}
}
