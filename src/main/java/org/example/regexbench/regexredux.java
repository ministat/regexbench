package org.example.regexbench;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.*;

public class regexredux {
    @Option(name="-s", aliases="--strFile", required=true, usage="Specify the input string file, every input string is in every single line.")
    private String inputStringFile;

    @Option(name="-p", aliases="--patFile", required=true, usage="Specify the pattern string file, each pattern is in every single line.")
    private String inputPatternFile;

    @Option(name="-r", aliases="--re2j", usage="Only run regex through Re2j")
    private boolean onlyRe2j = false;

    @Option(name="-j", aliases="--jdk", usage="Only run regex through JDK")
    private boolean onlyJDK = false;

    @Option(name="-b", aliases="--runbench", usage="Evaluate replacement bench")
    private boolean onlyReplacement = false;

    @Option(name="-e", aliases="--search", usage="Evaluate search bench")
    private boolean onlySearch = false;

    @Option(name="-i", aliases="--iteration", usage="Iterations, default is 999999")
    private long iteration = 999999;

    @Option(name="-c", aliases="--check", usage="Validate result")
    private boolean check = false;

    @Option(name="-l", aliases="--precompile", usage="Precompile the pattern")
    private boolean precompile = false;

    @Option(name="-S", aliases="--hyperscanOnly", usage="Only run hyperscan")
    private boolean hyperscanOnly = false;

    @Option(name="-u", aliases="--simulateSQL", usage="Compare bench for simulating SQL")
    private boolean simulateSQL = false;

    private boolean parseArgs(final String[] args) {
        final CmdLineParser parser = new CmdLineParser(this);
        if (args.length < 1) {
            parser.printUsage(System.out);
            System.exit(-1);
        }
        boolean ret = true;
        try {
            parser.parseArgument(args);
        } catch (CmdLineException clEx) {
            System.out.println("Error: failed to parse command-line opts: " + clEx);
            ret = false;
        }
        return ret;
    }


    private String replaceBench(String pattern, String data, IReplaceEngine engine) {
        CompletableFuture<String> replacements = CompletableFuture.supplyAsync(() -> {
            final Map<String, String> iub = new LinkedHashMap<>();
            iub.put("tHa[Nt]", "<4>");
            iub.put("aND|caN|Ha[DS]|WaS", "<3>");
            iub.put("a[NSt]|BY", "<2>");
            iub.put("<[^>]*>", "|");
            iub.put("\\|[^|][^|]*\\|", "-");

            String buffer = data;
            for (Map.Entry<String, String> entry : iub.entrySet()) {
                buffer = engine.replace(entry.getKey(), buffer, entry.getValue());
            }
            return buffer;
        });
        return replacements.join();
    }

