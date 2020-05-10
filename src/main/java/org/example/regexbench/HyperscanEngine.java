package org.example.regexbench;

import com.gliwka.hyperscan.wrapper.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HyperscanEngine implements IReplaceEngine {

    private LongestMatchScanner _scanner;
    private com.gliwka.hyperscan.wrapper.Expression _expression;
    private Database _db;
    private String _pattern;

    public String getPattern() {
        return _pattern;
    }
    public HyperscanEngine(String pattern) {
        _pattern = pattern;
        try
        {
            compile(pattern);
        }
        catch (CompileErrorException e) {
            System.out.println("error " + e.getMessage());
        }
    }

    public void compile(String pattern) throws CompileErrorException {
        _expression = new Expression(pattern, EnumSet.of(ExpressionFlag.SOM_LEFTMOST, ExpressionFlag.MULTILINE));
        _db = Database.compile(_expression);
        _scanner = new LongestMatchScanner();
        _scanner.allocScratch(_db);
    }

    @Override
    public String replace(String pattern, String data, String rep) {
        return null;
    }

    @Override
    public boolean match(String pattern, String data) {
        //compile(pattern);
        List<Match> matches = _scanner.longestScan(_db, data);
        if (!matches.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public long scan(String data) {
        List<Match> matches = _scanner.longestScan(_db, data);
        return matches.size();
    }

    public void dumpAllMatched(String data) {
        List<Match> matches  = _scanner.longestScan(_db, data);
        StringBuilder sb = new StringBuilder();
        for (Match m : matches) {
            sb.append(" ")
                    .append(m.getStartPosition())
                    .append(":")
                    .append(m.getEndPosition())
            .append("->").append(m.getMatchedString());
        }
        System.out.println(sb.toString());
    }

    @Override
    public String name() {
        return this.getClass().getName();
    }
}
