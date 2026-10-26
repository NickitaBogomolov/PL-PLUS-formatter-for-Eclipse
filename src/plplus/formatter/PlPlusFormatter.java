package plplus.formatter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Formatter for PL+ / PL/SQL source code.
 * <p>
 * The formatter keeps the original line structure and only:
 * <ul>
 * <li>re-indents lines according to block nesting (BEGIN/END, IF, LOOP, CASE, parentheses, ...);</li>
 * <li>normalizes whitespace between tokens on a line and removes trailing whitespace;</li>
 * <li>changes the case of keywords;</li>
 * <li>limits the number of consecutive blank lines.</li>
 * </ul>
 * String literals, quoted identifiers, PL+ bracket names ({@code [ATTR]}) and comments are never modified.
 * As a safety net the result is re-tokenized and compared with the source: if the sequence of
 * significant tokens differs, the source is returned unchanged.
 */
public final class PlPlusFormatter {

    public enum KeywordCase {
        UPPER, LOWER, PRESERVE
    }

    public static final class Options {
        public final int indentSize;
        public final KeywordCase keywordCase;
        public final int maxBlankLines;

        public Options(int indentSize, KeywordCase keywordCase, int maxBlankLines) {
            this.indentSize = Math.max(0, Math.min(16, indentSize));
            this.keywordCase = keywordCase != null ? keywordCase : KeywordCase.PRESERVE;
            this.maxBlankLines = Math.max(0, Math.min(10, maxBlankLines));
        }

        public static Options defaults() {
            return new Options(2, KeywordCase.LOWER, 1);
        }
    }

