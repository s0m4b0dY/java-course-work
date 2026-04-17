package com.voronina.course.lastmessageapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.stream.Collectors;
import com.voronina.course.ApiObject;

public class LastMessage implements ApiObject {
  static final String[] CSV_HEADERS = new String[] {
      "id",
      "text",
      "sent_at",
      "read",
      "sender_id",
      "sender_name",
      "sender_avatar_url"
      ,
      "reactions"
    };

  private long id;
  private String text;
  private String sent_at;
  private boolean read;
  private Sender sender;
  private List<Reaction> reactions;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
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

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  public Sender getSender() {
    return sender;
  }

  public void setSender(Sender sender) {
    this.sender = sender;
  }

  public List<Reaction> getReactions() {
    return reactions;
  }

  public void setReactions(List<Reaction> reactions) {
    this.reactions = reactions;
  }

  public static class Sender {
    private long id;
    private String name;
    private String avatarUrl;

    public Sender() {}

    public Sender(long id, String name, String avatarUrl) {
      this.id = id;
      this.name = name;
      this.avatarUrl = avatarUrl;
    }

    public long getId() {
      return id;
    }

    public void setId(long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAvatarUrl() {
      return avatarUrl;
    }

    public void setAvatarUrl(String avatar_url) {
      this.avatarUrl = avatar_url;
    }
  }

  public static class Reaction {
    private String emoji;
    private int count;

    public Reaction() {}

    public Reaction(String emoji, int count) {
      this.emoji = emoji;
      this.count = count;
    }

    public String getEmoji() {
      return emoji;
    }

    public void setEmoji(String emoji) {
      this.emoji = emoji;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }
  }

  // JSON convenience
  public static LastMessage fromJson(String json) {
    Gson g = new GsonBuilder().create();
    return g.fromJson(json, LastMessage.class);
  }

  public String toJson() {
    Gson g = new GsonBuilder().create();
    return g.toJson(this);
  }

  @Override
  public String[] toCsvFields() {
    return new String[] {
      String.valueOf(id),
      text != null ? text : "",
      sent_at != null ? sent_at : "",
      String.valueOf(read),
      sender != null ? String.valueOf(sender.id) : "",
      sender != null ? sender.name : "",
      sender != null ? sender.avatarUrl : "",
      reactions != null
        ? reactions.stream().map(r -> r.getEmoji() + ":" + r.getCount()).collect(Collectors.joining(";"))
        : ""
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
