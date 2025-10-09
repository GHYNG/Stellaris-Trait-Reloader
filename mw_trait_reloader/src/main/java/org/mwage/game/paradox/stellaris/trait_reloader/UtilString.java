package org.mwage.game.paradox.stellaris.trait_reloader;
public interface UtilString {
	static String removeEmptyLines(Object text) {
		String input = STR."\{text}";
		if(input.isEmpty()) {
			return input;
		}
		String[] lines = input.split("\r\n|\n|\r");
		StringBuilder result = new StringBuilder();
		for(String line : lines) {
			if(!line.trim().isEmpty()) {
				result.append(line).append(System.lineSeparator());
			}
		}
		if(result.length() > 0) {
			result.setLength(result.length() - System.lineSeparator().length());
		}
		return result.toString();
	}
}