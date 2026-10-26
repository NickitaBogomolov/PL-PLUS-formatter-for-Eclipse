package plplus.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PlPlusFormatter {

    private static final List<String> KEYWORDS = Arrays.asList(
        "BEGIN", "END", "DECLARE", "IF", "THEN", "ELSE", "ELSIF", "LOOP",
        "WHILE", "FOR", "IN", "OUT", "INOUT", "RETURN", "CASE", "WHEN",
        "AS", "IS", "CREATE", "OR", "REPLACE", "PROCEDURE", "FUNCTION",
        "PACKAGE", "BODY", "TYPE", "EXCEPTION", "RAISE", "NULL", "SELECT",
        "FROM", "WHERE", "AND", "OR", "NOT", "ORDER", "BY", "GROUP", "HAVING",
        "INSERT", "UPDATE", "DELETE", "INTO", "VALUES", "SET", "JOIN", "LEFT",
        "RIGHT", "INNER", "OUTER", "ON"
    );

    private static final List<String> BLOCK_KEYWORDS = Arrays.asList(
        "BEGIN", "DECLARE", "LOOP", "CASE", "IF", "ELSE", "ELSIF", "WHEN"
    );

    public static String format(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\\r?\\n");
        int indent = 0;
        boolean inSingleLineComment = false;

        for (String rawLine : lines) {
            String line = rawLine.replace("\t", "    ");
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                    result.append(System.lineSeparator());
                }
                continue;
            }

            if (trimmed.startsWith("--")) {
                appendIndented(result, indent, trimmed);
                result.append(System.lineSeparator());
                continue;
            }

            if (trimmed.startsWith("/*")) {
                appendIndented(result, indent, trimmed);
                result.append(System.lineSeparator());
                inSingleLineComment = true;
                continue;
            }

            if (inSingleLineComment && trimmed.endsWith("*/")) {
                inSingleLineComment = false;
            }

            String normalized = normalizeOperators(trimmed);
            String prepared = prepareStatement(normalized);

            int delta = computeIndentDelta(prepared, indent);
            appendIndented(result, Math.max(0, indent), prepared);
            result.append(System.lineSeparator());

            indent += delta;
            indent = Math.max(0, indent);
        }

        return result.toString().trim() + System.lineSeparator();
    }

    private static void appendIndented(StringBuilder sb, int indent, String text) {
        sb.append(" ".repeat(indent * 4));
        sb.append(text);
    }

    private static String normalizeOperators(String text) {
        String result = text.replace(";", "; ");
        result = result.replace("(", " ( ");
        result = result.replace(")", " ) ");
        result = result.replace(",", ", ");
        result = result.replace("=", " = ");
        result = result.replace("<>", " <> ");
        result = result.replace("<", " < ");
        result = result.replace(">", " > ");
        result = result.replace("+", " + ");
        result = result.replace("-", " - ");
        result = result.replace("*", " * ");
        result = result.replace("/", " / ");
        return collapseWhitespace(result);
    }

    private static String prepareStatement(String text) {
        String result = text;
        result = result.replace("; ", ";");
        result = result.replace(" ( ", "(");
        result = result.replace(" ) ", ")");
        result = result.replace(" , ", ", ");
        result = result.replace(" = ", " = ");
        result = result.replace(" <> ", "<>");
        result = result.replace(" < ", " < ");
        result = result.replace(" > ", " > ");
        result = result.replace(" + ", " + ");
        result = result.replace(" - ", " - ");
        result = result.replace(" * ", " * ");
        result = result.replace(" / ", " / ");
        return collapseWhitespace(result.trim());
    }

    private static int computeIndentDelta(String statement, int currentIndent) {
        String upper = statement.toUpperCase();
        int delta = 0;

        for (String keyword : BLOCK_KEYWORDS) {
            if (upper.contains(keyword) && !upper.contains("END " + keyword)) {
                delta += 1;
            }
        }

        if (upper.startsWith("END") || upper.startsWith("ELSE") || upper.startsWith("ELSIF")) {
            delta -= 1;
        }

        if (upper.startsWith("WHEN") || upper.startsWith("EXCEPTION")) {
            delta += 1;
        }

        return delta;
    }

    private static String collapseWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
