package org.example.regexbench;
import com.google.re2j.Pattern;

public class Re2ReplaceEngine implements IReplaceEngine {
    @Override
    public String replace(String pattern, String data, String rep) {
        return Pattern.compile(pattern).matcher(data).replaceAll(rep);
    }

    @Override
    public boolean match(String pattern, String data) {
        return java.util.regex.Pattern.compile(pattern).matcher(data).find();
    }

    @Override
    public String name() {
        return this.getClass().getName();
    }
}
