package aceim.app.widgets.adapters;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.MultiChatRoom;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.dataentity.tkv.MessageAttachment;
import aceim.api.dataentity.tkv.MessageAttachment.MessageAttachmentType;
import aceim.api.utils.Logger;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.themeable.dataentity.ChatMessageItemThemeResource;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.chat.ChatMessageHolder;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.BitmapAjaxCallback;

public class MessagesAdapter extends ArrayAdapter<ChatMessageHolder> {
	
	private static SortedMap<String, Drawable> sSmileys;
	private static int sSmileyBound; 
	
	private DateFormat mDateFormat;
	private DateFormat mTimeFormat;
	
	private boolean mDontDrawSmilies = false;
	
	private Object copyModeStarter = null;
	private AQuery mAq = null;
	
	private final Account mAccount;
	private final Buddy mBuddy;
	
	private final ChatMessageItemThemeResource messageItemLayout;
	
	private final OnClickListener mCheckForCopyClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mAq.recycle(v).id(messageItemLayout.getCheckboxId()).checked(true);
		}
	};
	
	public MessagesAdapter(MainActivity activity, Account account, Buddy buddy, ChatMessageItemThemeResource messageItemLayout) {
		super(activity, 0, 0);		
		this.messageItemLayout = messageItemLayout;
		this.mAccount = account;
		this.mBuddy = buddy;
		init(activity);
	}

	public MessagesAdapter(MainActivity activity, Account account, Buddy buddy, ChatMessageItemThemeResource messageItemLayout, ChatMessageHolder[] objects) {
		super(activity, 0, 0, objects);
		this.messageItemLayout = messageItemLayout;
		this.mAccount = account;
		this.mBuddy = buddy;
		init(activity);
	}

	public MessagesAdapter(MainActivity activity, Account account, Buddy buddy, ChatMessageItemThemeResource messageItemLayout, List<ChatMessageHolder> objects) {
		super(activity, 0, 0, objects);
		this.messageItemLayout = messageItemLayout;
		this.mAccount = account;
		this.mBuddy = buddy;
		init(activity);
	}
	
	private void init(MainActivity activity){
		mDateFormat = android.text.format.DateFormat.getLongDateFormat(activity);
		mTimeFormat = android.text.format.DateFormat.getTimeFormat(activity);
		
		if (sSmileys == null) {
			sSmileys = Collections.synchronizedSortedMap(new TreeMap<String, Drawable>(activity.getSmileysManager().getManagedSmileys()));
			
			for (String smiley: new ArrayList<String>(sSmileys.keySet())) {
				if (ViewUtils.isSmileyReadOnly(smiley)) {
					Drawable value = sSmileys.remove(smiley);
					smiley = ViewUtils.escapeOmittableSmiley(smiley);
					sSmileys.put(smiley, value);
				}
			}
			
			sSmileyBound = (int) (activity.getResources().getDisplayMetrics().density * 10);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = messageItemLayout.getView();
		}
		
		if (mAq == null) {
			mAq = new AQuery(convertView);
		} else {
			mAq.recycle(convertView);
		}
		
		View v = convertView;
		
		ChatMessageHolder holder = getItem(position);
		
		mAq.id(messageItemLayout.getIconImageViewId()).visibility(copyModeStarter != null ? View.GONE : View.VISIBLE);
		
		String filename;
		
		if (!holder.getMessage().isIncoming()) {
			filename = mAccount.getFilename();
		} else {
			Buddy b = null;
			if (mBuddy instanceof MultiChatRoom) {
				b = ((MultiChatRoom)mBuddy).findOccupantByUid(holder.getMessage().getContactDetail());				
			}
			
			if (b == null) {
				b = mBuddy;
			}
			
			filename = b.getFilename();
		}
		
		ViewUtils.fillIcon(messageItemLayout.getIconImageViewId(), mAq, filename, getContext());
		
		mAq.id(messageItemLayout.getSenderTextViewId()).text(holder.getSenderName());		
		
		colorSenderName(holder, mAq.getTextView());
		
		mAq.id(messageItemLayout.getMessageStatusImageId()).image(BitmapAjaxCallback.getMemoryCached(getContext(), getImageResourceForAckState(holder.getAckState()))).visibility(copyModeStarter == null ? View.VISIBLE : View.INVISIBLE);
		mAq.id(messageItemLayout.getCheckboxId()).visibility(copyModeStarter != null ? View.VISIBLE : View.GONE).checked(copyModeStarter != null && copyModeStarter == v);
		
		if (copyModeStarter == null) {			
			v.setOnClickListener(null);
		} else {
			v.setOnClickListener(mCheckForCopyClickListener);
		}
		
		if (holder.getMessage().getMessageId() == 0) {
			v.setBackgroundDrawable(parent.getContext().getResources().getDrawable(R.color.transparent));
		} else {
			v.setBackgroundColor(0);
		}
		
		mAq.id(messageItemLayout.getTimeTextViewId()).text(getFormattedMessageTime(holder));
		
		setTextAndFormat(getMainActivity(), mAq.id(messageItemLayout.getMessageTextViewId()).getTextView(), holder, mDontDrawSmilies);
		
		if (v.getTag() == null || v.getTag() != holder) {
			if (hasAttachments(holder)) {
				fillAttachments(mAq, holder);
			} else {
				mAq.id(messageItemLayout.getAttachmentsListViewId()).gone();
			}
		}
		
		v.setTag(holder);
				
		return v;
	}
	
	private void fillAttachments(AQuery aq, ChatMessageHolder holder) {
		ViewGroup attachmentsContainer = (ViewGroup) aq.id(messageItemLayout.getAttachmentsListViewId()).getView();
		
		if (attachmentsContainer == null) {
			return;
		}
		
		attachmentsContainer.removeAllViews();
		aq.visible();
		
		for (MessageAttachment a : ((TextMessage)holder.getMessage()).getAttachments()) {
			View attachmentView = constructAttachmentView(aq, a);
			attachmentsContainer.addView(attachmentView);
		}
	}

	private View constructAttachmentView(AQuery aq, final MessageAttachment attachment) {
		View attachmentView = LayoutInflater.from(getContext()).inflate(R.layout.message_attachment, null);
		aq.recycle(attachmentView);
		
		aq.id(R.id.title).text(attachment.getTitle());
		aq.id(R.id.source).text(attachment.getSource());
		
		switch (attachment.getType()) {
		case AUDIO:
		case VIDEO:
			aq.id(R.id.source).gone();
			aq.id(R.id.picture).gone().image((Drawable)null);
			aq.id(R.id.play).visible().clicked(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent();
					i.setAction(Intent.ACTION_VIEW);
					i.setDataAndType(Uri.parse(attachment.getSource()), attachment.getType() == MessageAttachmentType.AUDIO ? "audio/*" : "video/*");
					getMainActivity().startActivity(i);
				}
			});
			break;
		case PHOTO:
			aq.id(R.id.source).gone();
			aq.id(R.id.play).gone().clicked(null);
			aq.id(R.id.picture).visible().image(attachment.getSource(), true, true, 0, R.drawable.dummy_icon);
			break;
		default:
			aq.id(R.id.source).visible().text(attachment.getSource());
			aq.id(R.id.play).gone();
			aq.id(R.id.picture).gone();
			break;
		}
		
		return attachmentView;
	}

	private boolean hasAttachments(ChatMessageHolder holder) {
		return holder.getAttachmentsAdapter() == null 
				&& holder.getMessage() != null 
				&& holder.getMessage() instanceof TextMessage
				&& ((TextMessage)holder.getMessage()).getAttachments().size() > 0;

	}

	//color sender label according to message type
	//TODO maybe add styling support
	private void colorSenderName(ChatMessageHolder holder, TextView sender) {
		Message text = holder.getMessage();
		if (text instanceof ServiceMessage) {
			sender.setTextAppearance(getContext(), R.style.chat_message_sender_color_service);
		} else if (text instanceof TextMessage) {
			TextMessage txt = (TextMessage) text;
			sender.setTextAppearance(getContext(), txt.isIncoming() ? R.style.chat_message_sender_color_received : R.style.chat_message_sender_color_sent);
		} else {
			sender.setTextAppearance(getContext(), android.R.color.primary_text_light);
		}
	}

	private String getFormattedMessageTime(ChatMessageHolder holder) {
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(holder.getMessage().getTime());
		
		Calendar now = Calendar.getInstance();
		now.setTimeInMillis(System.currentTimeMillis());
		
		if (now.get(Calendar.DATE) == time.get(Calendar.DATE)
				&& now.get(Calendar.MONTH) == time.get(Calendar.MONTH)
				&& now.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
			return mTimeFormat.format(time.getTime());
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(mTimeFormat.format(time.getTime()))
					.append(" ")
					.append(mDateFormat.format(time.getTime()));
			return sb.toString();
		}
	}

	private static final int getImageResourceForAckState(MessageAckState ackState) {
		int resId;
		
		if (ackState == null) {
			resId = R.drawable.btn_check_off_disable;
		} else {
			switch (ackState) {
			case READ_ACK:
				resId = R.drawable.btn_check_on_selected;
				break;
			case RECIPIENT_ACK:
				resId = R.drawable.btn_check_on;					
				break;
			default:
				resId = R.drawable.btn_check_on_disable;					
				break;
			}
		}
		
		return resId;
	}

	private static final void setTextAndFormat(MainActivity activity, TextView view, ChatMessageHolder holder, boolean dontDrawSmileys) {
		
		MovementMethod mm = view.getMovementMethod();
        if (!(mm instanceof LinkMovementMethod))
        {
             view.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        String text;
        if (holder.getMessage() instanceof FileMessage) {
        	StringBuilder b = new StringBuilder();
        	b.append(activity.getString(R.string.buddy_sends_files, holder.getSenderName()));
        	for (FileInfo i : ((FileMessage)holder.getMessage()).getFiles()) {
        		b.append("\n");
        		b.append(activity.getString(R.string.file_transfer_request_format, i.getFilename(), ViewUtils.humanReadableByteCount(i.getSize(), true)));
        	}
        	text = b.toString();
        } else {
        	text = holder.getMessage().getText();
        }
        
        //A hack for aligning ImageSpans correctly in a case of no text except smileys.
		view.setText(text + " ", TextView.BufferType.EDITABLE);

		Spannable spannable = view.getEditableText();
		if (spannable == null) {
			return;
		}
		
		//ViewUtils.spanKnownUrls(spannable, text, activity);

		if (dontDrawSmileys) {
			return;
		}
		
		for (String key : sSmileys.keySet()) {
			int pos = text.indexOf(key);

			if (pos < 0) {
				continue;
			}
			
			Drawable value = sSmileys.get(key);
			
			int height = (int) (view.getTextSize() + sSmileyBound);
			int width = (int) ((value.getIntrinsicWidth() + 0.0f)/value.getIntrinsicHeight() * height);			
			value.setBounds(0, 0, width, height);

			while (pos < text.length()) {
				if (pos > -1) {
					try {
						spannable.setSpan(new ImageSpan(value, ImageSpan.ALIGN_BASELINE), pos, pos + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					} catch (Exception e) {
						Logger.log(e);
					}
					pos = text.indexOf(key, pos + key.length());
				} else {
					break;
				}
			}
			byte[] replace = new byte[key.length()];
			Arrays.fill(replace, (byte) '_');
			text = text.replace(key, new String(replace));
		}

		if (holder.getMessage() instanceof ServiceMessage) {
			view.setTextAppearance(activity, R.style.chat_message_sender_color_service);
		}
	}

	/**
	 * @return the mDontDrawSmilies
	 */
	public boolean isDontDrawSmilies() {
		return mDontDrawSmilies;
	}
	
	public String grabSelectedText(ListView list) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<list.getChildCount(); i++){
			View item = list.getChildAt(i);
			CheckBox cb = (CheckBox) item.findViewById(messageItemLayout.getCheckboxId());
			
			if (cb.isChecked() && item.getTag() != null) {
				ChatMessageHolder holder = (ChatMessageHolder) item.getTag();
				Message m = holder.getMessage();
				sb.append(getContext().getString(R.string.copy_mode_format, 
						holder.getSenderName(), 
						android.text.format.DateFormat.getTimeFormat(getContext()).format(new Date(m.getTime())),
						m.getText()));
				sb.append('\n');
				//cb.setChecked(false);
			}
		}
		
		return sb.toString();
	}

	/**
	 * @param mDontDrawSmilies the mDontDrawSmilies to set
	 */
	public void setDontDrawSmilies(boolean mDontDrawSmilies) {
		this.mDontDrawSmilies = mDontDrawSmilies;
	}

	public void setCopyMode(boolean copyMode, View starter) {
		if (copyMode) {
			if (starter != null) {
				copyModeStarter = starter;
			} else {
				copyModeStarter = new Object();
			}
		} else {
			copyModeStarter = null;
		}
		notifyDataSetChanged();
	}
	
	protected MainActivity getMainActivity() {
		return (MainActivity) getContext();
	}
}
