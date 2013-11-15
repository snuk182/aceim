package aceim.app.widgets.pickers;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import aceim.api.dataentity.tkv.TKV;

import aceim.app.MainActivity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.widget.TimePicker;

public class TimePickerListener extends CalendarPickerListenerBase {

	public TimePickerListener(TKV tkv, ValuePickedListener listener, MainActivity activity) {
		super(tkv, listener, activity, SimpleDateFormat.getTimeInstance());
	}

	@Override
	protected AlertDialog createDialog(final Calendar cal) {
		OnTimeSetListener callback = new OnTimeSetListener() {

			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
				cal.set(Calendar.MINUTE, minute);
				listener.onValuePicked(String.valueOf(cal.getTimeInMillis()));
			}
		};
		TimePickerDialog dialog = new TimePickerDialog(activity, callback, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(activity));
		return dialog;
	}
}
