package aceim.app.dataentity.listeners;

import aceim.app.MainActivity;
import aceim.app.dataentity.ActivityResult;

public interface IHasFilePicker {

	void onFilePicked(ActivityResult result, MainActivity activity);
}
