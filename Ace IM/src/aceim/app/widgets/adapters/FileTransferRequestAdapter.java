package aceim.app.widgets.adapters;

import java.util.List;

import aceim.api.dataentity.FileInfo;
import aceim.app.R;
import aceim.app.utils.ViewUtils;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileTransferRequestAdapter extends ArrayAdapter<FileInfo> {

	public FileTransferRequestAdapter(Context context, List<FileInfo> objects) {
		super(context, 0, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		TextView view;
		
		if (convertView != null) {
			view = (TextView) convertView;
		} else {
			view = new TextView(getContext());
		}
		
		FileInfo info = getItem(position);
		view.setText(getContext().getString(R.string.file_transfer_request_format, info.getFilename(), ViewUtils.humanReadableByteCount(info.getSize(), true)));
		
		return view;
	}
}
