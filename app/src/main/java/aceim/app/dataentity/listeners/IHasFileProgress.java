package aceim.app.dataentity.listeners;

import aceim.api.dataentity.FileProgress;

public interface IHasFileProgress {
	void onFileProgress(FileProgress progress);
}
