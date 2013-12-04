package aceim.app.view.page.accounteditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import aceim.api.dataentity.ProtocolOption;
import aceim.api.dataentity.tkv.FileTKV;
import aceim.api.dataentity.tkv.ListTKV;
import aceim.api.dataentity.tkv.StringTKV;
import aceim.api.dataentity.tkv.ToggleTKV;
import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.app.AceImException;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

@SuppressLint("InlinedApi")
public class AccountEditor extends Page implements IHasFilePicker {

	private static final LayoutParams LIST_PARAMS = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

	private ArrayAdapter<ProtocolResources> mAdapter;
	private ProtocolResources mSelectedResources;
	private final Account mAccount;

	private List<ProtocolOption> mOptions;

	private boolean mHasUnsavedChanges = false;
	
	private final OnClickListener mCancelClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final MainActivity activity = (MainActivity) getMainActivity();
			
			if (mHasUnsavedChanges) {
				final AlertDialog dialog = new AlertDialog.Builder(activity)
						.setTitle(R.string.unsaved_data)
						.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mSaveClickListener.onClick(null);
								dialog.dismiss();
							}
						})
						.setNegativeButton(R.string.dont_save, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								removeMe();
							}
						})
						.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
					.create();
				dialog.show();
			} else {
				removeMe();
			}		
		}
	};

	private final OnClickListener mSaveClickListener = new OnClickListener() {

		@Override
		public void onClick(View clicked) {
			MainActivity activity = (MainActivity) getMainActivity();

			//if protocol still not chosen
			if (mOptions == null) {
				return;
			}

			final LinearLayout container = (LinearLayout) getView().findViewById(R.id.container);
			boolean areMandatoryFieldsFilled = true;
			
			for (int i = 0; i < mOptions.size(); i++) {
				ProtocolOption o = mOptions.get(i);
				View v = container.getChildAt(i);
				if (!checkValueMandatory(o, v)) {
					v.setBackgroundResource(R.drawable.criteria_bad);
					areMandatoryFieldsFilled = false;
				} else {
					v.setBackgroundResource(0);
					if (o.getValue() == null) {
						o.setValue(o.getDefaultValue());
					}
				}
			}
			
			if (!areMandatoryFieldsFilled) {
				ViewUtils.showAlertToast(activity, android.R.drawable.ic_dialog_alert, R.string.unfilled_mandatory_warning, null);
				return;
			}
			
			try {
				if (mAccount == null) {
					Account account = activity.getCoreService().createAccount(mSelectedResources.getProtocolServicePackageName(), mOptions);
					if (account == null) {
						Logger.log("Cannot create account #" + mOptions.get(0).getValue(), LoggerLevel.INFO);
						return;
					} else {
						activity.accountAdded(account);
					}
				} else {
					activity.getCoreService().editAccount(mAccount, mOptions, mSelectedResources.getProtocolServicePackageName());
				}
			} catch (RemoteException e) {
				activity.onRemoteException(e);
			}
			
			removeMe();
		}
	};

	private void saveValueToToption(String value, ProtocolOption option) {
		if (value.equals(option.getValue())) {
			return;
		}

		mHasUnsavedChanges = true;
		option.setValue(value);
	}

	private boolean checkValueMandatory(ProtocolOption o, View v) {
		return !o.isMandatory() || !TextUtils.isEmpty(o.getValue());
	}

	public AccountEditor(Account account) {
		this.mAccount = account;
	}

	@Override
	public View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		MainActivity activity = (MainActivity) getMainActivity();
		Collection<ProtocolResources> resources = obtainResourcesForEditor();
		this.mAdapter = new ArrayAdapter<ProtocolResources>(activity.getApplicationContext(), android.R.layout.simple_spinner_item, resources.toArray(new ProtocolResources[resources.size()]));
		this.mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		View view = inflater.inflate(R.layout.account_editor, group, false);

		final LinearLayout container = (LinearLayout) view.findViewById(R.id.container);

		Spinner protocolSpinner = (Spinner) view.findViewById(R.id.protocols);
		protocolSpinner.setAdapter(mAdapter);
		protocolSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mSelectedResources = (ProtocolResources) parent.getItemAtPosition(position);
				constructEditor(container);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				container.removeAllViews();
			}
		});

		if (mAccount != null) {

			for (int i = 0; i < mAdapter.getCount(); i++) {
				if (mAdapter.getItem(i).getProtocolServicePackageName().equals(mAccount.getProtocolServicePackageName())) {
					protocolSpinner.setSelection(i);
					break;
				}
			}

			protocolSpinner.setEnabled(mAccount.getProtocolServicePackageName() == null);
		}

		BottomBarButton cancelBtn = (BottomBarButton) view.findViewById(R.id.cancel);
		BottomBarButton saveBtn = (BottomBarButton) view.findViewById(R.id.save);

		cancelBtn.setOnClickListener(mCancelClickListener);
		saveBtn.setOnClickListener(mSaveClickListener);

		return view;
	}

	private Collection<ProtocolResources> obtainResourcesForEditor() {
		List<ProtocolResources> res = new ArrayList<ProtocolResources>();
		for (ProtocolResources r : getMainActivity().getProtocolResources().values()) {
			if (mAccount != null && !r.getProtocolName().equals(mAccount.getProtocolName())) {
				continue;
			}
			
			res.add(r);
		}
		
		if (res.size() > 0) {
			return res;
		} else {
			return getMainActivity().getProtocolResources().values();
		}
	}

	private void constructEditor(LinearLayout container) {
		container.removeAllViews();

		MainActivity activity = (MainActivity) getMainActivity();
		Resources resources;
		try {
			resources = mSelectedResources.getNativeResourcesForProtocol(getMainActivity().getPackageManager());
			mOptions = activity.getCoreService().getProtocolOptions(mSelectedResources.getProtocolServicePackageName(), mAccount!=null ? mAccount.getServiceId() : (byte)-1);
		} catch (RemoteException e) {
			activity.onRemoteException(e);
			return;
		} catch (AceImException e) {
			Logger.log(e);
			return;
		}
		
		LayoutInflater inflater = LayoutInflater.from(activity);
		
		if (mAccount != null) {
			if (mOptions.size() > 0 && TextUtils.isEmpty(mOptions.get(0).getValue())) {
				mOptions.get(0).setValue(mAccount.getProtocolUid());
			}
		}

		for (ProtocolOption o : mOptions) {
			View item;

			if (o.getTkv() instanceof FileTKV) {
				item = fillDialogItem(inflater, o, new FilePickerListener(o.getTkv(), new ProtocolOptionValuePickedListener(o), getMainActivity()));
			} else if (o.getTkv() instanceof ListTKV) {
				item = inflater.inflate(R.layout.options_item_list, null);
				
				Spinner spinner = (Spinner) item.findViewById(R.id.value);
				ListTKV list = (ListTKV) o.getTkv();

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(getMainActivity(), android.R.layout.simple_spinner_item, android.R.id.text1, list.getChoices());
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
				spinner.setAdapter(adapter);
				
				for (int i=0; i<list.getChoices().length; i++) {
					if (list.getChoices()[i].equals(list.getValue())) {
						spinner.setSelection(i);
						break;
					}
				}
				
				spinner.setEnabled(mAccount == null || mOptions.get(0) != o);
			} else if (o.getTkv() instanceof ToggleTKV) {
				item = inflater.inflate(R.layout.options_item_checkbox, null);
				CheckBox cb = (CheckBox) item.findViewById(R.id.value);
				boolean b;
				if (o.getValue() != null) {
					b = Boolean.parseBoolean(o.getValue());
				} else {
					b = Boolean.parseBoolean(o.getDefaultValue());
					o.setValue(o.getDefaultValue());
				}
				cb.setChecked(b);
				
				cb.setEnabled(mOptions.get(0) != o);
			} else if (o.getTkv() instanceof StringTKV) {
				StringTKV tkv = (StringTKV) o.getTkv();
				switch (tkv.getContentType()) {
				case DATE:
					item = fillDialogItem(inflater, o, new DatePickerListener(o.getTkv(), new ProtocolOptionValuePickedListener(o), getMainActivity()));
					break;
				case TIME:
					item = fillDialogItem(inflater, o, new TimePickerListener(o.getTkv(), new ProtocolOptionValuePickedListener(o), getMainActivity()));
					break;
				/*case CHECKBOX:
					item = inflater.inflate(R.layout.options_item_checkbox, null);
					CheckBox cb = (CheckBox) item.findViewById(R.id.value);
					boolean b;
					if (o.getValue() != null) {
						b = Boolean.parseBoolean(o.getValue());
					} else {
						b = Boolean.parseBoolean(o.getDefaultValue());
						o.setValue(o.getDefaultValue());
					}
					cb.setChecked(b);
					break;*/
				case DOUBLE:
					item = fillEditTextItem(R.layout.options_item_double, inflater, o);
					break;
				case INTEGER:
					item = fillEditTextItem(R.layout.options_item_integer, inflater, o);
					break;
				case PASSWORD:
					item = fillEditTextItem(R.layout.options_item_password, inflater, o);
					break;
				case STRING:
					item = fillEditTextItem(R.layout.options_item_text, inflater, o);
					break;
				default:
					item = fillDialogItem(inflater, o, null);	
					break;
				}
			} else {
				item = fillDialogItem(inflater, o, null);				
			}
			
			((TextView) item.findViewById(R.id.label)).setText(resources.getString(o.getLabelId()));
			item.setTag(o.getKey());
			container.addView(item, LIST_PARAMS);
		}
	}

	private View fillDialogItem(LayoutInflater inflater, ProtocolOption o, OnClickListener clickListener) {
		View item = inflater.inflate(R.layout.options_item, null);
		ImageButton editBtn = (ImageButton) item.findViewById(R.id.edit);
		editBtn.setImageResource(android.R.drawable.ic_menu_edit);
		if (clickListener != null && (mAccount == null || mOptions.get(0) != o)) {
			editBtn.setOnClickListener(clickListener);
		} else {
			editBtn.setVisibility(View.INVISIBLE);
		}

		TextView tvalue = (TextView) item.findViewById(R.id.value);
		if (o.getValue() != null) {
			if (o.getTkv() instanceof FileTKV) {
				setFileItemLabelValue(item, o.getValue());
			} else if (o.getTkv() instanceof StringTKV) {
				StringTKV tkv = (StringTKV) o.getTkv();
				switch (tkv.getContentType()) {
				case DATE:
				case TIME:
					CalendarPickerListenerBase l = (CalendarPickerListenerBase) clickListener;
					long timeMillis = Long.parseLong(o.getValue());
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(timeMillis);
					tvalue.setText(l.getFormat().format(cal.getTime()));
					break;
				default:
					tvalue.setText(o.getValue());
					break;
				}
			} else if (o.getTkv() instanceof ListTKV) {
				//TODO
			}
		}

		return item;
	}

	private View fillEditTextItem(int layoutId, LayoutInflater inflater, final ProtocolOption o) {
		View item = inflater.inflate(layoutId, null);
		final EditText et = ((EditText) item.findViewById(R.id.value));
		String value;
		if (o.getValue() != null) {
			value = o.getValue();
		} else if (o.getDefaultValue() != null) {
			value = o.getDefaultValue();
			o.setValue(o.getDefaultValue());
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
				o.setValue(et.getText().toString());
				mHasUnsavedChanges = true;
			}
		});
		
		et.setEnabled(mAccount == null || mOptions.get(0) != o);

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
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.ic_menu_manage);
	}

	@Override
	public String getTitle(Context context) {
		if (mAccount != null) {
			return mAccount.getSafeName();
		} else {
			return context.getResources().getString(R.string.new_account);
		}
	}
	
	@Override
	public String getPageId(){
		return getClass().getSimpleName() + (mAccount != null ? (" " + mAccount.getAccountId()) : "");
	}

	@Override
	public void onFilePicked(ActivityResult result, MainActivity activity) {
		switch (result.getResultCode()) {
		case Activity.RESULT_OK:
			for (int i = 0; i < mOptions.size(); i++) {
				ProtocolOption o = mOptions.get(i);
				if (ServiceUtils.getRequestCodeForActivity(o.getKey().hashCode()) == result.getRequestCode()) {
					LinearLayout container = (LinearLayout) getView().findViewById(R.id.container);
					View item = container.getChildAt(i);
					String filePath = result.getData().getData().getPath();

					saveValueToToption(filePath, o);
					setFileItemLabelValue(item, filePath);
					
					break;
				}
			}
			break;
		}
	}
	
	private final class ProtocolOptionValuePickedListener implements ValuePickedListener {
		
		private final ProtocolOption option;

		private ProtocolOptionValuePickedListener(ProtocolOption option) {
			this.option = option;
		}
		
		@Override
		public void onValuePicked(String value) {
			saveValueToToption(value, option);
		}
	}
}
