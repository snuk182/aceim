package aceim.app.themeable;

import java.lang.reflect.Constructor;

import aceim.api.utils.Logger;
import aceim.app.R;
import aceim.app.themeable.dataentity.ChatMessageItemThemeResource;
import aceim.app.themeable.dataentity.ContactListGridItemThemeResource;
import aceim.app.themeable.dataentity.ContactListPlainItemThemeResource;
import aceim.app.themeable.dataentity.HistoryMessageItemThemeResource;
import aceim.app.themeable.dataentity.TabThemeResource;
import aceim.app.themeable.dataentity.ThemeResource;
import android.content.Context;

public class ThemeResources {
	//Layouts
	private final ContactListGridItemThemeResource gridItemLayout;
	private final ContactListPlainItemThemeResource listItemLayout;
	private final ChatMessageItemThemeResource chatMessageItemLayout;
	private final HistoryMessageItemThemeResource historyMessageItemLayout;
	private final TabThemeResource tabItemLayout;
	
	private final int topBarBackground;
	private final int bottomBarBackground;
	private final int screenBackground;
	private final int bottomBarButtonBackground;
	private final int accentBackground;
	private final int gridItemSize;
	private final int listItemHeight;
	
	private ThemeResources(ContactListGridItemThemeResource gridItemLayout, ContactListPlainItemThemeResource listItemLayout, ChatMessageItemThemeResource chatMessageItemLayout, HistoryMessageItemThemeResource historyMessageItemLayout,
			TabThemeResource tabItemLayout, int topBarBackground, int bottomBarBackground, int screenBackground, int bottomBarButtonBackground, int accentBackground, int gridItemSize, int listItemHeight) {
		super();
		this.gridItemLayout = gridItemLayout;
		this.listItemLayout = listItemLayout;
		this.chatMessageItemLayout = chatMessageItemLayout;
		this.historyMessageItemLayout = historyMessageItemLayout;
		this.tabItemLayout = tabItemLayout;
		this.topBarBackground = topBarBackground;
		this.bottomBarBackground = bottomBarBackground;
		this.screenBackground = screenBackground;
		this.bottomBarButtonBackground = bottomBarButtonBackground;
		this.accentBackground = accentBackground;
		this.gridItemSize = gridItemSize;
		this.listItemHeight = listItemHeight;
	}

	/**
	 * @return the topBarBackground
	 */
	public int getTopBarBackgroundId() {
		return topBarBackground;
	}


	/**
	 * @return the bottomBarBackground
	 */
	public int getBottomBarBackgroundId() {
		return bottomBarBackground;
	}


	/**
	 * @return the screenBackground
	 */
	public int getScreenBackgroundId() {
		return screenBackground;
	}


	/**
	 * @return the bottomBarButtonBackground
	 */
	public int getBottomBarButtonBackgroundId() {
		return bottomBarButtonBackground;
	}


	/**
	 * @return the accentBackground
	 */
	public int getAccentBackgroundId() {
		return accentBackground;
	}


	/**
	 * @return the gridItemSize
	 */
	public int getGridItemSizeId() {
		return gridItemSize;
	}


	/**
	 * @return the listItemHeight
	 */
	public int getListItemHeightId() {
		return listItemHeight;
	}


	/**
	 * @return the gridItemLayout
	 */
	public ContactListGridItemThemeResource getGridItemLayout() {
		return gridItemLayout;
	}


	/**
	 * @return the listItemLayout
	 */
	public ContactListPlainItemThemeResource getListItemLayout() {
		return listItemLayout;
	}


	/**
	 * @return the chatMessageItemLayout
	 */
	public ChatMessageItemThemeResource getChatMessageItemLayout() {
		return chatMessageItemLayout;
	}


	/**
	 * @return the historyMessageItemLayout
	 */
	public HistoryMessageItemThemeResource getHistoryMessageItemLayout() {
		return historyMessageItemLayout;
	}


	/**
	 * @return the tabItemLayout
	 */
	public TabThemeResource getTabItemLayout() {
		return tabItemLayout;
	}


	public static class Builder {
		
		//Layouts
		private ContactListGridItemThemeResource gridItemLayout;
		private ContactListPlainItemThemeResource listItemLayout;
		private ChatMessageItemThemeResource chatMessageItemLayout;
		private HistoryMessageItemThemeResource historyMessageItemLayout;
		private TabThemeResource tabItemLayout;
		
		//Identifiers
		private int topBarBackground;
		private int bottomBarBackground;
		private int screenBackground;
		private int bottomBarButtonBackground;
		private int accentBackground;
		private int gridItemSize;
		private int listItemHeight;
		
		
		public ThemeResources build() {
			return new ThemeResources(
					gridItemLayout, 
					listItemLayout, 
					chatMessageItemLayout, 
					historyMessageItemLayout, 
					tabItemLayout, 
					topBarBackground, 
					bottomBarBackground, 
					screenBackground, 
					bottomBarButtonBackground, 
					accentBackground, 
					gridItemSize, 
					listItemHeight
				);
		}

