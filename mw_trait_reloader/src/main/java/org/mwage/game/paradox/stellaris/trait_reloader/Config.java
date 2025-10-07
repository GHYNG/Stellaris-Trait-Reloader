package org.mwage.game.paradox.stellaris.trait_reloader;
import java.io.File;
public interface Config {
	File FOLDER_PROCESSOR = new File("processor");
	File FOLDER_PROCESSOR_INPUT = new File(FOLDER_PROCESSOR, "input");
	File FOLDER_PROCESSOR_OUTPUT = new File(FOLDER_PROCESSOR, "output");
	File FOLDER_PROCESSOR_INPUT_TRAITS = new File(FOLDER_PROCESSOR_INPUT, "traits");
	File FOLDER_PROCESSOR_OUTPUT_TRAITS = new File(FOLDER_PROCESSOR_OUTPUT, "traits");
}