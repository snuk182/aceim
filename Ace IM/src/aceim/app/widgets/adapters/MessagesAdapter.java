package aceim.app.widgets.adapters;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aceim.api.dataentity.FileInfo;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.MessageAckState;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.utils.Logger;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.SmileyResources;
import aceim.app.utils.ViewUtils;
import aceim.app.view.page.chat.ChatMessageHolder;
import aceim.app.view.page.chat.ChatMessageTimeFormat;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.AQuery;

public class MessagesAdapter extends ArrayAdapter<ChatMessageHolder> {
	
	private static Map<String, Drawable> sSmileys;
	private static int sSmileyBound; 
	
	private DateFormat mDateFormat;
	private DateFormat mTimeFormat;
	private ChatMessageTimeFormat mTimeDisplayFormat;
	
	private boolean mDontDrawSmilies = false;
	
	private Object copyModeStarter = null;
	
	private final  OnClickListener mCheckForCopyClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			new AQuery(v).id(R.id.checkbox).checked(true);
		}
	};
	
	public MessagesAdapter(MainActivity activity, int resource, ChatMessageTimeFormat timeDisplayFormat) {
		super(activity, resource, R.id.sender);		
		init(activity, timeDisplayFormat);
	}

	public MessagesAdapter(MainActivity activity, int resource, ChatMessageHolder[] objects, ChatMessageTimeFormat timeDisplayFormat) {
		super(activity, resource, R.id.sender, objects);
		init(activity, timeDisplayFormat);
	}

	public MessagesAdapter(MainActivity activity, int resource, List<ChatMessageHolder> objects, ChatMessageTimeFormat timeDisplayFormat) {
		super(activity, resource, R.id.sender, objects);
		init(activity, timeDisplayFormat);
	}
	
	private void init(MainActivity activity, ChatMessageTimeFormat timeDisplayFormat){
		mDateFormat = android.text.format.DateFormat.getMediumDateFormat(activity);
		mTimeFormat = android.text.format.DateFormat.getTimeFormat(activity);
		mTimeDisplayFormat = timeDisplayFormat;
		
		if (sSmileys == null) {
			sSmileys = Collections.synchronizedMap(new HashMap<String, Drawable>());
			
			List<SmileyResources> smileys = new ArrayList<SmileyResources>(activity.getAdditionalSmileys());
			
			for (int ii = smileys.size()-1; ii>=0; ii--) {
				SmileyResources r = smileys.get(ii);
				try {
					Resources nr = r.getNativeResourcesForProtocol(activity.getPackageManager());
					
					for (int i=0; i<r.getNames().length; i++) {
						if (!sSmileys.containsKey(r.getNames()[i])) {
							Drawable d = nr.getDrawable(r.getDrawableIDs()[i]);
							sSmileys.put(r.getNames()[i], d);
						}
					}
				} catch (Exception e) {
					Logger.log(e);
				}
			}
			
			sSmileyBound = (int) (activity.getResources().getDisplayMetrics().density * 4);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		
		ChatMessageHolder holder = getItem(position);

		ImageView status = (ImageView) v.findViewById(R.id.status);
		CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
		TextView sender = (TextView) v.findViewById(R.id.sender);
		sender.setText(holder.getSenderName());
		
		colorSenderName(holder, sender);

		if (status != null) {
			status.setImageResource(getImageResourceForAckState(holder.getAckState()));
			
			status.setVisibility(copyModeStarter != null ? View.INVISIBLE : View.VISIBLE);
		}
		
		checkbox.setVisibility(copyModeStarter != null ? View.VISIBLE : View.GONE);
		
		if (copyModeStarter == null) {
			checkbox.setChecked(false);
			v.setOnClickListener(null);
		} else {
			v.setOnClickListener(mCheckForCopyClickListener);
			if (copyModeStarter == v) {
				checkbox.setChecked(true);
			}
		}
		
		if (v.getTag() != null && v.getTag() == holder) {
			//we entered here just to update ack, move out
			return v;			
		} else {
			v.setTag(holder);
		}
		
		TextView time = (TextView) v.findViewById(R.id.time);
		TextView message = (TextView) v.findViewById(R.id.message);
		
		time.setText(getFormattedMessageTime(holder));
		setTextAndFormat(getMainActivity(), message, holder, mDontDrawSmilies);
		
		return v;
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
		Date time = new Date(holder.getMessage().getTime());
		switch (mTimeDisplayFormat){
		case DO_NOT_DISPLAY:
			return "";
		case TIME_ONLY:
			return mTimeFormat.format(time);
		default:
			StringBuilder sb = new StringBuilder();
			sb.append(mTimeFormat.format(time))
					.append(" ")
					.append(mDateFormat.format(time));
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

		spanUrl("ftp", spannable, text);
		spanUrl("http", spannable, text);
		spanUrl("https", spannable, text);
		spanUrl("market", spannable, text);

		if (dontDrawSmileys) {
			return;
		}
		
		for (String key : sSmileys.keySet()) {
			int pos = text.indexOf(key);

			if (pos < 0) {
				continue;
			}
			
			Drawable value = sSmileys.get(key);
			
			int smileySize = (int) (view.getTextSize() + sSmileyBound);
			int width = (int) ((value.getIntrinsicHeight() + 0.0f)/value.getIntrinsicWidth() * smileySize);			
			value.setBounds(0, 0, smileySize, width);

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

	private static final void spanUrl(String protocol, Spannable spannable, String text) {
		if (text.indexOf(protocol + "://") > -1) {
			int pos = 0;

			while (pos > -1 && pos < text.length()) {
				pos = text.indexOf(protocol + "://", pos);

				if (pos > -1) {
					int spaceEndPos = text.indexOf(" ", pos);
					int endPos = spaceEndPos > -1 ? spaceEndPos : text.length();

					int nlEndPos = text.indexOf("\n", pos);

					if (nlEndPos > pos && nlEndPos < endPos) {
						endPos = nlEndPos;
					}

					String url = text.substring(pos, endPos);
					URLSpan urlSpan = new URLSpan(url);
					spannable.setSpan(urlSpan, pos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					byte[] replace = new byte[endPos - pos];
					Arrays.fill(replace, (byte) '_');
					text = text.replace(url, new String(replace));
					pos = ++endPos;
				} else {
					break;
				}
			}
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
			CheckBox cb = (CheckBox) item.findViewById(R.id.checkbox);
			
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
