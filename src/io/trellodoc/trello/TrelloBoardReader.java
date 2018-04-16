package io.trellodoc.trello;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONObject;

import io.trellodoc.model.TrelloCard;
import io.trellodoc.model.TrelloList;

public class TrelloBoardReader {
	private static final String TRELLO_BASE_URL = "https://api.trello.com";
	private RestClient client;
	private String boardId;
	private String token;
	private String key;

	public TrelloBoardReader(String key, String token, String boardId) {
		this.client = new RestClient(TRELLO_BASE_URL);
		this.boardId = boardId;
		this.key = key;
		this.token = token;
	}

	public List<TrelloCard> getCards() {
		RestResult<JSONArray> restResult = client.getList("/1/boards/" + boardId + "/cards", "key", this.key, "token",
				this.token);
		if (restResult.getResponse().getStatus() == Status.OK.getStatusCode()) {
			List<TrelloCard> cards = new ArrayList<>();
			JSONArray cardsArray = restResult.getPayload();
			for (int i = 0; i < cardsArray.length(); i++) {
				JSONObject cardObject = cardsArray.getJSONObject(i);
				TrelloCard card = makeTrelloCard(cardObject);
				cards.add(card);
			}
			return cards;
		}
		return null;
	}

	private TrelloCard makeTrelloCard(JSONObject cardObject) {
		TrelloCard card = new TrelloCard();
		card.setName(cardObject.getString("name"));
		card.setDescription(cardObject.getString("desc"));
		card.setId(cardObject.getInt("idShort"));
		return card;
	}

	public List<TrelloList> getLists() {
		RestResult<JSONArray> restResult = client.getList("/1/boards/" + boardId + "/lists", "cards", "open", "key",
				this.key, "token", this.token);
		if (restResult.getResponse().getStatus() == Status.OK.getStatusCode()) {
			List<TrelloList> lists = new ArrayList<>();
			JSONArray listsArray = restResult.getPayload();
			for (int i = 0; i < listsArray.length(); i++) {
				JSONObject listObject = listsArray.getJSONObject(i);
				TrelloList list = makeTrelloList(listObject);
				lists.add(list);
			}
			return lists;
		}
		return null;
	}

	private TrelloList makeTrelloList(JSONObject listObject) {
		TrelloList list = new TrelloList();
		list.setName(listObject.getString("name"));
		list.setId(listObject.getString("id"));
		if (listObject.has("cards")) {
			JSONArray cardsArray = listObject.getJSONArray("cards");
			for (int i = 0; i < cardsArray.length(); i++) {
				JSONObject cardObject = cardsArray.getJSONObject(i);
				TrelloCard card = makeTrelloCard(cardObject);
				list.addCard(card);
			}
		}
		return list;
	}

}