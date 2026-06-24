package org.mwage.game.paradox.stellaris.trait_reloader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// Thank you, DeepSeek
public class ProcessorTraits implements Config {
	public static void main(String[] args) {
		System.out.println("Generator Version 1.2");
		try {
			processTraits();
			System.out.println("特质文件处理完成！");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
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
		List<Variable> fileVariables = extractVariablesFile(content);
		// 提取所有特质
		List<Trait> traits = extractTraits(content);
		// 按特质名分组（去除数字后缀）
		Map<String, List<Trait>> traitGroups = new LinkedHashMap<>();
		for(Trait trait : traits) {
			String baseTraitName = getNameBaseTrait(trait.name);
			traitGroups.computeIfAbsent(baseTraitName, k -> new ArrayList<>()).add(trait);
		}
		// 为每个特质组生成文件
		int traitCount = getCountActualTraits(traits);
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
	/// 因为部分特质是相同的（等级不同），存入同一文件。所以要减去重复的特质数量，以避免文件名中过多的0。
	private static int getCountActualTraits(List<Trait> traits) {
		Set<String> names = new HashSet<>();
		for(Trait trait : traits) {
			names.add(getNameBaseTrait(trait.name));
		}
		return names.size();
	}
	private static List<Variable> extractVariablesFile(String content) {
		content = removeComment(content);
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
	// Rewrite method, because the old one DeepSeek provided is buggy.
	private static List<Trait> extractTraits(String content) {
		List<Trait> traits = new ArrayList<>();
		String[] lines = content.split("\\r?\\n|\\r");
		int levelScope = 0;
		int[] pointerL = {-1, -1}, pointerR = {-1, -1}; // [indexLine, indexCharInLine]
		boolean[][] areaQuoted = new boolean[lines.length][];
		boolean[][] areaCommented = new boolean[lines.length][];
		parseLine: for(int indexLine = 0; indexLine < lines.length; indexLine++) {
			String line = lines[indexLine];
			char[] cs = line.toCharArray();
			int numQuote = 0;
			areaQuoted[indexLine] = new boolean[cs.length];
			areaCommented[indexLine] = new boolean[cs.length];
			boolean commented = false;
			parseChar: for(int indexChar = 0; indexChar < cs.length; indexChar++) {
				char c = cs[indexChar];
				if(numQuote % 2 != 0) { // 在字符串内
					if(c == '"') {
						if(cs[indexChar - 1] != '\\') { // 考虑转义字符
							numQuote++;
						}
					}
					areaQuoted[indexLine][indexChar] = true;
				}
				else { // 在字符串外
					if(c == '#') {
						commented = true;
					}
					if(commented) {
						areaCommented[indexLine][indexChar] = true;
						continue;
					}
					if(c == '"') {
						numQuote++;
						areaQuoted[indexLine][indexChar] = true;
					}
					if(c == '{') {
						if(levelScope == 0) {
							pointerL[0] = indexLine;
							pointerL[1] = indexChar;
						}
						levelScope++;
					}
					if(c == '}') {
						levelScope--;
						if(levelScope == 0) {
							pointerR[0] = indexLine;
							pointerR[1] = indexChar;
							String nameTrait = getNameTrait(lines, pointerL, areaQuoted, areaCommented);
							String contentSub = STR."\{nameTrait} = \{getContentSub(lines, pointerL, pointerR)}";
							traits.add(new Trait(nameTrait, contentSub));
						}
					}
				}
			}
		}
		return traits;
	}
	private static String getContentSub(String[] lines, int[] pointerL, int[] pointerR) {
		StringBuilder sbContent = new StringBuilder();
		for(int indexLine = pointerL[0]; indexLine <= pointerR[0]; indexLine++) {
			String line = lines[indexLine];
			if(indexLine == pointerL[0]) {
				int l = pointerL[1];
				if(indexLine == pointerR[0]) {
					int r = pointerR[1];
					sbContent.append(line, l, r + 1);
				}
				else {
					sbContent.append(line.substring(l));
				}
			}
			else if(indexLine == pointerR[0]) {
				int r = pointerR[1];
				sbContent.append(line, 0, r + 1);
			}
			else {
				sbContent.append(line);
			}
			sbContent.append(System.lineSeparator());
		}
		return sbContent.toString();
	}
	private static String getNameTrait(String[] lines, int[] pointerL, boolean[][] areaQuoted, boolean[][] areaCommented) {
		boolean foundSignEqual = false; // =
		for(int indexLine = pointerL[0]; indexLine >= 0; indexLine--) {
			String line = lines[indexLine];
			char[] cs = line.toCharArray();
			int r = cs.length - 1;
			if(indexLine == pointerL[0]) {
				r = pointerL[1];
			}
			for(int indexChar = r; indexChar >= 0; indexChar--) {
				if(areaQuoted[indexLine][indexChar] || areaCommented[indexLine][indexChar]) {
					continue;
				}
				char c = cs[indexChar];
				if(!foundSignEqual) {
					if(c == '=') {
						foundSignEqual = true;
					}
				}
				else {
					if(Character.isLetterOrDigit(c) || c == '_') { // found identifier!
						String identifier = "";
						for(int index = indexChar; index >= 0; index--) {
							boolean isLetterGoodForIdentifier = Character.isLetterOrDigit(cs[index]) || cs[index] == '_';
							if(isLetterGoodForIdentifier) {
								identifier = STR."\{cs[index]}\{identifier}";
							}
							if(!isLetterGoodForIdentifier || index == 0) {
								return identifier;
							}
						}
					}
				}
			}
		}
		throw new RuntimeException(STR."Identifier did not found, pointerL = {\{pointerL[0]}, \{pointerL[1]}}");
	}
	private static String getNameBaseTrait(String traitName) {
		// 去除数字后缀，如 _2, _3 等
		return traitName.replaceAll("_\\d+$", "");
	}
	private static void writeTraitFile(Path outputFile, List<Trait> traits, List<Variable> fileVariables) throws IOException {
		StringBuilder content = new StringBuilder();
		// 确定需要声明的变量
		Set<Variable> neededVariables = findVariablesNeeded(traits, fileVariables);
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
		Files.writeString(outputFile, UtilString.removeEmptyLines(content.toString()));
		System.out.println("生成文件: " + outputFile.getFileName());
	}
	private static Set<Variable> findVariablesNeeded(List<Trait> traits, List<Variable> fileVariables) {
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
	/// 修复：DeepSeek似乎会将注释语句后的第一个Trait忽略掉
	private static String removeComment(String content) {
		String[] lines = content.split("\\r?\\n|\\r");
		StringBuilder sbContent = new StringBuilder();
		for(String line : lines) {
			String subline = line;
			int indexComment = indexCommentAt(line);
			if(indexComment >= 0) {
				subline = line.substring(0, indexComment);
			}
			sbContent.append(subline).append(System.lineSeparator());
		}
		return sbContent.toString();
	}
	private static int indexCommentAt(String line) {
		char[] cs = line.toCharArray();
		if(cs.length == 0) {
			return -1;
		}
		int length = cs.length;
		int numQuote = 0;
		for(int i = 0; i < length; i++) {
			char c = cs[i];
			if(c == '#') {
				if(numQuote % 2 == 0) {
					return i;
				}
			}
			if(c == '"') {
				numQuote++;
			}
		}
		return -1;
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