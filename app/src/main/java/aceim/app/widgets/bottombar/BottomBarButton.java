package aceim.app.widgets.bottombar;

import aceim.app.AceIMActivity;
import aceim.app.R;
import android.content.Context;
import android.content.res.Resources;
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
		Resources themeResources = activity.getThemesManager().getCurrentThemeContext().getResources();
		mBackground = themeResources.getDrawable(activity.getThemesManager().getViewResources().getBottomBarButtonBackgroundId());
		
		if (mBackground == null) {
			mBackground = activity.getResources().getDrawable(android.R.drawable.menuitem_background);
		}
	}
}
