package io.trellodoc.model;

public class TrelloCard {
	private int id;
	private String name;
	private String description;
	private String labels;
	
	public String getLabels() {
		return labels;
	}
	
	public void setLabels(String labels) {
		this.labels = labels;
	}

	public int getId() {
		return id;
	}

	public void setId(int i) {
		this.id = i;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}



}
