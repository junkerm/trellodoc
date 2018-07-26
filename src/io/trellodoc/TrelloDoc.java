package io.trellodoc;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.trellodoc.model.TrelloList;
import io.trellodoc.trello.TrelloBoardReader;
import io.trellodoc.word.TrelloDocException;
import io.trellodoc.word.WordWriter;

public class TrelloDoc {

	public static void main(String[] args) {
		CommandLine cmd = null;
		try {
			cmd = parseCommandLineArgs(args);
		} catch (ParseException e) {
			System.err.println(e.getLocalizedMessage());
			return;
		}

		String key = cmd.getOptionValue("trelloKey");
		String token = cmd.getOptionValue("trelloToken");
		String boardId = cmd.getOptionValue("boardId");
		String templateUrl = cmd.getOptionValue("templateUrl");
		String targetUrl = cmd.getOptionValue("targetUrl");
		String headerStyle = cmd.getOptionValue("headerStyle");

		TrelloBoardReader reader = new TrelloBoardReader(key, token, boardId);
		List<TrelloList> trelloLists = reader.getLists();
		try {
			WordWriter writer = new WordWriter(templateUrl);
			writer.writeTo(targetUrl, trelloLists, headerStyle);
		} catch (TrelloDocException e) {
			System.err.println("Generation failed.");
			return;
		}
		System.out.println("finished");
	}

	/**
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	private static CommandLine parseCommandLineArgs(String[] args) throws ParseException {
		Option trelloKey = OptionBuilder.withArgName("key").hasArg().isRequired().create("trelloKey");
		Option trelloToken = OptionBuilder.withArgName("token").isRequired().hasArg().create("trelloToken");
		Option boardId = OptionBuilder.withArgName("id").hasArg().isRequired().create("boardId");
		Option templateUrl = OptionBuilder.withArgName("url").hasArg().isRequired().create("templateUrl");
		Option targetUrl = OptionBuilder.withArgName("url").hasArg().isRequired().create("targetUrl");
		Option headerStyle = OptionBuilder.withArgName("style").hasArg().isRequired().create("headerStyle");

		Options options = new Options();
		options.addOption(trelloKey);
		options.addOption(trelloToken);
		options.addOption(boardId);
		options.addOption(templateUrl);
		options.addOption(targetUrl);
		options.addOption(headerStyle);

		CommandLineParser parser = new DefaultParser();
		return parser.parse(options, args);
	}

}
