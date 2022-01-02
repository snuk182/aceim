package aceim.app.view.page.inputform;

import java.io.File;
import java.util.Calendar;

import aceim.api.dataentity.InputFormFeature;
import aceim.api.dataentity.OnlineInfo;
import aceim.api.dataentity.tkv.FileTKV;
import aceim.api.dataentity.tkv.ListTKV;
import aceim.api.dataentity.tkv.StringTKV;
import aceim.api.dataentity.tkv.TKV;
import aceim.api.dataentity.tkv.ToggleTKV;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.ActivityResult;
import aceim.app.dataentity.ProtocolResources;
import aceim.app.dataentity.listeners.IHasFilePicker;
import aceim.app.service.ServiceUtils;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.Page;
import aceim.app.widgets.bottombar.BottomBarButton;
import aceim.app.widgets.pickers.CalendarPickerListenerBase;
import aceim.app.widgets.pickers.DatePickerListener;
import aceim.app.widgets.pickers.FilePickerListener;
import aceim.app.widgets.pickers.PickerListenerBase.ValuePickedListener;
import aceim.app.widgets.pickers.TimePickerListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

@SuppressLint("InlinedApi")
public class InputFormFeaturePage extends Page implements IHasFilePicker {

	private static final LayoutParams LIST_PARAMS = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

	private final InputFormFeature mFeature;
	private final OnlineInfo mInfo;
	private final ProtocolResources mProtocolResources;
	
	private BottomBarButton mRunBtn;
	private LinearLayout mContainer;

