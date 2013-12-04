package aceim.app.view.page.about;

import java.util.List;

import aceim.api.utils.Logger;
import aceim.app.R;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
			List<ProtocolResources> resources = getMainActivity().getCoreService().getAllProtocolResources(true);
			
			AQuery aq = new AQuery(v);
			aq.id(R.id.list).adapter(new ProtocolAdapter(getMainActivity(), resources));
			aq.id(R.id.version).text(getMainActivity().getString(R.string.version_X, getMainActivity().getString(R.string.version)));
			aq.id(R.id.apiVersion).text(getMainActivity().getString(R.string.api_version_X, getMainActivity().getString(aceim.api.R.string.api_version)));
			
			spanUrlsInInfo(v);
			
		} catch (RemoteException e) {
			Logger.log(e);
		}
		
		return v;
	}

	private static void spanUrlsInInfo(View v) {
		TextView info = (TextView) v.findViewById(R.id.info);
		if (info != null) {
			MovementMethod mm = info.getMovementMethod();
	        if (!(mm instanceof LinkMovementMethod))
	        {
	             info.setMovementMethod(LinkMovementMethod.getInstance());
	        }
	        
	        ViewUtils.spanKnownUrls(info.getEditableText(), info.getText().toString());
		}
	}

	private final class ProtocolAdapter extends ArrayAdapter<ProtocolResources> {

		public ProtocolAdapter(Context context, List<ProtocolResources> objects) {
			super(context, R.layout.about_protocol_item, R.id.label, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			
			ProtocolResources r = getItem(position);
			
			AQuery aq = new AQuery(view);
			aq.id(R.id.label).text(r.toString());
			aq.id(R.id.info).text(r.getProtocolInfo());
			aq.id(R.id.id).text(r.getProtocolServicePackageName());
			aq.id(R.id.version).text(getMainActivity().getString(R.string.version_X, r.getProtocolVersion()));
			aq.id(R.id.apiVersion).text(getMainActivity().getString(R.string.api_version_X, r.getApiVersion()));
			
			spanUrlsInInfo(view);
			
			return view;
		}
	}
}
