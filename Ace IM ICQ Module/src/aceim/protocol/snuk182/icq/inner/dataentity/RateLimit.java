package aceim.protocol.snuk182.icq.inner.dataentity;

import java.io.Serializable;

public class RateLimit implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1829533506746133373L;

	public short id = 0;
	public int windowSize = 0;
	public int clearLevel = 0;
	public int alertLevel = 0;
	public int limitLevel = 0;
	public int disconnectLevel = 0;
	public int currentLevel = 0;
	public int maxLevel = 0;
	public int lastTime = 0;
	public byte currentState = 0;
	public RateGroup rateGroup = null;
	
	public RateLimit(short id, int windowSize, int clearLevel, int alertLevel,
			int limitLevel, int disconnectLevel, int currentLevel,
			int maxLevel, int lastTime, byte currentState) {
		super();
		this.id = id;
		this.windowSize = windowSize;
		this.clearLevel = clearLevel;
		this.alertLevel = alertLevel;
		this.limitLevel = limitLevel;
		this.disconnectLevel = disconnectLevel;
		this.currentLevel = currentLevel;
		this.maxLevel = maxLevel;
		this.lastTime = lastTime;
		this.currentState = currentState;
	}
	
	public RateLimit(){}
}
