package aceim.app.view.page.transfers;

import java.util.List;

import aceim.api.dataentity.FileProgress;
import aceim.app.R;
import aceim.app.dataentity.FileTransfer;
import aceim.app.utils.ViewUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileTransfersAdapter extends ArrayAdapter<FileTransfer> {

	private static final int MAX_PROGRESS = 10000;

	public FileTransfersAdapter(Context context, List<FileTransfer> objects) {
		super(context, R.layout.file_transfer_item, R.id.title, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null) {
			view = super.getView(position, convertView, parent);
		} else {
			view = convertView;
		}

		FileTransfer t = getItem(position);
		
		view.setTag(t);
		
		populate(view, t);
		
		return view;
	}

	void populate(View view, final FileTransfer t) {
		final FileProgress p = t.getProgress();
		if (p == null) {
			// TODO
			view.setVisibility(View.INVISIBLE);

			return;
		}
		view.setVisibility(View.VISIBLE);

		ImageView leftIcon = (ImageView) view.findViewById(R.id.left_icon);
		ImageView rightIcon = (ImageView) view.findViewById(R.id.right_icon);
		View leftArrow = view.findViewById(R.id.left_arrow);
		View rightArrow = view.findViewById(R.id.right_arrow);
		TextView leftLabel = (TextView) view.findViewById(R.id.left_label);
		TextView rightLabel = (TextView) view.findViewById(R.id.right_label);

		TextView info = (TextView) view.findViewById(R.id.info);
		TextView title = (TextView) view.findViewById(R.id.title);
		ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);

		final String filename = ViewUtils.getFileNameFromPath(p.getFilePath());
		title.setText(filename);
		progress.setMax(MAX_PROGRESS);

		progress.setIndeterminate(p.getTotalSizeBytes() < 1);
		progress.setVisibility((p.getTotalSizeBytes() > 0 && p.getSentBytes() >= p.getTotalSizeBytes()) || p.getError() != null ? View.INVISIBLE : View.VISIBLE);
		progress.getLayoutParams().height = p.getError() == null ? LayoutParams.MATCH_PARENT : 0;

		Bitmap bicon = ViewUtils.getIcon(getContext(), t.getParticipant().getFilename());

		if (p.isIncoming()) {
			leftIcon.getLayoutParams().width = LayoutParams.WRAP_CONTENT;// getContext().getResources().getDimensionPixelSize(R.dimen.contact_list_grid_item_size);
			leftLabel.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
			leftArrow.getLayoutParams().width = LayoutParams.WRAP_CONTENT;

			if (bicon != null) {
				leftIcon.setImageBitmap(bicon);
			} else {
				leftIcon.setImageResource(R.drawable.dummy_icon);
			}

			leftLabel.setText(t.getParticipant().getSafeName());

			rightIcon.getLayoutParams().width = 0;
			rightLabel.getLayoutParams().width = 0;
			rightArrow.getLayoutParams().width = 0;
		} else {
			rightIcon.getLayoutParams().width = LayoutParams.WRAP_CONTENT; //getContext().getResources().getDimensionPixelSize(R.dimen.contact_list_grid_item_size);
			rightLabel.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
			rightArrow.getLayoutParams().width = LayoutParams.WRAP_CONTENT;

			if (bicon != null) {
				rightIcon.setImageBitmap(bicon);
			} else {
				rightIcon.setImageResource(R.drawable.dummy_icon);
			}

			rightLabel.setText(t.getParticipant().getSafeName());

			leftIcon.getLayoutParams().width = 0;
			leftLabel.getLayoutParams().width = 0;
			leftArrow.getLayoutParams().width = 0;
		}

		if (p.getError() != null) {
			title.setBackgroundResource(R.drawable.criteria_bad);
			info.setText(p.getError());
		} else if (p.getTotalSizeBytes() > 0 && p.getSentBytes() >= p.getTotalSizeBytes()) {
			title.setBackgroundResource(R.drawable.criteria_good);
			info.setText(R.string.press_to_open);
		} else {
			view.setBackgroundColor(Color.TRANSPARENT);
			if (p.getTotalSizeBytes() < 1) {
				progress.setIndeterminate(true);
				info.setText(R.string.file_transfer_init);
			} else {
				progress.setIndeterminate(false);
				progress.setProgress((int) (MAX_PROGRESS * p.getSentBytes() / p.getTotalSizeBytes()));
				info.setText(getContext().getString(R.string.file_transfer_progress_format, ViewUtils.humanReadableByteCount(p.getSentBytes(), true), ViewUtils.humanReadableByteCount(p.getTotalSizeBytes(), true)));
			}
		}

	}
}
