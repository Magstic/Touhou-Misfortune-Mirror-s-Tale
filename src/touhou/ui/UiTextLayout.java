package touhou.ui;

import java.util.Vector;

/** Text layout rules that do not depend on MIDP rendering classes. */
public final class UiTextLayout {
    private static final String OPENING = "([{（［｛〈《「『【〔〖〘〚“‘";
    private static final String CLOSING = ")]}）］｝〉》」』】〕〗〙〛”’、。，．！？；：,.!?;:%％‰…—–ー々ゝゞヽヾぁぃぅぇぉっゃゅょゎァィゥェォッャュョヮヵヶ";

    public interface Measurer {
        int charWidth(char c);
    }

    private UiTextLayout() {
    }

    public static String[] wrap(String text, int maxWidth, Measurer measurer) {
        requireMeasurer(measurer);
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return new String[] { "" };
        }

        Vector lines = new Vector();
        int lineStart = 0;
        int width = 0;
        int breakEnd = -1;
        int breakNext = 0;
        int breakWidth = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\r' || c == '\n') {
                addLine(lines, text, lineStart, i);
                i++;
                if (c == '\r' && i < text.length() && text.charAt(i) == '\n') i++;
                lineStart = i;
                width = 0;
                breakEnd = -1;
                continue;
            }
            boolean whitespace = isWhitespace(c);
            if (whitespace && i == lineStart) {
                lineStart = ++i;
                continue;
            }
            int charWidth = measurer.charWidth(c);
            if (!whitespace && width + charWidth > maxWidth && i > lineStart) {
                if (breakEnd > lineStart) {
                    addLine(lines, text, lineStart, breakEnd);
                    lineStart = breakNext;
                    // Carry the measured word prefix onto the next line without rescanning it.
                    width -= breakWidth;
                } else {
                    addLine(lines, text, lineStart, i);
                    lineStart = i;
                    width = 0;
                }
                breakEnd = -1;
                if (width + charWidth > maxWidth && i > lineStart) {
                    addLine(lines, text, lineStart, i);
                    lineStart = i;
                    width = 0;
                }
            }
            width += charWidth;
            i++;
            if (whitespace) {
                if (breakEnd > lineStart && breakNext == i - 1) {
                    breakNext = i;
                    breakWidth = width;
                }
            } else if (i < text.length() && canBreak(text, i)) {
                breakEnd = i;
                breakNext = i;
                breakWidth = width;
            }
        }
        addLine(lines, text, lineStart, text.length());
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

    private static boolean canBreak(String text, int index) {
        if (isOpening(text, index - 1)) {
            return false;
        }
        int next = index;
        while (next < text.length() && isWhitespace(text.charAt(next))) next++;
        if (next < text.length() && isClosing(text, next)) return false;
        if (next != index) return true;
        return !isWordPart(text, index - 1) || !isWordPart(text, index);
    }

    private static boolean isOpening(String text, int index) {
        char c = text.charAt(index);
        if (OPENING.indexOf(c) >= 0) return true;
        if (c != '\'' && c != '"') return false;
        return index == 0 || isWhitespace(text.charAt(index - 1))
                || text.charAt(index - 1) == '\r' || text.charAt(index - 1) == '\n'
                || OPENING.indexOf(text.charAt(index - 1)) >= 0;
    }

    private static boolean isClosing(String text, int index) {
        char c = text.charAt(index);
        return CLOSING.indexOf(c) >= 0 || ((c == '\'' || c == '"') && !isOpening(text, index));
    }

    private static boolean isWordPart(String text, int index) {
        char c = text.charAt(index);
        if (isWordChar(c)) return true;
        return (c == '\'' || c == '’') && index > 0 && index + 1 < text.length()
                && isWordChar(text.charAt(index - 1)) && isWordChar(text.charAt(index + 1));
    }

    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || (c >= 'ａ' && c <= 'ｚ') || (c >= 'Ａ' && c <= 'Ｚ') || (c >= '０' && c <= '９');
    }

    private static void addLine(Vector lines, String text, int start, int end) {
        while (end > start && isWhitespace(text.charAt(end - 1))) end--;
        lines.addElement(text.substring(start, end));
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
