/*
 * Derives the jpackage --add-modules list from module-info.java so that the
 * packaging scripts never drift from the module descriptor again.
 *
 * Run as a single file source program, which behaves identically on the Linux,
 * macOS and Windows runners:
 *
 *     java packaging/AddModules.java                 print the module list
 *     java packaging/AddModules.java --verify-pom    fail if pom.xml drifted
 *
 * Only platform modules are emitted. Third party requires such as jlayer are
 * skipped because they are supplied as ordinary jars on the class path, and
 * automatic modules cannot be linked into a runtime image at all. Test only
 * requires such as org.junit.jupiter.api are skipped for the same reason.
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddModules {

    private static final Path DESCRIPTOR =
            Path.of("src", "main", "java", "module-info.java");

    private static final Path POM = Path.of("pom.xml");

    /** Matches "requires [transitive] [static] some.module;" in any order. */
    private static final Pattern REQUIRES = Pattern.compile(
            "requires\\s+((?:transitive\\s+|static\\s+)*)([A-Za-z0-9_.]+)\\s*;");

    private static final Pattern ADD_MODULE =
            Pattern.compile("<addmodule>\\s*([A-Za-z0-9_.]+)\\s*</addmodule>");

    private static final Pattern BLOCK_COMMENT =
            Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    private AddModules() {
    }

    public static void main(String[] args) throws IOException {
        boolean verifyPom = args.length > 0 && "--verify-pom".equals(args[0]);

        Set<String> required = platformModules(read(DESCRIPTOR));
        if (required.isEmpty()) {
            fail("No platform modules found in " + DESCRIPTOR);
        }

        if (!verifyPom) {
            System.out.println(String.join(",", required));
            return;
        }

        Set<String> declared = new TreeSet<>();
        Matcher matcher = ADD_MODULE.matcher(read(POM));
        while (matcher.find()) {
            declared.add(matcher.group(1));
        }

        if (declared.equals(required)) {
            System.out.println("pom.xml <addmodules> matches module-info.java ("
                    + required.size() + " modules)");
            return;
        }

        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(declared);
        Set<String> extra = new TreeSet<>(declared);
        extra.removeAll(required);

        System.err.println("pom.xml <addmodules> drifted from module-info.java.");
        if (!missing.isEmpty()) {
            System.err.println("  missing in pom.xml: " + String.join(", ", missing));
        }
        if (!extra.isEmpty()) {
            System.err.println("  not required by module-info.java: "
                    + String.join(", ", extra));
        }
        System.err.println("  expected: " + String.join(",", required));
        System.exit(1);
    }

    /** Returns the platform modules required by the given descriptor, sorted. */
    static Set<String> platformModules(String source) {
        String stripped = LINE_COMMENT.matcher(
                BLOCK_COMMENT.matcher(source).replaceAll(" ")).replaceAll(" ");

        Set<String> modules = new TreeSet<>();
        Matcher matcher = REQUIRES.matcher(stripped);
        while (matcher.find()) {
            // "requires static" is a compile time only dependency and must not
            // be linked into the shipped runtime image.
            if (matcher.group(1).contains("static")) {
                continue;
            }
            String module = matcher.group(2);
            if (isPlatformModule(module)) {
                modules.add(module);
            }
        }
        return modules;
    }

    private static boolean isPlatformModule(String module) {
        return module.startsWith("java.")
                || module.startsWith("jdk.")
                || module.startsWith("javafx.");
    }

    private static String read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            fail("Not found: " + path.toAbsolutePath()
                    + " (run this from the repository root)");
        }
        return Files.readString(path);
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(2);
    }
}
