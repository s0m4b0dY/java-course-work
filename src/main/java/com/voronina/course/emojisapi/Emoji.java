package com.voronina.course.emojisapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voronina.course.ApiObject;

import java.util.List;

public class Emoji implements ApiObject {
	static final String[] CSV_HEADERS = new String[] {
		"name",
		"category",
		"group",
		"htmlCode",
		"unicode"
	};

	private String name;
	private String category;
	private String group;
	private List<String> htmlCode;
	private List<String> unicode;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public List<String> getHtmlCode() {
		return htmlCode;
	}

	public void setHtmlCode(List<String> htmlCode) {
		this.htmlCode = htmlCode;
	}

	public List<String> getUnicode() {
		return unicode;
	}

	public void setUnicode(List<String> unicode) {
		this.unicode = unicode;
	}

	// JSON convenience
	public static Emoji fromJson(String json) {
		return new GsonBuilder().create().fromJson(json, Emoji.class);
	}

	public String toJson() {
		return new GsonBuilder().create().toJson(this);
	}

	@Override
	public String[] toCsvFields() {
		return new String[] {
			name != null ? name : "",
			category != null ? category : "",
			group != null ? group : "",
			htmlCode != null ? String.join(";", htmlCode) : "",
			unicode != null ? String.join(";", unicode) : ""
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
