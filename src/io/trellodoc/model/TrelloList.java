package io.trellodoc.model;

import java.util.ArrayList;
import java.util.List;

public class TrelloList {
	private String name;
	private String id;
	private List<TrelloCard> cards = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void addCard(TrelloCard card) {
		cards.add(card);
	}

	public List<TrelloCard> getCards() {
		return this.cards;
	}

}
