package aceim.app.view.page.other;

import aceim.app.R;
import aceim.app.view.page.Page;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Splash extends Page {

	@Override
	public View createView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
		return inflater.inflate(R.layout.splash, group, false);
	}

	@Override
	public Drawable getIcon(Context context) {
		return context.getResources().getDrawable(R.drawable.ic_launcher);
	}

	@Override
	public String getTitle(Context context) {
		return context.getResources().getString(R.string.wait);
	}
}
