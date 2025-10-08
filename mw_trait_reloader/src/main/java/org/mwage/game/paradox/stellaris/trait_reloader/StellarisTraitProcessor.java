package org.mwage.game.paradox.stellaris.trait_reloader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// Thank you, DeepSeek
public class StellarisTraitProcessor {
	private static final String INPUT_DIR = "processor/input/traits";
	private static final String OUTPUT_DIR = "processor/output/traits";
	public static void main(String[] args) {
		try {
			processTraits();
			System.out.println("特质文件处理完成！");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	public static void produceEmptyFiles() {
	}
	public static void processTraits() throws IOException {
		Path inputPath = Paths.get(INPUT_DIR);
		Path outputPath = Paths.get(OUTPUT_DIR);
		// 创建输出目录
		if(!Files.exists(outputPath)) {
			Files.createDirectories(outputPath);
		}
		// 遍历输入目录中的所有文件
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.txt")) {
			for(Path filePath : stream) {
				processFile(filePath, outputPath);
			}
		}
	}
	private static void processFile(Path inputFile, Path outputDir) throws IOException {
		String fileName = inputFile.getFileName().toString();
		String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
		System.out.println("处理文件: " + fileName);
		String content = Files.readString(inputFile);
		// 提取文件级变量
		List<Variable> fileVariables = extractFileVariables(content);
		// 提取所有特质
		List<Trait> traits = extractTraits(content);
		// 按特质名分组（去除数字后缀）
		Map<String, List<Trait>> traitGroups = new LinkedHashMap<>();
		for(Trait trait : traits) {
			String baseTraitName = getBaseTraitName(trait.name);
			traitGroups.computeIfAbsent(baseTraitName, k -> new ArrayList<>()).add(trait);
		}
		// 为每个特质组生成文件
		int traitCount = traits.size();
		int padding = String.valueOf(traitCount).length();
		/*
		 * DeepSeek给的代码会将不同等级的相同特质独立计算出现顺序，因此文件名中的序号会出现跳过部分序号的情况（之前出现多级特质）。
		 * 因此使用offset来将之后的序号减回去。
		 */
		int offset = 0;
		for(Map.Entry<String, List<Trait>> entry : traitGroups.entrySet()) {
			String baseTraitName = entry.getKey();
			List<Trait> traitGroup = entry.getValue();
			// 使用组中第一个特质的索引
			int firstIndex = traits.indexOf(traitGroup.get(0)) + 1 - offset;
			String indexStr = String.format("%0" + padding + "d", firstIndex);
			String outputFileName = baseName + "_" + indexStr + "_" + baseTraitName + ".txt";
			Path outputFile = outputDir.resolve(outputFileName);
			writeTraitFile(outputFile, traitGroup, fileVariables);
			offset += traitGroup.size() - 1;
		}
		Path emptyFile = outputDir.resolve(inputFile.getFileName().toString());
		Files.writeString(emptyFile, "", StandardCharsets.UTF_8);
	}
	private static List<Variable> extractFileVariables(String content) {
		List<Variable> variables = new ArrayList<>();
		// 匹配文件级变量定义：@variable_name = value
		Pattern pattern = Pattern.compile("^@(\\w+)\\s*=\\s*(.+)$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String name = matcher.group(1);
			String value = matcher.group(2).trim();
			variables.add(new Variable(name, value));
		}
		return variables;
	}
	private static List<Trait> extractTraits(String content) {
		List<Trait> traits = new ArrayList<>();
		// 匹配特质定义：trait_name = { ... }
		Pattern pattern = Pattern.compile("^(\\w+)\\s*=\\s*\\{", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(content);
		while(matcher.find()) {
			String traitName = matcher.group(1);
			int start = matcher.start();
			// 找到匹配的右大括号
			int braceCount = 0;
			int end = start;
			boolean inString = false;
			char stringChar = '\0';
			for(int i = start; i < content.length(); i++) {
				char c = content.charAt(i);
				if(!inString) {
					if(c == '"' || c == '\'') {
						inString = true;
						stringChar = c;
					}
					else if(c == '{') {
						braceCount++;
					}
					else if(c == '}') {
						braceCount--;
						if(braceCount == 0) {
							end = i + 1;
							break;
						}
					}
				}
				else {
					if(c == stringChar && content.charAt(i - 1) != '\\') {
						inString = false;
					}
				}
			}
			if(end > start) {
				String traitContent = content.substring(start, end);
				traits.add(new Trait(traitName, traitContent));
			}
		}
		return traits;
	}
	private static String getBaseTraitName(String traitName) {
		// 去除数字后缀，如 _2, _3 等
		return traitName.replaceAll("_\\d+$", "");
	}
	private static void writeTraitFile(Path outputFile, List<Trait> traits, List<Variable> fileVariables) throws IOException {
		StringBuilder content = new StringBuilder();
		// 确定需要声明的变量
		Set<Variable> neededVariables = findNeededVariables(traits, fileVariables);
		// 写入变量声明
		for(Variable var : neededVariables) {
			content.append("@").append(var.name).append(" = ").append(var.value).append("\n");
		}
		if(!neededVariables.isEmpty()) {
			content.append("\n");
		}
		// 写入特质内容
		for(Trait trait : traits) {
			content.append(trait.content).append("\n\n");
		}
		Files.writeString(outputFile, content.toString());
		System.out.println("生成文件: " + outputFile.getFileName());
	}
	private static Set<Variable> findNeededVariables(List<Trait> traits, List<Variable> fileVariables) {
		Set<Variable> needed = new LinkedHashSet<>();
		// 构建所有特质内容的字符串
		StringBuilder allContent = new StringBuilder();
		for(Trait trait : traits) {
			allContent.append(trait.content).append("\n");
		}
		String combinedContent = allContent.toString();
		// 检查每个变量是否在特质内容中被使用
		for(Variable var : fileVariables) {
			if(combinedContent.contains("@" + var.name)) {
				needed.add(var);
			}
		}
		return needed;
	}
	// 内部类：表示变量
	static class Variable {
		String name;
		String value;
		Variable(String name, String value) {
			this.name = name;
			this.value = value;
		}
		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;
			Variable variable = (Variable)o;
			return Objects.equals(name, variable.name);
		}
		@Override
		public int hashCode() {
			return Objects.hash(name);
		}
	}
	// 内部类：表示特质
	static class Trait {
		String name;
		String content;
		Trait(String name, String content) {
			this.name = name;
			this.content = content;
		}
	}
}