package aceim.app.widgets.pickers;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import aceim.api.dataentity.tkv.TKV;
import aceim.app.MainActivity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.widget.DatePicker;

public class DatePickerListener extends CalendarPickerListenerBase {

	public DatePickerListener(TKV tkv, ValuePickedListener listener, MainActivity activity) {
		super(tkv, listener, activity, SimpleDateFormat.getDateInstance());
	}

	@Override
	protected AlertDialog createDialog(final Calendar cal) {
		OnDateSetListener callback = new OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, monthOfYear);
				cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				listener.onValuePicked(String.valueOf(cal.getTimeInMillis()));
			}
		};

		DatePickerDialog dialog = new DatePickerDialog(activity, callback, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		return dialog;
	}
}
