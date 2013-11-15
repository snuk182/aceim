package aceim.app.utils.history.impl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.FileMessage;
import aceim.api.dataentity.Message;
import aceim.api.dataentity.ServiceMessage;
import aceim.api.dataentity.TextMessage;
import aceim.api.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import aceim.app.utils.history.HistorySaver;
import android.content.Context;

public final class JsonHistorySaver implements HistorySaver {

	private static final String CHARSET = "UTF-16BE";
	private static final byte[] RECORD_DIVIDER_BYTES;
	private static final String RECORD_DIVIDER = "{}";
	private static final byte[] NEW_LINE_BYTES;
	private static final int MAX_MESSAGES_TO_READ = 8;

	public static final String FILE_EXTENSION = ".history";
	
	private final Context mContext;
	
	static {
		RECORD_DIVIDER_BYTES = getBytes(RECORD_DIVIDER);
		NEW_LINE_BYTES = getBytes("\n");
	}

	public JsonHistorySaver(Context context) {
		this.mContext = context;
	}	

	/* (non-Javadoc)
	 * @see aceim.app.utils.HistorySaver#saveMessage(aceim.app.api.dataentity.Buddy, aceim.app.api.dataentity.Message)
	 */
	@Override
	public void saveMessage(final Buddy buddy, final Message message) {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				OutputStream stream;
				try {
					stream = new BufferedOutputStream(mContext.openFileOutput(buddy.getFilename() + FILE_EXTENSION, Context.MODE_APPEND));
				} catch (FileNotFoundException e1) {
					// Should not happen, as Android API guarantees the creation of the new file.
					Logger.log("Starting new history file for " + buddy);
					stream = null;
				}
				
				JSONObject o;
				try {
					o = HistoryObject.fromMessage(message);
				} catch (JSONException e) {
					Logger.log(e);
					o = new JSONObject();
				}
				
				byte[] buffer = getBytes(o.toString());

				try {
					stream.write(RECORD_DIVIDER_BYTES);
					stream.write(NEW_LINE_BYTES);
					stream.write(buffer);
					stream.write(NEW_LINE_BYTES);					
				} catch (IOException e) {
					Logger.log(e);
				} finally {
					try {
						stream.flush();
						stream.close();
					} catch (IOException e) {
						Logger.log(e);
					}
				}
			}
		};
		
		Executors.defaultThreadFactory().newThread(r).start();
	}

	/* (non-Javadoc)
	 * @see aceim.app.utils.HistorySaver#getMessages(aceim.app.api.dataentity.Buddy)
	 */
	@Override
	public List<Message> getMessages(Buddy buddy) {
		return getMessages(buddy, 0, MAX_MESSAGES_TO_READ);
	}

	/* (non-Javadoc)
	 * @see aceim.app.utils.HistorySaver#getMessages(aceim.app.api.dataentity.Buddy, int, int)
	 */
	@Override
	public List<Message> getMessages(Buddy buddy, int startFrom, int maxMessagesToRead) {
		BufferedReader stream = null;
		try {
			stream = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(mContext.getFileStreamPath(buddy.getFilename() + FILE_EXTENSION)), CHARSET));
			
			int index = 0;
			String line = null;
			StringBuilder sb = new StringBuilder();
			List<Message> messages = new ArrayList<Message>();
			
			long endTo = startFrom + maxMessagesToRead;
			
			while ((line = stream.readLine()) != null && index < endTo) {
				
				if (line.equals(RECORD_DIVIDER))
                {
                    if (sb.length() > 2) {
                    	if (index >= startFrom) {
                    		HistoryObject o = new HistoryObject(sb.toString());
                        	messages.add(0, o.toMessage(buddy));
                    	}
                    } 
                    
                    index++;
                }
                else
                {
                	if (index >= startFrom) {
                		sb.insert(0, line);
                	}
                }
			}
			
			return messages;
		} catch (FileNotFoundException e) {
			Logger.log("No history found for " + buddy);
		} catch (IOException e) {
			Logger.log(e);
		} catch (JSONException e) {
			Logger.log(e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					Logger.log(e);
				}
			}
		}
		
		return Collections.emptyList();
	}
	
	private static final byte[] getBytes(String string) {
		try {
			return string.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			Logger.log(CHARSET + " is unsupported, using " + Charset.defaultCharset() + " instead");
			return string.getBytes();
		}
	}

	private static enum MessageType {
		TEXT, FILE, SERVICE;
		
		static MessageType fromMessage(Message message) {
			if (message instanceof TextMessage) {
				return MessageType.TEXT;
			} else if (message instanceof FileMessage) {
				return MessageType.FILE;
			} else if (message instanceof ServiceMessage) {
				return MessageType.SERVICE;
			} else {
				// TODO here may be dragons...
				throw new IllegalArgumentException("Cannot store message of type " + message.getClass().getName());
			}
		}	
		
		Message getMessageByType(Buddy buddy) {
			Message message;
			switch (this) {
			case FILE:
				message = new FileMessage(buddy.getServiceId(), buddy.getProtocolUid());
				break;
			case SERVICE:
				message = new ServiceMessage(buddy.getServiceId(), buddy.getProtocolUid(), false);
				break;
			case TEXT:
				message = new TextMessage(buddy.getServiceId(), buddy.getProtocolUid());
				break;
			default:
				message = null;
				break;
			}

			return message;
		}
	}
	
	private static final class HistoryObject extends JSONObject {
		
		private HistoryObject(){
			super();
		}
		
		private HistoryObject(String str) throws JSONException {
			super(str);
		}
		
		static HistoryObject fromMessage(Message message) throws JSONException{
			HistoryObject o = new HistoryObject();
			
			o.put("type", MessageType.fromMessage(message).name());
			o.put("contact-name", message.getContactDetail() != null ? message.getContactDetail() : message.getContactUid());
			o.put("incoming", message.isIncoming());
			o.put("text", message.getText());
			o.put("message-id", message.getMessageId());
			o.put("time", message.getTime());
			
			return o;
		}
		
		Message toMessage(Buddy buddy) throws JSONException {
			MessageType type = MessageType.valueOf((String) get("type"));
			String contactName = getString("contact-name");
			boolean incoming = getBoolean("incoming");
			String text = getString("text");
			long messageId = getLong("message-id");
			long time = getLong("time");
			
			Message m = type.getMessageByType(buddy);
			if (!contactName.equals(buddy.getProtocolUid())) {
				m.setContactDetail(contactName);
			}
			m.setIncoming(incoming);
			m.setText(text);
			m.setMessageId(messageId);
			m.setTime(time);
			
			return m;
		}
	}
	
	private static class ReverseLineInputStream extends InputStream {

	    RandomAccessFile in;

	    long currentLineStart = -1;
	    long currentLineEnd = -1;
	    long currentPos = -1;
	    long lastPosInFile = -1;

	    ReverseLineInputStream(File file) throws FileNotFoundException {
	        in = new RandomAccessFile(file, "r");
	        currentLineStart = file.length();
	        currentLineEnd = file.length();
	        lastPosInFile = file.length() -1;
	        currentPos = currentLineEnd; 
	    }

	    public void findPrevLine() throws IOException {

	        currentLineEnd = currentLineStart; 

	        // There are no more lines, since we are at the beginning of the file and no lines.
	        if (currentLineEnd == 0) {
	            currentLineEnd = -1;
	            currentLineStart = -1;
	            currentPos = -1;
	            return; 
	        }

	        long filePointer = currentLineStart -1;

	         while ( true) {
	             filePointer--;

	            // we are at start of file so this is the first line in the file.
	            if (filePointer < 0) {  
	                break; 
	            }

	            in.seek(filePointer);
	            int readByte = in.readByte();

	            // We ignore last LF in file. search back to find the previous LF.
	            if (readByte == 0xA && filePointer != lastPosInFile ) {   
	                break;
	            }
	         }
	         // we want to start at pointer +1 so we are after the LF we found or at 0 the start of the file.   
	         currentLineStart = filePointer + 1;
	         currentPos = currentLineStart;
	    }

	    public int read() throws IOException {

	        if (currentPos < currentLineEnd ) {
	            in.seek(currentPos++);
	            int readByte = in.readByte();
	            return readByte;

	        }
	        else if (currentPos < 0) {
	            return -1;
	        }
	        else {
	            findPrevLine();
	            return read();
	        }
	    }
	}

	@Override
	public boolean deleteHistory(Buddy buddy) {
		return mContext.deleteFile(buddy.getFilename() + FILE_EXTENSION);
	}
}