    public static String readInputLines(String inputFileLines) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(inputFileLines);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static String[] readInputMultiLines(String inputFileLines) {
        List<String> strList = new ArrayList<String>();
        try {
            FileInputStream fis = new FileInputStream(inputFileLines);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                strList.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (strList.size() > 0) {
            String[] res = new String[strList.size()];
            strList.toArray(res);
            return res;
        } else {
            return null;
        }
    }

    public void TestPreCompiledSearchBench(String[] pattern, String sequence, IReplaceEngine engine, long iter) {
        BiFunction<Pattern, String, Map.Entry<String, Long>> counts = (v, s) -> {
            Long count = v.splitAsStream(s).count() - 1; //Off by one
            return new AbstractMap.SimpleEntry<>(v.toString(), count);
        };
        final List<String> variants = Arrays.asList(pattern);
        List<Pattern> compiledPatterns = variants.parallelStream()
                .map(variant -> Pattern.compile(variant))
                .collect(toList());
        Map<String, Long> results = null;
        long start = System.currentTimeMillis();
        for (long i = 0; i < iter; i++) {
            results = compiledPatterns.parallelStream()
                    .map(variant -> counts.apply(variant, sequence))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        final Map<String, Long> resultsCopy = results;
        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(engine.name() + ": " + dur);
        variants.forEach(variant -> System.out.println(variant + " " + resultsCopy.get(variant)));
    }

    public void VerifyHsSearchBench(String[] pattern, String sequence) {
        BiFunction<HyperscanEngine, String, Map.Entry<String, Long>> counts = (v, s) -> {
            Long count = v.scan(s); //Off by one
            System.out.println("=====Pattern: " + v.getPattern());
            v.dumpAllMatched(s);
            return new AbstractMap.SimpleEntry<>(v.getPattern(), count);
        };
        List<String> variants = Arrays.asList(pattern);
        List<HyperscanEngine> compiledPatterns = variants.parallelStream()
                .map(v -> new HyperscanEngine(v))
                .collect(toList());
        Map<String, Long> results = compiledPatterns.stream()
                .map(variant -> counts.apply(variant, sequence))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Map<String, Long> resultsCopy = results;
        variants.forEach(variant -> System.out.println(variant + " " + resultsCopy.get(variant)));
    }

    public void TestHsSearchBench(String[] pattern, String sequence, long iter) {
        BiFunction<HyperscanEngine, String, Map.Entry<String, Long>> counts = (v, s) -> {
            Long count = v.scan(s); //Off by one
            return new AbstractMap.SimpleEntry<>(v.getPattern(), count);
        };
        final List<String> variants = Arrays.asList(pattern);
        List<HyperscanEngine> compiledPatterns = variants.parallelStream()
                .map(variant -> new HyperscanEngine(variant))
                .collect(toList());
        Map<String, Long> results = null;
        long start = System.currentTimeMillis();
        for (long i = 0; i < iter; i++) {
            results = compiledPatterns.parallelStream()
                    .map(variant -> counts.apply(variant, sequence))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        final Map<String, Long> resultsCopy = results;
        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(HyperscanEngine.class.getName() + ": " + dur);
        variants.forEach(variant -> System.out.println(variant + " " + resultsCopy.get(variant)));
    }

    public void SimulateSQLHsBench(String[] pattern, String [] sequences, long iter) {
        BiFunction<HyperscanEngine, String, Map.Entry<String, Long>> counts = (v, s) -> {
            Long count = v.scan(s); //Off by one
            return new AbstractMap.SimpleEntry<>(v.getPattern(), count);
        };
        final List<String> variants = Arrays.asList(pattern);
        List<HyperscanEngine> compiledPatterns = variants.parallelStream()
                .map(variant -> new HyperscanEngine(variant))
                .collect(toList());
        Map<String, Long> results = null;
        long start = System.currentTimeMillis();
        for (long i = 0; i < iter; i++) {
            for (String sequence : sequences) {
                results = compiledPatterns.parallelStream()
                        .map(variant -> counts.apply(variant, sequence))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        final Map<String, Long> resultsCopy = results;
        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(HyperscanEngine.class.getName() + ": " + dur);
        variants.forEach(variant -> System.out.println(variant + " " + resultsCopy.get(variant)));
    }

    public void TestSearchBench(String[] pattern, String sequence, IReplaceEngine engine, long iter) {
        BiFunction<String, String, Map.Entry<String, Long>> counts = (v, s) -> {
            Long count = Pattern.compile(v).splitAsStream(s).count() - 1; //Off by one
            return new AbstractMap.SimpleEntry<>(v, count);
        };
        final List<String> variants = Arrays.asList(pattern);
        Map<String, Long> results = null;
        long start = System.currentTimeMillis();
        for (long i = 0; i < iter; i++) {
             results = variants.parallelStream()
                    .map(variant -> counts.apply(variant, sequence))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        final Map<String, Long> resultsCopy = results;
        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(engine.name() + ": " + dur);
        for (String s : pattern) {
            System.out.println(s + " " + resultsCopy.get(s));
        }
    }

    public void SimulateSQLRegexBench(String[] pattern, String[] sequences, IReplaceEngine engine, long iter) {
        BiFunction<String, String, Map.Entry<String, Long>> counts = (v, s) -> {
            Long count = Pattern.compile(v).splitAsStream(s).count() - 1; //Off by one
            return new AbstractMap.SimpleEntry<>(v, count);
        };
        final List<String> variants = Arrays.asList(pattern);
        Map<String, Long> results = null;
        long start = System.currentTimeMillis();
        for (long i = 0; i < iter; i++) {
            for (String sequence : sequences) {
                results = variants.parallelStream()
                        .map(variant -> counts.apply(variant, sequence))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        final Map<String, Long> resultsCopy = results;
        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(engine.name() + ": " + dur);
        for (String s : pattern) {
            System.out.println(s + " " + resultsCopy.get(s));
        }
    }

    public void TestReplaceBench(String pattern, String data, IReplaceEngine engine, long iter) {
        long start = System.currentTimeMillis();
        for (long i = 0; i < iter; i++) {
            replaceBench(pattern, data, engine);
        }
        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(engine.name() + ": " + dur);
    }

    public void Validation(String pattern, String data, IReplaceEngine engine1, IReplaceEngine engine2) {
        String result1 = replaceBench(pattern, data, engine1);
        String result2 = replaceBench(pattern, data, engine2);
        if ((result1 == null && result2 == null) || (result1.equals(result2))) {
            System.out.println("Equal");
        } else {
            System.out.println(engine1.name() + " Not equal to " + engine2.name());
        }
    }

    public static void main(String[] args) {
        regexredux regex = new regexredux();
        if (!regex.parseArgs(args)) {
            return;
        }

        if (regex.inputPatternFile == null && regex.inputStringFile == null) {
            System.out.println("No pattern file and data file");
            return;
        }
        String pat = readInputLines(regex.inputPatternFile);
        String data = readInputLines(regex.inputStringFile);
        String[] patterns = readInputMultiLines(regex.inputPatternFile);
        String[] sequences = readInputMultiLines(regex.inputStringFile);
        if (regex.onlyReplacement) {
            JDKReplaceEngine jdk = new JDKReplaceEngine();
            Re2ReplaceEngine re2 = new Re2ReplaceEngine();
            regex.TestReplaceBench(pat, data, jdk, regex.iteration);
            regex.TestReplaceBench(pat, data, re2, regex.iteration);
        }
        if (regex.check) {
            JDKReplaceEngine jdk = new JDKReplaceEngine();
            Re2ReplaceEngine re2 = new Re2ReplaceEngine();
            regex.Validation(pat, data, jdk, re2);
        }
        if (regex.onlySearch) {
            JDKReplaceEngine jdk = new JDKReplaceEngine();
            Re2ReplaceEngine re2 = new Re2ReplaceEngine();
            regex.TestSearchBench(patterns, data, jdk, regex.iteration);
            regex.TestSearchBench(patterns, data, re2, regex.iteration);
        }
        if (regex.precompile) {
            JDKReplaceEngine jdk = new JDKReplaceEngine();
            Re2ReplaceEngine re2 = new Re2ReplaceEngine();
            regex.TestPreCompiledSearchBench(patterns, data, jdk, regex.iteration);
            regex.TestPreCompiledSearchBench(patterns, data, re2, regex.iteration);
            regex.TestHsSearchBench(patterns, data, regex.iteration);
        }
        if (regex.hyperscanOnly) {
            regex.VerifyHsSearchBench(patterns, data);
        }
        if (regex.simulateSQL) {
            JDKReplaceEngine jdk = new JDKReplaceEngine();
            Re2ReplaceEngine re2 = new Re2ReplaceEngine();
            regex.SimulateSQLRegexBench(patterns, sequences, jdk, regex.iteration);
            regex.SimulateSQLRegexBench(patterns, sequences, re2, regex.iteration);
            regex.SimulateSQLHsBench(patterns, sequences, regex.iteration);
        }
    }
}
