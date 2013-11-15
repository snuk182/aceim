package aceim.app.widgets.pickers;

import aceim.api.dataentity.tkv.TKV;
import aceim.api.utils.Logger;

import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.service.ServiceUtils;
import aceim.app.utils.ViewUtils;
import android.content.Intent;
import android.view.View;

public class FilePickerListener extends PickerListenerBase {

	public FilePickerListener(TKV tkv, ValuePickedListener listener, MainActivity activity) {
		super(tkv, listener, activity);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("file/*");
		try {
			activity.startActivityForResult(intent, ServiceUtils.getRequestCodeForActivity(tkv.getKey().hashCode()));
		} catch (Exception e) {
			Logger.log(e);
			ViewUtils.showAlertToast(activity, android.R.drawable.ic_menu_info_details, R.string.no_app_for_picking_file_found, null);
		}
	}
}
