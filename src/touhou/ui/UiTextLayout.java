package touhou.ui;

import java.util.Vector;

/** Text layout rules that do not depend on MIDP rendering classes. */
public final class UiTextLayout {
    public interface Measurer {
        int charWidth(char c);

        int stringWidth(String text);
    }

    private UiTextLayout() {
    }

    public static String[] wrapCharacters(String text, int maxWidth, Measurer measurer) {
        requireMeasurer(measurer);
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return new String[] { "" };
        }

        Vector lines = new Vector();
        int lineStart = 0;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = measurer.charWidth(text.charAt(i));
            if (width + charWidth > maxWidth && i > lineStart) {
                lines.addElement(text.substring(lineStart, i));
                lineStart = i;
                width = 0;
            }
            width += charWidth;
        }
        if (lineStart < text.length()) {
            lines.addElement(text.substring(lineStart));
        }
        return toArray(lines);
    }

    public static String[] wrapWords(String text, int maxWidth, Measurer measurer) {
        requireMeasurer(measurer);
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return new String[] { "" };
        }

        Vector lines = new Vector();
        StringBuffer line = new StringBuffer();
        int lineWidth = 0;
        int i = 0;
        while (i < text.length()) {
            int end = i;
            boolean whitespace = isWhitespace(text.charAt(i));
            while (end < text.length() && isWhitespace(text.charAt(end)) == whitespace) {
                end++;
            }

            String token = text.substring(i, end);
            if (whitespace) {
                if (lineWidth == 0) {
                    i = end;
                    continue;
                }
                int tokenWidth = measurer.stringWidth(token);
                if (lineWidth + tokenWidth > maxWidth) {
                    addLine(lines, line);
                    lineWidth = 0;
                } else {
                    line.append(token);
                    lineWidth += tokenWidth;
                }
                i = end;
                continue;
            }

            int tokenWidth = measurer.stringWidth(token);
            if (lineWidth > 0 && lineWidth + tokenWidth > maxWidth) {
                addLine(lines, line);
                lineWidth = 0;
            }
            if (tokenWidth <= maxWidth) {
                line.append(token);
                lineWidth += tokenWidth;
                i = end;
                continue;
            }

            for (int j = 0; j < token.length(); j++) {
                char c = token.charAt(j);
                int charWidth = measurer.charWidth(c);
                if (lineWidth > 0 && lineWidth + charWidth > maxWidth) {
                    addLine(lines, line);
                    lineWidth = 0;
                }
                line.append(c);
                lineWidth += charWidth;
            }
            i = end;
        }

        if (line.length() > 0) {
            addLine(lines, line);
        }
        if (lines.size() == 0) {
            return new String[] { "" };
        }
        return toArray(lines);
    }

    public static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuffer out = new StringBuffer(count);
        for (int i = 0; i < count; i++) {
            out.append(c);
        }
        return out.toString();
    }

    public static int pingPongOffset(int tick, int range, int pause, int tickDivisor) {
        if (range <= 0) {
            return 0;
        }
        if (pause < 0) {
            pause = 0;
        }
        if (tickDivisor <= 0) {
            tickDivisor = 1;
        }
        int step = tick >= 0 ? tick / tickDivisor : 0;
        int period = (pause + range) * 2;
        int position = step % period;
        if (position < pause) {
            return 0;
        }
        if (position < pause + range) {
            return position - pause;
        }
        if (position < pause + range + pause) {
            return range;
        }
        return range - (position - pause - range - pause);
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }

    private static void addLine(Vector lines, StringBuffer line) {
        lines.addElement(line.toString());
        line.setLength(0);
    }

    private static String[] toArray(Vector lines) {
        String[] out = new String[lines.size()];
        lines.copyInto(out);
        return out;
    }

    private static void requireMeasurer(Measurer measurer) {
        if (measurer == null) {
            throw new IllegalArgumentException("measurer");
        }
    }
}
