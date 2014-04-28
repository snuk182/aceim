package aceim.api.dataentity;

/**
 * An interface to help core distinguish ID-based entities, used in UI only.
 */
public interface EntityWithID {

	/**
	 * Get entity's UI ID
	 * @return ID
	 */
	public abstract String getEntityId();
}
