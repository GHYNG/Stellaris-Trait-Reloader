package org.mwage.game.paradox.stellaris.trait_reloader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ProcessorFilesTrait implements Config {
	static Map<File, List<String>> linessTraitsInput = new HashMap<>();
	public static void main(String... args) throws IOException {
		FOLDER_PROCESSOR_INPUT_TRAITS.mkdirs();
		FOLDER_PROCESSOR_OUTPUT_TRAITS.mkdirs();
		inputAllFiles();
		System.out.println(linessTraitsInput);
	}
	static void inputAllFiles() throws IOException {
		File[] filesTraitsInput = FOLDER_PROCESSOR_INPUT_TRAITS.listFiles();
		for(File fileTraitsInput : filesTraitsInput) {
			List<String> linesTraitInput = Files.readAllLines(fileTraitsInput.toPath());
			linessTraitsInput.put(fileTraitsInput, linesTraitInput);
		}
	}
	static void cleanLiness() {
		for(File fileTraitsInput : linessTraitsInput.keySet()) {
			List<String> lines = linessTraitsInput.get(fileTraitsInput);
			cleanLines(lines);
		}
	}
	static void cleanLines(List<String> lines) {
		List<String> linesCache = new ArrayList<>();
	}
}