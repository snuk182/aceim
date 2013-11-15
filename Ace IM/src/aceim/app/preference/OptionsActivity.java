package aceim.app.preference;

import aceim.app.Constants;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.utils.ViewUtils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Window;

public class OptionsActivity extends FragmentActivity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Ace_IM_Theme_Transparent);
        
        Account a = getIntent().getParcelableExtra(Constants.INTENT_EXTRA_ACCOUNT);
        
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        requestWindowFeature(Window.FEATURE_OPTIONS_PANEL);
        
        OptionsPage preferences;
        if (a != null) {
        	preferences = new AccountOptions(a);
        } else {
        	preferences = new GlobalOptions();
        }
        
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, preferences).commit();
        
        setTitle(preferences.getTitle(getBaseContext()));        
        getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON, preferences.getIcon(getBaseContext()));
        
        ViewUtils.setWallpaperMode(this, findViewById(android.R.id.content));
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_BACK){
			startActivity(new Intent(this, MainActivity.class));
			finish();
		}
		
		return super.onKeyDown(keyCode, event);
	}
}
