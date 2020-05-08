package org.example.regexbench;

import java.util.regex.Pattern;

public class JDKReplaceEngine implements IReplaceEngine {
    @Override
    public String replace(String pattern, String data, String rep) {
        return Pattern.compile(pattern).matcher(data).replaceAll(rep);
    }

    @Override
    public boolean match(String pattern, String data) {
        return Pattern.compile(pattern).matcher(data).find();
    }

    @Override
    public String name() {
        return this.getClass().getName();
    }
}
