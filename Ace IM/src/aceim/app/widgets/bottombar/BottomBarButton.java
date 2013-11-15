package aceim.app.widgets.bottombar;

import aceim.app.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class BottomBarButton extends ImageButton {
	
	public BottomBarButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		int padding = context.getResources().getDimensionPixelSize(R.dimen.contact_list_grid_items_spacing);
		setPadding(0, 0, 0, padding);
		setScaleType(ScaleType.CENTER);
		//setAdjustViewBounds(true);
	}

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > (int)(height + 0.5)) {
            width = (int)(height + 0.5);
        } else {
            height = (int)(width + 0.5);
        }
        
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }
	
	@Override
	public void setImageBitmap(Bitmap bm){
		/*int offset = getContext().getResources().getDimensionPixelSize(R.dimen.top_bottom_bar_top_padding);
		
		int wbias = bm.getWidth() - getWidth() - offset;
		int hbias = bm.getHeight() - getHeight() - offset;
		
		if (wbias > 0 || hbias > 0) {
			int dstWidth;
			int dstHeight;
			
			if (bm.getWidth() > bm.getHeight()) {
				dstWidth = getWidth() - offset;
				dstHeight = (int) ((dstWidth+0f / bm.getWidth()) * bm.getHeight());
			} else {
				dstHeight = getHeight() - offset;
				dstWidth = (int) (dstHeight+0f / bm.getHeight() * bm.getWidth());
			}
			
			Bitmap tmp = Bitmap.createScaledBitmap(bm, dstWidth, dstHeight, false);
			
			if (tmp != bm) {
				bm.recycle();
				bm = tmp;
			}
		}*/
		
		super.setImageBitmap(bm);
	}
}
