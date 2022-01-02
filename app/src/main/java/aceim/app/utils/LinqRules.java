package aceim.app.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import aceim.api.dataentity.Buddy;
import aceim.api.dataentity.PersonalInfo;
import aceim.app.dataentity.Account;
import aceim.app.dataentity.listeners.IHasAccount;
import aceim.app.dataentity.listeners.IHasAccountList;
import aceim.app.dataentity.listeners.IHasBuddy;
import aceim.app.dataentity.listeners.IHasFileProgress;
import aceim.app.dataentity.listeners.IHasMessages;
import aceim.app.utils.linq.KindaLinqRule;
import aceim.app.view.page.Page;
import aceim.app.view.page.chat.Chat;
import aceim.app.view.page.history.History;
import aceim.app.view.page.personalinfo.PersonalInfoPage;
import aceim.app.view.page.transfers.FileTransfers;

public class LinqRules {
	
	public static final class AccountByProtocolUidLinqRule implements KindaLinqRule<Account> {
		
		private final String protocolUid;

		public AccountByProtocolUidLinqRule(String protocolUid) {
			this.protocolUid = protocolUid;
		}

		@Override
		public boolean match(Account t) {
			return t != null && t.getProtocolUid().equals(protocolUid);
		}
		
	}

	public static final class AccountPageLinqRule implements KindaLinqRule<Page> {
		
		private final byte serviceId;
		
		public AccountPageLinqRule(byte serviceId) {
			this.serviceId = serviceId;
		}

		@Override
		public boolean match(Page t) {
			return (t instanceof IHasAccount) && ((IHasAccount)t).getAccount().getServiceId() == serviceId;
		}		
	}
	
	public static final class ProtocolAccountPageLinqRule implements KindaLinqRule<Page> {
		
		private final String protocolServiceClassName;
		
		public ProtocolAccountPageLinqRule(String protocolServiceClassName) {
			this.protocolServiceClassName = protocolServiceClassName;
		}

		@Override
		public boolean match(Page t) {
			return (t instanceof IHasAccount) && ((IHasAccount)t).getAccount().getProtocolServicePackageName().equals(protocolServiceClassName);
		}		
	}
	
	public static final class AccountListPageLinqRule implements KindaLinqRule<Page> {
		
		@Override
		public boolean match(Page t) {
			return (t instanceof IHasAccountList);
		}		
	}
	
	public static final class FileTransfersPageLinqRule implements KindaLinqRule<Page> {
		
		private final byte serviceId;
		
		public FileTransfersPageLinqRule(byte serviceId) {
			this.serviceId = serviceId;
		}

		@Override
		public boolean match(Page t) {
			return (t instanceof FileTransfers) && ((FileTransfers)t).getAccount().getServiceId() == serviceId;
		}		
	}
	
	public static final class FileProgressPageLinqRule implements KindaLinqRule<Page> {
		
		@Override
		public boolean match(Page t) {
			return (t instanceof IHasFileProgress);
		}		
	}
	
	public static final class BuddyPageLinqRule implements KindaLinqRule<Page> {
		
		private final byte serviceId;
		private final List<String> protocolUids;

		public BuddyPageLinqRule(Buddy b) {
			this.serviceId = b.getServiceId();
			this.protocolUids = Arrays.asList(b.getProtocolUid());
		}
		
		public BuddyPageLinqRule(List<Buddy> b) {
			if (b == null || b.size() < 1) {
				this.serviceId = -1;
				this.protocolUids = Collections.emptyList();
			} else {
				this.serviceId = b.get(0).getServiceId();
				this.protocolUids = new ArrayList<String>(b.size());
				
				for (Buddy bb : b) {
					this.protocolUids.add(bb.getProtocolUid());
				}
			}
		}

		public BuddyPageLinqRule(byte serviceId, String buddyProtocolUid) {
			this.serviceId = serviceId;
			this.protocolUids = Arrays.asList(buddyProtocolUid);
		}

		@Override
		public boolean match(Page t) {
			boolean found = false;
			
			for (String protocolUid : protocolUids) {
				found |= (t instanceof IHasBuddy) && ((IHasBuddy)t).hasThisBuddy(serviceId, protocolUid);
			}
			return found;
		}				
	}
	
	public static final class MessagePageLinqRule implements KindaLinqRule<Page> {
		
		private final byte serviceId;
		private final String buddyUid;

		public MessagePageLinqRule(byte serviceId, String buddyUid) {
			this.serviceId = serviceId;
			this.buddyUid = buddyUid;
		}

		@Override
		public boolean match(Page t) {
			return (t instanceof IHasMessages) && ((IHasMessages)t).hasMessagesOfBuddy(serviceId, buddyUid);
		}		
	}
	
	public static final class BuddyLinqRule implements KindaLinqRule<Buddy> {
		
		private final Buddy b;

		public BuddyLinqRule(Buddy b) {
			this.b = b;
		}

		@Override
		public boolean match(Buddy bu) {
			return bu == b || (bu.getServiceId() == b.getServiceId() && bu.getProtocolUid().equals(b.getProtocolUid()));
		}				
	}
	
	public static final class ChatLinqRule implements KindaLinqRule<Page> {
		
		private final Buddy b;

		public ChatLinqRule(Buddy b) {
			this.b = b;
		}

		@Override
		public boolean match(Page t) {
			if (t instanceof Chat) {
				Buddy bu = ((Chat)t).getBuddy();
				return bu.getServiceId() == b.getServiceId() && bu.getProtocolUid().equals(b.getProtocolUid());
			} else {
				return false;
			}
		}				
	}
	
	public static final class PageIdLinqRule implements KindaLinqRule<Page> {
		
		private final String pageId;

		public PageIdLinqRule(String pageId) {
			this.pageId = pageId;
		}

		@Override
		public boolean match(Page t) {
			return t.getPageId().equals(pageId);
		}		
	}
	
	public static final class StringCompareLinqRule implements KindaLinqRule<String> {
		
		private final String s;
		
		public StringCompareLinqRule(String s) {
			this.s = s;
		}

		@Override
		public boolean match(String t) {
			return s.equals(t);
		}
	}
	
	public static final class PersonalInfoLinqRule implements KindaLinqRule<Page> {
		
		private final PersonalInfo info;

		public PersonalInfoLinqRule(PersonalInfo info) {
			this.info = info;
		}

		@Override
		public boolean match(Page t) {
			
			return (t instanceof PersonalInfoPage)
						&& ((PersonalInfoPage) t).getInfo().getServiceId() == info.getServiceId() 
						&& ((PersonalInfoPage) t).getInfo().getProtocolUid().equals(info.getProtocolUid());
		}
		
	}
	
	public static final class PageWithSmileysLinqRule implements KindaLinqRule<Page> {

		@Override
		public boolean match(Page t) {
			return t instanceof Chat || t instanceof History;
		}
		
	}
}
