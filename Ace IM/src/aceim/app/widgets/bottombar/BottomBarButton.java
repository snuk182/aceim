package aceim.app.widgets.bottombar;

import aceim.app.AceIMActivity;
import aceim.app.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class BottomBarButton extends ImageButton {
	
	private Drawable mBackground = null;
	
	@SuppressWarnings("deprecation")
	public BottomBarButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		int padding = context.getResources().getDimensionPixelSize(R.dimen.contact_list_grid_items_spacing);
		setPadding(0, 0, 0, padding);
		setScaleType(ScaleType.CENTER);
		
		initVariables((AceIMActivity) context);
		
		setBackgroundDrawable(mBackground);
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
		super.setImageBitmap(bm);
	}
	
	private void initVariables(AceIMActivity activity) {
		TypedArray array = activity.getThemesManager().getCurrentTheme().obtainStyledAttributes(aceim.res.R.styleable.Ace_IM_Theme);
		
		for (int i =0; i< array.getIndexCount(); i++) {
			int res = array.getIndex(i);
			
			switch (res) {
			case aceim.res.R.styleable.Ace_IM_Theme_bottom_bar_button_background:
				mBackground = array.getDrawable(i);
				break;
			}
		}
		
		array.recycle();
		
		if (mBackground == null) {
			mBackground = activity.getResources().getDrawable(android.R.drawable.menuitem_background);
		}
	}
}
