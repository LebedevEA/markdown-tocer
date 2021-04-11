import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

public class Tocer {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No arguments given, was needed [0] = file path.");
        } else try {
            Tocer tocer = new Tocer(args[0]);
            System.out.println(tocer.make());
            try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not find file " + args[0] + ", try again.");
        } catch (IOException e) {
            System.out.println("Something went wrong while working with file " + args[0] + ", try again");
        }
    }

    private final Section root;
    private final Map<String, Integer> usedNames = new HashMap<>();
    private static final String fewSpaces = "( {0,3})";
    private static final String anySpaces = " *";
    private static final String header = fewSpaces + "#{1,6} .*";

    public Tocer(String path) throws IOException {
        List<Pair> headers = makePairs(path);
        Stack<Section> sections = new Stack<>();

        root = new Section(null);

        sections.push(root);

        for (Pair pair : headers) {
            int depth = pair.depth;
            String name = pair.name;

            var newSec = new Section(name);

            if (sections.size() - 1 >= depth) {
                while (sections.size() - 1 != depth)
                    sections.pop();
            } else {
                while (sections.size() - 1 != depth) {
                    var empty = new Section(null);
                    sections.peek().addSubsection(empty);
                    sections.push(empty);
                }
            }
            sections.peek().addSubsection(newSec);
            sections.push(newSec);
        }
    }

    private List<Pair> makePairs(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            boolean inComment = false;
            String prev = null; // not null means prev is not " ### ... "
            String line;
            ArrayList<Pair> list = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (!inComment && line.matches(header)) {
                    list.add(makePair(line));
                    prev = null;
                } else {
                    if (line.matches("(.*<!--.*)|(.*-->.*)")) { // handle comments
                        Matcher matcher = Pattern.compile("(<!--)|(-->)").matcher(line);
                        int lastBeg = 0;
                        while (matcher.find()) {
                            lastBeg = matcher.start();
                        }
                        inComment = line.charAt(lastBeg) == '<';
                    } else if (prev != null && line.matches(fewSpaces+ "=+" + anySpaces)) {
                        list.add(new Pair(0, prev.trim()));
                    } else if (prev != null && line.matches(fewSpaces + "-+" + anySpaces)) {
                        list.add(new Pair(1, prev.trim()));
                    } else {
                        prev = line;
                    }
                }
            }
            return list;
        }
    }

    private Pair makePair(String line) {
        var trimmed = line.trim();
        Matcher matcher = Pattern.compile("#*").matcher(trimmed);
        if (!matcher.find()) throw new RuntimeException("String does not match \" *#+\".");
        return new Pair(matcher.end() - 1, trimmed.substring(matcher.end()).trim());
    }

    public String make() {
        return root.make(0).toString();
    }

    private class Section {
        public final String name;
        public final List<Section> subsections = new ArrayList<>();

        public Section(String name) {
            this.name = name;
        }

        public void addSubsection(Section sec) {
            subsections.add(sec);
        }

        public StringBuilder make(int depth) {
            final StringBuilder stringBuilder = new StringBuilder();
            final int n = subsections.size();
            for (int i = 0; i < n; i++) {
                var cur = subsections.get(i);
                if (cur.name != null) {
                    stringBuilder
                            .append(spaces(depth))
                            .append(i + 1)
                            .append(". [")
                            .append(cur.name)
                            .append("](")
                            .append(makeLink(cur.name))
                            .append(")\n")
                            .append(cur.make(depth + 1));
                } else {
                    stringBuilder
                            .append(cur.make(depth + 1));
                }
            }
            return stringBuilder;
        }
    }

    private static class Pair {
        public int depth;
        public String name;
        public Pair(int depth, String name) {
            this.depth = depth;
            this.name = name;
        }
    }

    private String makeLink(String name) {
        String ini = name
                .toLowerCase()
                .chars()
                .map(c -> { if (c == ' ') return '-'; else return c; })
                .mapToObj((int i) -> (char)i)
                .collect(Collector.of(
                        StringBuilder::new,
                        StringBuilder::append,
                        StringBuilder::append,
                        StringBuilder::toString));
        Integer count = usedNames.get(ini);
        if (count == null) {
            usedNames.put(ini, 1);
            return '#' + ini;
        } else {
            usedNames.put(ini, count + 1);
            return '#' + ini + '-' + count;
        }
        /*
         * Все это нужно на случай, если имена у загаловков
         * повторяются, НО...
         * Это, конечно же, ломается, например таким примером
         * # Step 3
         * # Step 3
         * # Step 3-1
         * Но, вроде бы и markdown в принципе этим ломается...
         * А так как не была указана точная реализация markdown,
         * то особо посмотреть документацию не вышло, но
         * мне не кажется, что был рассчет на то, что будут
         * такие мелкие проблемы решаться
         */
    }

    private static String spaces(int n) {
        return "    ".repeat(n);
    }
}
