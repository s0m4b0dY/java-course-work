package com.voronina.course.chatsapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voronina.course.ApiObject;

public class Chat implements ApiObject {
	static final String[] CSV_HEADERS = new String[] {
		"chat_id",
		"name",
		"unread_count",
		"is_muted",
		"last_activity_text",
		"last_activity_sent_at",
		"last_activity_author"
	};

	private long chat_id;
	private String name;
	private int unread_count;
	private boolean is_muted;
	private LastActivity last_activity;

	public long getChat_id() {
		return chat_id;
	}

	public void setChat_id(long chat_id) {
		this.chat_id = chat_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUnread_count() {
		return unread_count;
	}

	public void setUnread_count(int unread_count) {
		this.unread_count = unread_count;
	}

	public boolean isIs_muted() {
		return is_muted;
	}

	public void setIs_muted(boolean is_muted) {
		this.is_muted = is_muted;
	}

	public LastActivity getLast_activity() {
		return last_activity;
	}

	public void setLast_activity(LastActivity last_activity) {
		this.last_activity = last_activity;
	}

	public static class LastActivity {
		private String text;
		private String sent_at;
		private String author;

		public LastActivity() {}

		public LastActivity(String text, String sent_at, String author) {
			this.text = text;
			this.sent_at = sent_at;
			this.author = author;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getSent_at() {
			return sent_at;
		}

		public void setSent_at(String sent_at) {
			this.sent_at = sent_at;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}
	}

	// JSON convenience
	public static Chat fromJson(String json) {
		Gson g = new GsonBuilder().create();
		return g.fromJson(json, Chat.class);
	}

	public String toJson() {
		Gson g = new GsonBuilder().create();
		return g.toJson(this);
	}

	@Override
	public String[] toCsvFields() {
		return new String[] {
			String.valueOf(chat_id),
			name != null ? name : "",
			String.valueOf(unread_count),
			String.valueOf(is_muted),
			last_activity != null && last_activity.getText() != null ? last_activity.getText() : "",
			last_activity != null && last_activity.getSent_at() != null ? last_activity.getSent_at() : "",
			last_activity != null && last_activity.getAuthor() != null ? last_activity.getAuthor() : ""
		};
	}

	@Override
	public String[] csvHeaders() {
		return CSV_HEADERS;
	}

	@Override
	public Gson toGson() {
		return new GsonBuilder().serializeNulls().create();
	}
}
