package aceim.app.preference;

import aceim.app.AceIMActivity;
import aceim.app.Constants;
import aceim.app.MainActivity;
import aceim.app.R;
import aceim.app.dataentity.Account;
import aceim.app.utils.ViewUtils;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

public class OptionsActivity extends AceIMActivity {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Account a = getIntent().getParcelableExtra(Constants.INTENT_EXTRA_ACCOUNT);
        
        OptionsPage preferences;
        if (a != null) {
        	preferences = new AccountOptions(a);
        } else {
        	preferences = new GlobalOptions();
        }
        
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, preferences).commit();
        
        View content = findViewById(android.R.id.content);
        ViewUtils.setWallpaperMode(this, content);
        content.setBackgroundResource(R.color.transparent);
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
