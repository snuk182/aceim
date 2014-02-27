package aceim.app.view.page.history;

import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.app.MainActivity;
import aceim.app.dataentity.Account;
import aceim.app.themeable.dataentity.HistoryMessageItemThemeResource;
import aceim.app.view.page.chat.ChatMessageHolder;
import aceim.app.widgets.adapters.MessagesAdapter;
import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;

@SuppressLint("InlinedApi")
class HistoryMessagesAdapter extends MessagesAdapter {
	
	private Button mButton;
	private final OnClickListener mButtonClickListener;

	public HistoryMessagesAdapter(MainActivity activity, Account account, Buddy buddy, HistoryMessageItemThemeResource resource, OnClickListener clickListener) {
		super(activity, account, buddy, resource);
		mButtonClickListener = clickListener;
	}

	public HistoryMessagesAdapter(MainActivity activity, Account account, Buddy buddy, HistoryMessageItemThemeResource resource, ChatMessageHolder[] objects, OnClickListener clickListener) {
		super(activity, account, buddy, resource, objects);
		mButtonClickListener = clickListener;
	}

	public HistoryMessagesAdapter(MainActivity activity, Account account, Buddy buddy, HistoryMessageItemThemeResource resource, List<ChatMessageHolder> objects, OnClickListener clickListener) {
		super(activity, account, buddy, resource, objects);
		mButtonClickListener = clickListener;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatMessageHolder holder = getItem(position);

		if (holder.getMessage() instanceof AddMoreButtonMessage){
			if (mButton == null) {
				mButton = new Button(getContext());
				mButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				if (mButtonClickListener != null) {
					mButton.setOnClickListener(mButtonClickListener);
				}
			} else {
				ViewGroup pparent = (ViewGroup)mButton.getParent();
				if (pparent != null) {
					pparent.removeView(mButton);
				}
			}
			
			mButton.setText(holder.getMessage().getText());			
			
			return mButton;
		} else {
			return super.getView(position, (convertView instanceof Button) ? null : convertView , parent);
		}
	}
}