		public ThemeResources.Builder resourcesFromThemesManager(ThemesManager tm) {
			if (tm != null) {
				Context themeContext = tm.getCurrentThemeContext();
				Context nativeContext = tm.getContext();
				
				gridItemLayout = getResource(ContactListGridItemThemeResource.class, themeContext, nativeContext, "contact_list_grid_item", "layout", R.layout.contact_list_grid_item);
				listItemLayout = getResource(ContactListPlainItemThemeResource.class, themeContext, nativeContext, "contact_list_plain_item", "layout", R.layout.contact_list_plain_item);				
				chatMessageItemLayout = getResource(ChatMessageItemThemeResource.class, themeContext, nativeContext, "chat_message", "layout", R.layout.chat_message);		
				tabItemLayout = getResource(TabThemeResource.class, themeContext, nativeContext, "tab_indicator", "layout", R.layout.tab_indicator);
				//TODO
				historyMessageItemLayout = new HistoryMessageItemThemeResource(nativeContext, R.layout.history_message);
				
				accentBackground = themeContext.getResources().getIdentifier("accent_background", "drawable", themeContext.getPackageName());
				topBarBackground = themeContext.getResources().getIdentifier("top_bar_background", "drawable", themeContext.getPackageName());
				bottomBarBackground = themeContext.getResources().getIdentifier("bottom_bar_background", "drawable", themeContext.getPackageName());
				screenBackground = themeContext.getResources().getIdentifier("screen_background", "drawable", themeContext.getPackageName());
				bottomBarButtonBackground = themeContext.getResources().getIdentifier("bottom_bar_button_background", "drawable", themeContext.getPackageName());
				gridItemSize = themeContext.getResources().getIdentifier("grid_item_size", "dimen", themeContext.getPackageName());
				listItemHeight = themeContext.getResources().getIdentifier("list_item_height", "dimen", themeContext.getPackageName());
			}
			
			return this;
		}
		
		private <T extends ThemeResource> T getResource(Class<T> cls, Context themeContext, Context nativeContext, String idName, String idType, int fallbackResourceId) {
			int id = themeContext.getResources().getIdentifier(idName, idType, themeContext.getPackageName());
			
			T resource = null;
			try {
				Constructor<T> constructor = cls.getConstructor(Context.class, Integer.TYPE);
				
				if (id != 0) {
					resource = constructor.newInstance(themeContext, id);
				} else {
					resource = constructor.newInstance(nativeContext, fallbackResourceId);
				}
			} catch (Exception e) {
				Logger.log(e);
			}
			
			return resource;
		}

		/**
		 * @param gridItemLayout the gridItemLayout to set
		 * @return 
		 */
		public Builder gridItemLayout(ContactListGridItemThemeResource gridItemLayout) {
			this.gridItemLayout = gridItemLayout;

			return this;
		}

		/**
		 * @param listItemLayout the listItemLayout to set
		 * @return 
		 */
		public Builder listItemLayout(ContactListPlainItemThemeResource listItemLayout) {
			this.listItemLayout = listItemLayout;

			return this;
		}

		/**
		 * @param chatMessageItemLayout the chatMessageItemLayout to set
		 * @return 
		 */
		public Builder chatMessageItemLayout(ChatMessageItemThemeResource chatMessageItemLayout) {
			this.chatMessageItemLayout = chatMessageItemLayout;

			return this;
		}
		
		public Builder historyMessageItemLayout(HistoryMessageItemThemeResource historyMessageItemLayout) {
			this.historyMessageItemLayout = historyMessageItemLayout;

			return this;
		}
		
		public Builder tabItemLayout(TabThemeResource tabItemLayout) {
			this.tabItemLayout = tabItemLayout;

			return this;
		}

		public Builder topBarBackgroundId(int topBarBackground) {
			this.topBarBackground = topBarBackground;
			
			return this;
		}

		public Builder bottomBarBackgroundId(int bottomBarBackground) {
			this.bottomBarBackground = bottomBarBackground;
			
			return this;
		}

		public Builder screenBackgroundId(int screenBackground) {
			this.screenBackground = screenBackground;
			
			return this;
		}

		public Builder bottomBarButtonBackgroundId(int bottomBarButtonBackground) {
			this.bottomBarButtonBackground = bottomBarButtonBackground;
			
			return this;
		}

		public Builder accentBackgroundId(int accentBackground) {
			this.accentBackground = accentBackground;
			
			return this;
		}

		public Builder gridItemSizeId(int gridItemSize) {
			this.gridItemSize = gridItemSize;
			
			return this;
		}

		public Builder listItemHeightId(int listItemHeight) {
			this.listItemHeight = listItemHeight;
			
			return this;
		}
	}
}
