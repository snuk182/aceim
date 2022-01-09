package aceim.protocol.snuk182.vkontakte.model;

import aceim.api.utils.Logger;
import aceim.api.utils.Logger.LoggerLevel;
import aceim.protocol.snuk182.vkontakte.model.LongPollResponse.LongPollResponseUpdate;


public class VkOnlineInfo extends ApiObject {
	
	public static final byte STATUS_OFFLINE = -1;
	public static final byte STATUS_ONLINE = 0;
	public static final byte STATUS_AWAY = 1;
	
	
	private final long uid;
	private byte status = STATUS_ONLINE;

	public VkOnlineInfo(long uid) {
		super();
		this.uid = uid;		
	}
	
	public VkOnlineInfo(Integer uid) {
		super();
		this.uid = uid;		
	}
	
	public long getUid() {
		return uid;
	}

	/**
	 * @return the status
	 */
	public byte getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(byte status) {
		this.status = status;
	}
	
	public static VkOnlineInfo fromLongPollUpdate(LongPollResponseUpdate update) {
		VkOnlineInfo info;
		switch (update.getType()) {
		case BUDDY_OFFLINE_AWAY:
			info = new VkOnlineInfo(0-update.getId());
			byte s = Byte.parseByte(update.getParams()[0]);
			
			switch (s) {
			case 0:
				info.status = STATUS_OFFLINE;
				break;
			case 1:
				info.status = STATUS_AWAY;
				break;
			}
			
			break;
		case BUDDY_ONLINE:
			info = new VkOnlineInfo(0-update.getId());
			break;
		default:
			Logger.log("Cannot instantiate VkOnlineInfo from " + update.getType(), LoggerLevel.INFO);
			info = null;
		}
		
		return info;
	}
}