    static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTHID", "BEGIN", "BETWEEN", "BINARY_INTEGER", "BODY",
        "BOOLEAN", "BULK", "BY", "CASE", "CHAR", "CLOSE", "COLLECT", "COMMIT", "CONNECT", "CONSTANT",
        "CONTINUE", "CREATE", "CROSS", "CURSOR", "DATE", "DECLARE", "DEFAULT", "DELETE", "DESC", "DISTINCT",
        "DROP", "EACH", "ELSE", "ELSIF", "END", "EXCEPTION", "EXCEPTION_INIT", "EXECUTE", "EXISTS", "EXIT",
        "FALSE", "FETCH", "FOR", "FORALL", "FROM", "FULL", "FUNCTION", "GOTO", "GROUP", "HAVING", "IF",
        "IMMEDIATE", "IN", "INDEX", "INNER", "INSERT", "INTEGER", "INTERSECT", "INTO", "IS", "JOIN", "LEFT",
        "LIKE", "LIMIT", "LOOP", "MERGE", "MINUS", "NOCOPY", "NOT", "NULL", "NUMBER", "OF", "ON", "OPEN", "OR",
        "ORDER", "OTHERS", "OUT", "OUTER", "PACKAGE", "PLS_INTEGER", "PRAGMA", "PRIOR", "PROCEDURE", "RAISE",
        "RECORD", "REPLACE", "RETURN", "RETURNING", "REVERSE", "RIGHT", "ROLLBACK", "ROWTYPE", "SAVEPOINT",
        "SELECT", "SET", "START", "SUBTYPE", "TABLE", "THEN", "TO", "TRIGGER", "TRUE", "TYPE", "UNION",
        "UNIQUE", "UPDATE", "USING", "VALUES", "VARCHAR2", "WHEN", "WHERE", "WHILE", "WITH"
    ));

    /** SQL clauses that start a continuation line at the statement level instead of one level deeper. */
    private static final Set<String> CLAUSE_KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "INTO", "BULK", "GROUP", "ORDER", "HAVING", "UNION", "INTERSECT", "MINUS",
        "VALUES", "SET", "RETURNING", "CONNECT", "START", "JOIN", "LEFT", "RIGHT", "INNER", "FULL", "CROSS",
        "LIMIT"
    ));

    /** Statements whose SQL clauses (FROM, WHERE, ...) are aligned with the statement start. */
    private static final Set<String> SQL_STATEMENTS = new HashSet<>(Arrays.asList(
        "SELECT", "INSERT", "UPDATE", "DELETE", "MERGE", "WITH"
    ));

    /** Arithmetic operators that are surrounded by spaces when used as binary operators. */
    private static final Set<String> ARITHMETIC_OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "**"
    ));

    /** Operators that are always surrounded by single spaces. */
    private static final Set<String> SPACED_OPERATORS = new HashSet<>(Arrays.asList(
        ":=", "=>", "=", "<>", "!=", "^=", "~=", "<=", ">=", "<", ">", "||"
    ));

    private static final String[] MULTI_CHAR_OPERATORS = {
        ":=", "=>", "<=", ">=", "<>", "!=", "^=", "~=", "||", "**", "..", "::", "<<", ">>"
    };

    private PlPlusFormatter() {
    }

    public static String format(String source) {
        return format(source, Options.defaults());
    }

    public static String format(String source, Options options) {
        if (source == null) {
            return "";
        }
        List<Token> tokens = tokenize(source);
        String formatted = new Layout(options, detectLineSeparator(source)).format(tokens);
        return sameCode(tokens, tokenize(formatted)) ? formatted : source;
    }

    private static String detectLineSeparator(String text) {
        int lf = text.indexOf('\n');
        if (lf > 0 && text.charAt(lf - 1) == '\r') {
            return "\r\n";
        }
        if (lf >= 0) {
            return "\n";
        }
        return text.indexOf('\r') >= 0 ? "\r" : System.lineSeparator();
    }

    // ------------------------------------------------------------------------------------------
    // Lexer
    // ------------------------------------------------------------------------------------------

    enum TokenType {
        WHITESPACE, NEWLINE, LINE_COMMENT, BLOCK_COMMENT, STRING, QUOTED_IDENT, BRACKET_IDENT, NUMBER, WORD,
        OPERATOR
    }

    static final class Token {
        final TokenType type;
        final String text;
        /** Set by the layout for arithmetic operators that follow an operand. */
        boolean binary;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }

        boolean isOp(String op) {
            return type == TokenType.OPERATOR && text.equals(op);
        }

        boolean isComment() {
            return type == TokenType.LINE_COMMENT || type == TokenType.BLOCK_COMMENT;
        }

        boolean isCode() {
            return type != TokenType.WHITESPACE && type != TokenType.NEWLINE && !isComment();
        }

        @Override
        public String toString() {
            return type + "[" + text + "]";
        }
    }

    static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            int start = i;
            TokenType type;
            int end;
            if (c == '\r' || c == '\n') {
                i += (c == '\r' && i + 1 < n && s.charAt(i + 1) == '\n') ? 2 : 1;
                type = TokenType.NEWLINE;
            } else if (isBlank(c)) {
                while (i < n && isBlank(s.charAt(i))) {
                    i++;
                }
                type = TokenType.WHITESPACE;
            } else if (s.startsWith("--", i)) {
                while (i < n && s.charAt(i) != '\r' && s.charAt(i) != '\n') {
                    i++;
                }
                type = TokenType.LINE_COMMENT;
            } else if (s.startsWith("/*", i)) {
                end = s.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                type = TokenType.BLOCK_COMMENT;
            } else if ((end = endOfQQuote(s, i)) > 0) {
                i = end;
                type = TokenType.STRING;
            } else if (c == '\'') {
                i = endOfQuoted(s, i, '\'');
                type = TokenType.STRING;
            } else if (c == '"') {
                i = endOfQuoted(s, i, '"');
                type = TokenType.QUOTED_IDENT;
            } else if (c == '[' && (end = endOfBracket(s, i)) > 0) {
                i = end;
                type = TokenType.BRACKET_IDENT;
            } else if (isDigit(c) || (c == '.' && i + 1 < n && isDigit(s.charAt(i + 1)) && !lastIsOperand(out))) {
                i = endOfNumber(s, i);
                type = TokenType.NUMBER;
            } else if (isWordStart(c)) {
                while (i < n && isWordPart(s.charAt(i))) {
                    i++;
                }
                type = TokenType.WORD;
            } else {
                i += matchOperatorLength(s, i);
                type = TokenType.OPERATOR;
            }
            out.add(new Token(type, s.substring(start, i)));
        }
        return out;
    }

    private static boolean isBlank(char c) {
        return c != '\r' && c != '\n' && Character.isWhitespace(c);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$' || c == '#';
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    private static boolean lastIsOperand(List<Token> tokens) {
        for (int k = tokens.size() - 1; k >= 0; k--) {
            Token t = tokens.get(k);
            if (t.type == TokenType.WHITESPACE) {
                continue;
            }
            switch (t.type) {
                case WORD:
                case NUMBER:
                case STRING:
                case QUOTED_IDENT:
                case BRACKET_IDENT:
                    return true;
                case OPERATOR:
                    return t.text.equals(")");
                default:
                    return false;
            }
        }
        return false;
    }

    /** Returns the index after a quoted literal, handling doubled quotes; unterminated literals run to EOF. */
    private static int endOfQuoted(String s, int start, char quote) {
        int i = start + 1;
        while (i < s.length()) {
            if (s.charAt(i) == quote) {
                if (i + 1 < s.length() && s.charAt(i + 1) == quote) {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return s.length();
    }

    /** Oracle alternative quoting: q'[...]', Q'{...}', nq'!...!' etc. Returns -1 if not a q-quote. */
    private static int endOfQQuote(String s, int start) {
        int i = start;
        if (i < s.length() && (s.charAt(i) == 'n' || s.charAt(i) == 'N')) {
            i++;
        }
        if (i + 2 >= s.length() || (s.charAt(i) != 'q' && s.charAt(i) != 'Q') || s.charAt(i + 1) != '\'') {
            return -1;
        }
        char open = s.charAt(i + 2);
        if (Character.isWhitespace(open)) {
            return -1;
        }
        char close;
        switch (open) {
            case '[': close = ']'; break;
            case '{': close = '}'; break;
            case '(': close = ')'; break;
            case '<': close = '>'; break;
            default: close = open;
        }
        int end = s.indexOf(close + "'", i + 3);
        return end < 0 ? s.length() : end + 2;
    }

    /** PL+ bracketed name such as [ACCOUNT]; must be closed on the same line. */
    private static int endOfBracket(String s, int start) {
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ']') {
                return i + 1;
            }
            if (c == '[' || c == '\r' || c == '\n') {
                return -1;
            }
        }
        return -1;
    }

    private static int endOfNumber(String s, int start) {
        int n = s.length();
        int i = start;
        while (i < n && isDigit(s.charAt(i))) {
            i++;
        }
        if (i < n && s.charAt(i) == '.' && !(i + 1 < n && s.charAt(i + 1) == '.')) {
            i++;
            while (i < n && isDigit(s.charAt(i))) {
                i++;
            }
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            if (j < n && isDigit(s.charAt(j))) {
                i = j;
                while (i < n && isDigit(s.charAt(i))) {
                    i++;
                }
            }
        }
        return i;
    }

    private static int matchOperatorLength(String s, int i) {
        for (String op : MULTI_CHAR_OPERATORS) {
            if (s.startsWith(op, i)) {
                return op.length();
            }
        }
        return 1;
    }

    /** True if both token lists contain the same code and comments (ignoring whitespace and keyword case). */
    static boolean sameCode(List<Token> a, List<Token> b) {
        List<Token> x = significant(a);
        List<Token> y = significant(b);
        if (x.size() != y.size()) {
            return false;
        }
        for (int k = 0; k < x.size(); k++) {
            Token p = x.get(k);
            Token q = y.get(k);
            if (p.type != q.type) {
                return false;
            }
            boolean equal;
            if (p.type == TokenType.WORD) {
                equal = p.text.equalsIgnoreCase(q.text);
            } else if (p.type == TokenType.LINE_COMMENT) {
                equal = p.text.stripTrailing().equals(q.text.stripTrailing());
            } else {
                equal = p.text.equals(q.text);
            }
            if (!equal) {
                return false;
            }
        }
        return true;
    }

    private static List<Token> significant(List<Token> tokens) {
        List<Token> result = new ArrayList<>();
        for (Token t : tokens) {
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                result.add(t);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------------------------------

    private static final class Layout {

        private enum Kind {
            DECL, BLOCK, IF, LOOP, CASE, BRANCH, PAREN
        }

        private static final class Frame {
            final Kind kind;
            /** For CASE/BRANCH: the CASE is an expression (inside a statement), not a CASE statement. */
            final boolean exprCase;
            /** For BLOCK: the EXCEPTION section has started. */
            final boolean inException;

            Frame(Kind kind, boolean exprCase, boolean inException) {
                this.kind = kind;
                this.exprCase = exprCase;
                this.inException = inException;
            }
        }

        private final Options options;
        private final String lineSeparator;
        private final Deque<Frame> stack = new ArrayDeque<>();

        /** A statement has started and its terminating ';' (or a block keyword) has not been seen yet. */
        private boolean inStatement;
        /** PROCEDURE/FUNCTION/PACKAGE header seen; the next IS/AS opens its declaration section. */
        private boolean programPending;
        /** First word of the current statement (upper case), or null. */
        private String statementStart;
        private Token prevCode;

        // per-line state
        private boolean firstToken;
        private int lineDepth;
        private Kind lineTopKind;
        private boolean lineStructural;

        Layout(Options options, String lineSeparator) {
            this.options = options;
            this.lineSeparator = lineSeparator;
        }

        String format(List<Token> tokens) {
            StringBuilder out = new StringBuilder();
            List<Token> line = new ArrayList<>();
            int pendingBlankLines = 0;
            boolean anyLine = false;
            for (int k = 0; k <= tokens.size(); k++) {
                Token t = k < tokens.size() ? tokens.get(k) : null;
                if (t != null && t.type != TokenType.NEWLINE) {
                    line.add(t);
                    continue;
                }
                if (isBlankLine(line)) {
                    pendingBlankLines++;
                } else {
                    if (anyLine) {
                        int blanks = Math.min(pendingBlankLines, options.maxBlankLines);
                        for (int b = 0; b < blanks; b++) {
                            out.append(lineSeparator);
                        }
                    }
                    out.append(formatLine(line)).append(lineSeparator);
                    anyLine = true;
                    pendingBlankLines = 0;
                }
                line.clear();
            }
            return out.toString();
        }

        private static boolean isBlankLine(List<Token> line) {
            for (Token t : line) {
                if (t.type != TokenType.WHITESPACE) {
                    return false;
                }
            }
            return true;
        }

        private String formatLine(List<Token> line) {
            int depthBefore = stack.size();
            boolean continuationBefore = inStatement && !topIs(Kind.PAREN);
            String statementBefore = inStatement ? statementStart : null;

            List<Token> code = new ArrayList<>();
            for (Token t : line) {
                if (t.isCode()) {
                    code.add(t);
                }
            }
            // SQL*Plus "/" terminator on its own line ends the current unit.
            if (code.size() == 1 && code.get(0).isOp("/")) {
                stack.clear();
                inStatement = false;
                programPending = false;
                prevCode = code.get(0);
                return "/";
            }

            StringBuilder text = new StringBuilder();
            Token first = null;
            Token prev = null;
            boolean hadSpace = false;
            firstToken = false;
            for (Token t : line) {
                if (t.type == TokenType.WHITESPACE) {
                    hadSpace = true;
                    continue;
                }
                if (t.type == TokenType.OPERATOR && ARITHMETIC_OPERATORS.contains(t.text)) {
                    t.binary = isOperand(prevCode);
                }
                if (prev != null && needsSpace(prev, t, hadSpace)) {
                    text.append(' ');
                }
                hadSpace = false;
                if (first == null) {
                    first = t;
                    firstToken = t.isCode();
                    lineDepth = -1;
                    lineStructural = false;
                }
                text.append(render(t));
                if (t.isCode()) {
                    process(t);
                    if (firstToken) {
                        markIndent();
                        firstToken = false;
                    }
                }
                prev = t;
            }

            int depth;
            if (first != null && first.isCode()) {
                depth = lineDepth;
                boolean clause = first.type == TokenType.WORD
                        && CLAUSE_KEYWORDS.contains(first.text.toUpperCase(Locale.ROOT))
                        && statementBefore != null && SQL_STATEMENTS.contains(statementBefore);
                if (continuationBefore && !lineStructural && !clause && !first.isOp(")")
                        && lineTopKind != Kind.PAREN) {
                    depth++;
                }
            } else {
                depth = depthBefore + (continuationBefore ? 1 : 0);
            }
            return " ".repeat(depth * options.indentSize) + text;
        }

        private String render(Token t) {
            switch (t.type) {
                case LINE_COMMENT:
                    return t.text.stripTrailing();
                case WORD:
                    if (options.keywordCase == KeywordCase.PRESERVE || isMemberName()) {
                        return t.text;
                    }
                    String upper = t.text.toUpperCase(Locale.ROOT);
                    if (!KEYWORDS.contains(upper)) {
                        return t.text;
                    }
                    return options.keywordCase == KeywordCase.UPPER ? upper : t.text.toLowerCase(Locale.ROOT);
                default:
                    return t.text;
            }
        }

        /** Words after '.', '::' or ':' are member/bind names, not keywords. */
        private boolean isMemberName() {
            return prevCode != null && (prevCode.isOp(".") || prevCode.isOp("::") || prevCode.isOp(":"));
        }

        private static boolean needsSpace(Token prev, Token t, boolean hadSpace) {
            if (t.isComment()) {
                return true;
            }
            if (prev.isComment()) {
                return hadSpace;
            }
            if (t.isOp(",") || t.isOp(";") || t.isOp(")") || prev.isOp("(")) {
                return false;
            }
            if (prev.isOp(",") || prev.isOp(";")) {
                return true;
            }
            if (t.isOp(".") || prev.isOp(".") || t.isOp("%") || prev.isOp("%")) {
                return false;
            }
            if (isSpacedOperator(t) || isSpacedOperator(prev) || t.binary || prev.binary) {
                return true;
            }
            return hadSpace;
        }

        /** True if a token can end an operand, so that a following '+', '-', '*' or '/' is binary. */
        private static boolean isOperand(Token t) {
            if (t == null) {
                return false;
            }
            switch (t.type) {
                case NUMBER:
                case STRING:
                case QUOTED_IDENT:
                case BRACKET_IDENT:
                    return true;
                case WORD:
                    String upper = t.text.toUpperCase(Locale.ROOT);
                    return !KEYWORDS.contains(upper) || upper.equals("NULL") || upper.equals("TRUE")
                            || upper.equals("FALSE");
                case OPERATOR:
                    return t.text.equals(")");
                default:
                    return false;
            }
        }

        private static boolean isSpacedOperator(Token t) {
            return t.type == TokenType.OPERATOR && SPACED_OPERATORS.contains(t.text);
        }

        // ---------------------------------------------------------------- block structure

        private void process(Token t) {
            if (!inStatement) {
                statementStart = t.type == TokenType.WORD ? t.text.toUpperCase(Locale.ROOT) : null;
            }
            if (t.type == TokenType.WORD) {
                processWord(t.text.toUpperCase(Locale.ROOT));
            } else if (t.type == TokenType.OPERATOR) {
                processOperator(t.text);
            } else {
                inStatement = true;
            }
            prevCode = t;
        }

        private void processOperator(String op) {
            switch (op) {
                case "(":
                    push(Kind.PAREN, false, false);
                    inStatement = true;
                    break;
                case ")":
                    if (containsParen()) {
                        while (!stack.isEmpty() && stack.pop().kind != Kind.PAREN) {
                            // drop frames left open inside the parentheses
                        }
                    }
                    inStatement = true;
                    break;
                case ";":
                    while (topIs(Kind.PAREN)) {
                        stack.pop();
                    }
                    inStatement = false;
                    programPending = false;
                    break;
                case ">>":
                    // end of a <<label>>
                    inStatement = false;
                    break;
                default:
                    inStatement = true;
            }
        }

        private void processWord(String word) {
            boolean afterEnd = prevCode != null && prevCode.type == TokenType.WORD
                    && prevCode.text.equalsIgnoreCase("END");
            String prevWord = prevCode != null && prevCode.type == TokenType.WORD
                    ? prevCode.text.toUpperCase(Locale.ROOT) : "";
            Frame top = stack.peek();
            switch (word) {
                case "DECLARE":
                    structural();
                    push(Kind.DECL, false, false);
                    programPending = false;
                    inStatement = false;
                    break;
                case "PROCEDURE":
                case "FUNCTION":
                case "PACKAGE":
                    programPending = true;
                    inStatement = true;
                    break;
                case "BODY":
                    if (prevWord.equals("TYPE")) {
                        programPending = true;
                    }
                    inStatement = true;
                    break;
                case "IS":
                case "AS":
                    if (programPending && !topIs(Kind.PAREN)) {
                        structural();
                        programPending = false;
                        push(Kind.DECL, false, false);
                        inStatement = false;
                    } else {
                        inStatement = true;
                    }
                    break;
                case "BEGIN":
                    structural();
                    programPending = false;
                    if (topIs(Kind.DECL)) {
                        stack.pop();
                    }
                    push(Kind.BLOCK, false, false);
                    inStatement = false;
                    break;
                case "EXCEPTION":
                    if (!inStatement && top != null && top.kind == Kind.BLOCK && !top.inException) {
                        structural();
                        stack.pop();
                        push(Kind.BLOCK, false, true);
                        inStatement = false;
                    } else {
                        inStatement = true;
                    }
                    break;
                case "IF":
                    if (!afterEnd) {
                        push(Kind.IF, false, false);
                    }
                    inStatement = true;
                    break;
                case "ELSIF":
                    if (topIs(Kind.IF)) {
                        structural();
                        reopenTop();
                    }
                    inStatement = true;
                    break;
                case "THEN":
                    if (topIs(Kind.IF) || topIs(Kind.BRANCH)) {
                        structural();
                        reopenTop();
                    }
                    inStatement = false;
                    break;
                case "ELSE":
                    if (topIs(Kind.IF) || (topIs(Kind.BRANCH) && (top.exprCase || !inStatement))) {
                        structural();
                        reopenTop();
                        inStatement = false;
                    } else {
                        inStatement = true;
                    }
                    break;
                case "CASE":
                    if (!afterEnd) {
                        push(Kind.CASE, inStatement, false);
                    }
                    inStatement = true;
                    break;
                case "WHEN":
                    if (!prevWord.equals("EXIT") && !prevWord.equals("CONTINUE")) {
                        if (topIs(Kind.CASE)) {
                            structural();
                            push(Kind.BRANCH, top.exprCase, false);
                        } else if (topIs(Kind.BRANCH) && (top.exprCase || !inStatement)) {
                            structural();
                            reopenTop();
                        } else if (topIs(Kind.BLOCK) && top.inException && !inStatement) {
                            structural();
                            push(Kind.BRANCH, false, false);
                        }
                    }
                    inStatement = true;
                    break;
                case "LOOP":
                    if (!afterEnd) {
                        structural();
                        push(Kind.LOOP, false, false);
                        inStatement = false;
                    } else {
                        inStatement = true;
                    }
                    break;
                case "END":
                    structural();
                    programPending = false;
                    if (topIs(Kind.BRANCH)) {
                        stack.pop();
                    }
                    if (!stack.isEmpty() && !topIs(Kind.PAREN)) {
                        stack.pop();
                    }
                    inStatement = true;
                    break;
                default:
                    inStatement = true;
            }
        }

        private void structural() {
            if (firstToken) {
                lineStructural = true;
            }
        }

        /** Pops the top frame and pushes it back, so that the current line is printed one level out. */
        private void reopenTop() {
            Frame f = stack.pop();
            push(f.kind, f.exprCase, f.inException);
        }

        private void push(Kind kind, boolean exprCase, boolean inException) {
            markIndent();
            stack.push(new Frame(kind, exprCase, inException));
        }

        /** Remembers the indentation of the line: depth after the first token's pops, before its pushes. */
        private void markIndent() {
            if (firstToken && lineDepth < 0) {
                lineDepth = stack.size();
                lineTopKind = stack.isEmpty() ? null : stack.peek().kind;
            }
        }

        private boolean topIs(Kind kind) {
            return !stack.isEmpty() && stack.peek().kind == kind;
        }

        private boolean containsParen() {
            for (Frame f : stack) {
                if (f.kind == Kind.PAREN) {
                    return true;
                }
            }
            return false;
        }
    }
}
