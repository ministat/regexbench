package org.example.regexbench;

public interface IReplaceEngine {
    public String replace(String pattern, String data, String rep);
    public boolean match(String pattern, String data);
    public String name();
}