	private OnClickListener mRunClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View clicked) {
			
			boolean areMandatoryFieldsFilled = true;
			
			for (int i = 0; i < mFeature.getEditorFields().length; i++) {
				TKV tkv = mFeature.getEditorFields()[i];
				
				View v = mContainer.getChildAt(i).findViewById(R.id.value);
				
				if (v instanceof CheckBox) {
					tkv.setValue(Boolean.toString(((CheckBox)v).isChecked()));	
				} else if (v instanceof Spinner) {
					tkv.setValue(((Spinner)v).getSelectedItem().toString());	
				} else {
					//FileTKV value is already filled
					if (!(tkv instanceof FileTKV)) {
						tkv.setValue(((TextView)v).getText().toString().trim());
					}					
				}
				
				if (!checkValueMandatory(tkv, v)) {
					v.setBackgroundResource(R.drawable.criteria_bad);
					areMandatoryFieldsFilled = false;
				} 
			}
			
			if (!areMandatoryFieldsFilled) {
				ViewUtils.showAlertToast(getMainActivity(), android.R.drawable.ic_dialog_alert, R.string.unfilled_mandatory_warning, null);
				return;
			}
			
			mInfo.getFeatures().putParcelableArray(mFeature.getFeatureId(), mFeature.getEditorFields());
			
			try {
				getMainActivity().getCoreService().setFeature(mFeature.getFeatureId(), mInfo);
				removeMe();
			} catch (RemoteException e) {
				getMainActivity().onRemoteException(e);
			}
		}
	};
	
	public InputFormFeaturePage(InputFormFeature feature, OnlineInfo info, ProtocolResources mProtocolResources) {
		this.mFeature = feature;
		this.mInfo = info;
		this.mProtocolResources = mProtocolResources;
	}

	@Override
	public Drawable getIcon(Context context) {
		try {
			return mProtocolResources.getNativeResourcesForProtocol(context.getPackageManager()).getDrawable(mFeature.getIconId());
		} catch (Exception e) {
			return context.getResources().getDrawable(R.drawable.logo_corner_small);
		}
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.default_key_value_format, mInfo.getProtocolUid(), mFeature.getFeatureName());
	}

	@Override
	protected View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.input_form, null);
		mContainer = (LinearLayout) view.findViewById(R.id.container);
		mRunBtn = (BottomBarButton) view.findViewById(R.id.run);
		
		mRunBtn.setOnClickListener(mRunClickListener);
		
		for (TKV field: mFeature.getEditorFields()) {
			processFormField(inflater, field);
		}
		
		return view;
	}

	private void processFormField(LayoutInflater inflater, TKV field) {
		View item;

		if (field instanceof FileTKV) {
			item = fillDialogItem(inflater, field, new FilePickerListener(field, new InputFormFieldValueListener(field), getMainActivity()));
		} else if (field instanceof ToggleTKV) {
			item = inflater.inflate(R.layout.options_item_checkbox, null);
			CheckBox cb = (CheckBox) item.findViewById(R.id.value);
			if (field.getValue() != null) {
				cb.setChecked(Boolean.parseBoolean(field.getValue()));
			} 
		} else if (field instanceof ListTKV) {
			item = inflater.inflate(R.layout.options_item_list, null);
			
			Spinner spinner = (Spinner) item.findViewById(R.id.value);
			ListTKV list = (ListTKV) field;

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getMainActivity(), android.R.layout.simple_spinner_item, android.R.id.text1, list.getChoices());
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			spinner.setAdapter(adapter);
			
			for (int i=0; i<list.getChoices().length; i++) {
				if (list.getChoices()[i].equals(list.getValue())) {
					spinner.setSelection(i);
					break;
				}
			}
		} else if (field instanceof StringTKV) {
			StringTKV tkv = (StringTKV) field;
			switch (tkv.getContentType()) {
			case DATE:
				item = fillDialogItem(inflater, field, new DatePickerListener(field, new InputFormFieldValueListener(field), getMainActivity()));
				break;
			case TIME:
				item = fillDialogItem(inflater, field, new TimePickerListener(field, new InputFormFieldValueListener(field), getMainActivity()));
				break;
			/*case CHECKBOX:
				item = inflater.inflate(R.layout.options_item_checkbox, null);
				CheckBox cb = (CheckBox) item.findViewById(R.id.value);
				boolean b;
				if (field.getValue() != null) {
					b = Boolean.parseBoolean(field.getValue());
				} else {
					b = false;
				}
				cb.setChecked(b);
				break;*/
			case DOUBLE:
				item = fillEditTextItem(R.layout.options_item_double, inflater, field);
				break;
			case INTEGER:
				item = fillEditTextItem(R.layout.options_item_integer, inflater, field);
				break;
			case PASSWORD:
				item = fillEditTextItem(R.layout.options_item_password, inflater, field);
				break;
			case STRING:
				item = fillEditTextItem(R.layout.options_item_text, inflater, field);
				break;
			default:
				item = fillDialogItem(inflater, field, null);	
				break;
			}
		} else {
			item = fillDialogItem(inflater, field, null);				
		}
		
		((TextView) item.findViewById(R.id.label)).setText(field.getKey());
		item.setLayoutParams(LIST_PARAMS);
		item.setTag(field.getKey());
		mContainer.addView(item, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	}
	
	private View fillDialogItem(LayoutInflater inflater, TKV field, OnClickListener clickListener) {
		View item = inflater.inflate(R.layout.options_item, null);
		ImageButton editBtn = (ImageButton) item.findViewById(R.id.edit);
		editBtn.setImageResource(android.R.drawable.ic_menu_edit);
		if (clickListener != null) {
			editBtn.setOnClickListener(clickListener);
		} else {
			editBtn.setVisibility(View.INVISIBLE);
		}

		TextView tvalue = (TextView) item.findViewById(R.id.value);
		if (field.getValue() != null) {
			if (field instanceof FileTKV) {
				setFileItemLabelValue(item, field.getValue());
			} else if (field instanceof StringTKV) {
				StringTKV tkv = (StringTKV) field;
				switch (tkv.getContentType()) {
				case DATE:
				case TIME:
					CalendarPickerListenerBase l = (CalendarPickerListenerBase) clickListener;
					long timeMillis = Long.parseLong(field.getValue());
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(timeMillis);
					tvalue.setText(l.getFormat().format(cal.getTime()));
					break;
				default:
					tvalue.setText(field.getValue());
					break;
				}
			} else if (field instanceof ListTKV) {
				//TODO
			}
		}

		return item;
	}

	private boolean checkValueMandatory(TKV tkv, View v) {
		return !tkv.isMandatory() || !TextUtils.isEmpty(tkv.getValue());
	}

	private View fillEditTextItem(int layoutId, LayoutInflater inflater, final TKV field) {
		View item = inflater.inflate(layoutId, null);
		final EditText et = ((EditText) item.findViewById(R.id.value));
		String value;
		if (field.getValue() != null) {
			value = field.getValue();
		} else {
			value = null;
		}
		et.setText(value);
		et.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				field.setValue(et.getText().toString());
			}
		});

		return item;
	}

	private static void setFileItemLabelValue(View item, String value) {
		TextView tvalue = (TextView) item.findViewById(R.id.value);
		if (value != null) {
			tvalue.setText(new File(value).getName());
		} else {
			tvalue.setText(null);
		}
	}

	@Override
	public void onFilePicked(ActivityResult result, MainActivity activity) {
		switch (result.getResultCode()) {
		case Activity.RESULT_OK:
			for (int i = 0; i < mFeature.getEditorFields().length; i++) {
				TKV tkv = mFeature.getEditorFields()[i];
				if (ServiceUtils.getRequestCodeForActivity(tkv.getKey().hashCode()) == result.getRequestCode()) {
					LinearLayout container = (LinearLayout) getView().findViewById(R.id.container);
					View item = container.getChildAt(i);
					String filePath = result.getData().getData().getPath();

					tkv.setValue(filePath);

					setFileItemLabelValue(item, filePath);
				}
			}
			break;
		}
	}
	
	private final class InputFormFieldValueListener implements ValuePickedListener {
		
		private final TKV tkv;

		private InputFormFieldValueListener(TKV tkv) {
			this.tkv = tkv;
		}

		@Override
		public void onValuePicked(String value) {
			this.tkv.setValue(value);
		}
		
	}

	/**
	 * @return the mInfo
	 */
	public OnlineInfo getOnlineInfo() {
		return mInfo;
	}
	
	@Override
	public String getPageId(){
		return Page.getPageIdForInputFormPage(mFeature, mInfo);
	}
}
