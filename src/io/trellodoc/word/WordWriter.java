package io.trellodoc.word;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList; //new
import java.util.Arrays; //new

/*import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;*/
import org.apache.poi.xwpf.usermodel.*; //new
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumbering;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.VisitHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import io.trellodoc.model.TrelloCard;
import io.trellodoc.model.TrelloList;

public class WordWriter {
	private static final String MARKER_MAIN = "[[MAIN]]";
	private XWPFParagraph hookParagraph;
	private XWPFDocument document;
	private XWPFParagraph currentParagraph;
	private XWPFTableCell currentCell;
	private BigInteger currentNumId;

	private NodeVisitor visitor = new NodeVisitor(new VisitHandler<>(Emphasis.class, this::visitEmphasis),
			new VisitHandler<>(Text.class, this::visitText), new VisitHandler<>(Paragraph.class, this::visitParagraph),
			new VisitHandler<>(BulletList.class, this::visitBulletList),
			new VisitHandler<>(BulletListItem.class, this::visitBulletListItem),
			new VisitHandler<>(OrderedList.class, this::visitOrderedList),
			new VisitHandler<>(OrderedListItem.class, this::visitOrderedListItem));
	private boolean firstParagraph;
	private boolean currentIsListItem;
	private int numId=100;

	public WordWriter(String templateUrl) throws TrelloDocException {
		this.document = getTemplateDocument(templateUrl);
		this.hookParagraph = getHookParagraph(document);
	}

	public void visitEmphasis(Emphasis arg0) {
		XWPFRun run = this.currentParagraph.createRun();
		run.setItalic(true);
		run.setText(arg0.getChildChars().toString());
	}

	public void visitParagraph(Paragraph par) {
		if (this.firstParagraph) {
			this.firstParagraph = false;
		} else {
			this.currentParagraph = this.currentCell.addParagraph();
		}
		if (this.currentIsListItem) {
			this.currentParagraph.setNumID(this.currentNumId);
		}
		visitor.visitChildren(par);
	}

	public void visitText(Text arg0) {
		XWPFRun run = this.currentParagraph.createRun();
		run.setText(arg0.getChars().toString());
	}

	public void visitBulletList(BulletList list) {
		try {
			this.currentNumId = createNumbering();
			visitor.visitChildren(list);
		} catch (XmlException e) {
			System.err.println("Error while creating numbering.");
		}
	}

	public void visitBulletListItem(BulletListItem listItem) {
		this.currentIsListItem = true;
		visitor.visitChildren(listItem);
		this.currentIsListItem = false;
	}
	
	public void visitOrderedList(OrderedList list) {
		try {
			this.currentNumId = createNumbering();
			visitor.visitChildren(list);
		} catch (XmlException e) {
			System.err.println("Error while creating numbering.");
		}
	}

	public void visitOrderedListItem(OrderedListItem listItem) {
		this.currentIsListItem = true;
		visitor.visitChildren(listItem);
		this.currentIsListItem = false;
	}
	

	public void writeTo(String targetUrl, List<TrelloList> trelloLists, String headerStyle) throws TrelloDocException {
		XmlCursor cursor = this.hookParagraph.getCTP().newCursor();
		for (TrelloList list : trelloLists) {
			XWPFParagraph listParagraph = document.insertNewParagraph(cursor);
			listParagraph.setStyle(headerStyle);
			XWPFRun run = listParagraph.createRun();
			run.setText(list.getName());
			cursor.toNextToken();
			writeCards(cursor, list);
		}

		removeParagraph(this.hookParagraph);

		try {
			this.document.write(new FileOutputStream(new File(new URI(targetUrl))));
		} catch (IOException | URISyntaxException e) {
			System.err.println("Could not write to file " + targetUrl);
			throw new TrelloDocException(e);
		}

	}

