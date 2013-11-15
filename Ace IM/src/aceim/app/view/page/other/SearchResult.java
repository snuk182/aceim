package aceim.app.view.page.other;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.PersonalInfo;
import aceim.api.service.ApiConstants;

import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.utils.DialogUtils;
import aceim.app.view.page.Page;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SearchResult extends Page {
	
	private final Account mAccount;
	private final List<PersonalInfo> mInfos;
	
	private ListView mList;

	public SearchResult(Account account, List<PersonalInfo> mInfos) {
		this.mAccount = account;
		this.mInfos = mInfos;
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(R.drawable.ic_menu_friendslist);
	}

	@Override
	public String getTitle(Context context) {
		return context.getString(R.string.search_result);
	}

	@Override
	protected View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		View view = inflater.inflate(R.layout.search_result_list, null);
		mList = (ListView) view.findViewById(R.id.list);
		mList.setAdapter(new PersonalInfoAdapter(getMainActivity()));
		return view;
	}
	
	private class PersonalInfoAdapter extends ArrayAdapter<PersonalInfo> {

		public PersonalInfoAdapter(Context context) {
			super(context, R.layout.contact_list_plain_item, R.id.username, mInfos);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			
			if (convertView == null) {
				view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list_plain_item, null);
			} else {
				view = convertView;
			}
			
			fillView(view, getItem(position));
			
			return view;
		}

		private void fillView(View container, final PersonalInfo item) {
			ImageView icon = (ImageView) container.findViewById(R.id.image_icon);
			ImageView status = (ImageView) container.findViewById(R.id.image_status);
			ImageView extraImage1 = (ImageView) container.findViewById(R.id.image_extra_1);
			ImageView extraImage2 = (ImageView) container.findViewById(R.id.image_extra_2);
			ImageView extraImage3 = (ImageView) container.findViewById(R.id.image_extra_3);
			ImageView extraImage4 = (ImageView) container.findViewById(R.id.image_extra_4);
			TextView xstatusLabel = (TextView) container.findViewById(R.id.label_xstatus);
			final TextView username = (TextView) container.findViewById(R.id.username);
			
			boolean buddyManagementAvailable = mAccount.getOnlineInfo().getFeatures().getBoolean(ApiConstants.FEATURE_BUDDY_MANAGEMENT, false);
			status.setVisibility(buddyManagementAvailable ? View.VISIBLE : View.GONE);
			
			if (buddyManagementAvailable) {
				status.setClickable(true);
				status.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Buddy buddy = new Buddy(item.getProtocolUid(), mAccount.getProtocolUid(), mAccount.getProtocolName(), mAccount.getServiceId());
						buddy.setGroupId(ApiConstants.NOT_IN_LIST_GROUP_ID);
						buddy.setName(username.getText().toString());
						
						if (item.isMultichat()) {
							
						} else {
							DialogUtils.showAddBuddyDialog(buddy, mAccount, getMainActivity());
						}
					}
				});
			} else {
				status.setClickable(false);
			}
			
			ImageView[] images = new ImageView[] { extraImage1, extraImage2, extraImage3, extraImage4 };
			
			if (item.getProperties().containsKey(PersonalInfo.INFO_NICK)) {
				xstatusLabel.setText(item.getProtocolUid());
			} 
			
			username.setText(item.getProperties().getString(PersonalInfo.INFO_NICK, item.getProtocolUid()));
			
			if (item.getProperties().containsKey(PersonalInfo.INFO_ICON)) {
				byte[] bytes = item.getProperties().getByteArray(PersonalInfo.INFO_ICON);
				Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
				icon.setImageBitmap(b);
				icon.setVisibility(View.VISIBLE);
			} else {
				icon.setVisibility(View.GONE);
			}
			
			int imagesIndex = 0;
			
			//TODO generalize
			if (item.getProperties().containsKey(PersonalInfo.INFO_GENDER)) {
				String gender = item.getProperties().getString(PersonalInfo.INFO_GENDER, "");
				
				if (gender.equalsIgnoreCase("male")) {
					extraImage1.setImageResource(R.drawable.male);
					imagesIndex++;
				} else if (gender.equalsIgnoreCase("female")) {
					extraImage1.setImageResource(R.drawable.female);
					imagesIndex++;
				} 
			}
			
			for (int i = imagesIndex; i < images.length; i++) {
				if (images[i] != null) {
					images[i].setVisibility(View.GONE);
				}
			}
			
			int pad = getContext().getResources().getDimensionPixelSize(R.dimen.default_padding);
			container.setPadding(pad, pad, pad, 0);
			
			container.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Page.getPersonalInfoPage(getMainActivity().getScreen(), item);
				}
			});
		}
	}
}