	private void writeCards(XmlCursor cursor, TrelloList list) {

		MutableDataSet options = new MutableDataSet();
		Parser parser = Parser.builder(options).build();

		for (TrelloCard card : list.getCards()) {
			if (card.getLabels().contains("Baseline 4") || card.getLabels().contains("Rejected")) {
				break; }
			document.insertNewParagraph(cursor);
			cursor.toNextToken();
			XWPFTable table = document.insertNewTbl(cursor);
			
			if (card.getLabels() == "") {
				XWPFTableRow row1 = table.getRow(0);
				row1.getCell(0).setText("ID");
				row1.addNewTableCell().setText(Integer.toString(card.getId()));
			}
			else {
				XWPFTableRow row0 = table.getRow(0);
				row0.getCell(0).setText("Label"); //new
				row0.addNewTableCell().setText(card.getLabels()); //new
			
				XWPFTableRow row1 = table.createRow(); //new
				row1.getCell(0).setText("ID");
				row1.getCell(1).setText(Integer.toString(card.getId()));
			}

			XWPFTableRow row2 = table.createRow();
			row2.getCell(0).setText("Name");
			row2.getCell(1).setText(card.getName());

			String description = card.getDescription();
			
			Document userStory = parser.parse(description.split("Akzeptanzkriterien:")[0]);
			XWPFTableRow row3 = table.createRow();
			row3.getCell(0).setText("User Story");
			this.currentCell = row3.getCell(1);
			this.currentParagraph = row3.getCell(1).getParagraphs().get(0);
			this.firstParagraph = true;
			visitor.visit(userStory);
			
			if (description.split("Akzeptanzkriterien:").length > 1) {
				Document akzeptanzkriterien = parser.parse(description.split("Akzeptanzkriterien:")[1]);
				XWPFTableRow row4 = table.createRow();
				row4.getCell(0).setText("Akzeptanzkriterien");
				this.currentCell = row4.getCell(1);
				this.currentParagraph = row4.getCell(1).getParagraphs().get(0);
				this.firstParagraph = true;
				visitor.visit(akzeptanzkriterien);
				}

			cursor.toNextToken();
		}

	}

	private BigInteger createNumbering() throws XmlException {
		//XML for Bullet Points:
		/*String cTAbstractNumBulletXML = "<w:abstractNum xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" w:abstractNumId=\"0\">"
				+ "<w:multiLevelType w:val=\"hybridMultilevel\"/>"
				+ "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"720\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Symbol\" w:hAnsi=\"Symbol\" w:hint=\"default\"/></w:rPr></w:lvl>"
				+ "<w:lvl w:ilvl=\"1\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"o\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"1440\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:cs=\"Courier New\" w:hint=\"default\"/></w:rPr></w:lvl>"
				+ "<w:lvl w:ilvl=\"2\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"bullet\"/><w:lvlText w:val=\"\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"2160\" w:hanging=\"360\"/></w:pPr><w:rPr><w:rFonts w:ascii=\"Wingdings\" w:hAnsi=\"Wingdings\" w:hint=\"default\"/></w:rPr></w:lvl>"
				+ "</w:abstractNum>";*/
		
		String cTAbstractNumBulletXML = "<w:abstractNum xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" w:abstractNumId=\""+ (this.numId) +"\">"
                + "<w:multiLevelType w:val=\"hybridMultilevel\"/>"
                + "<w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlRestart w:val=\"1\"/><w:lvlText w:val=\"%1.\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"720\" w:hanging=\"360\"/></w:pPr></w:lvl>"
                + "<w:lvl w:ilvl=\"1\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%1.%2\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"1440\" w:hanging=\"360\"/></w:pPr></w:lvl>"
                + "<w:lvl w:ilvl=\"2\" w:tentative=\"1\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%1.%2.%3\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"2160\" w:hanging=\"360\"/></w:pPr></w:lvl>"
                + "</w:abstractNum>";

		CTNumbering cTNumbering = CTNumbering.Factory.parse(cTAbstractNumBulletXML);
		CTAbstractNum cTAbstractNum = cTNumbering.getAbstractNumArray(0);
		
		XWPFAbstractNum abstractNum = new XWPFAbstractNum(cTAbstractNum);
		XWPFNumbering numbering = document.createNumbering();
		BigInteger abstractNumID = numbering.addAbstractNum(abstractNum);
		
		this.numId++;
		return numbering.addNum(abstractNumID);
	}

	private void removeParagraph(XWPFParagraph hookParagraph2) {
		int pos = this.document.getPosOfParagraph(this.hookParagraph);
		this.document.removeBodyElement(pos);
	}

	private XWPFParagraph getHookParagraph(XWPFDocument document) throws TrelloDocException {
		for (XWPFParagraph paragraph : document.getParagraphs()) {
			if (paragraph.getText().equals(MARKER_MAIN)) {
				return paragraph;
			}
		}
		System.err.println("Could not find " + MARKER_MAIN + " marker");
		throw new TrelloDocException();
	}

	private XWPFDocument getTemplateDocument(String templateUri) throws TrelloDocException {
		File file;
		try {
			file = new File(new URI(templateUri));
		} catch (URISyntaxException e) {
			System.err.println("Illegal URI syntax: " + templateUri);
			throw new TrelloDocException(e);
		}
		FileInputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + templateUri);
			throw new TrelloDocException(e);
		}
		try {
			XWPFDocument document = new XWPFDocument(is);
			return document;
		} catch (IOException e) {
			System.err.println("Could not load document: " + templateUri);
			throw new TrelloDocException(e);
		}
	}
}
