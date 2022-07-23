package org.symly.repositories;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * A representation of an ignore rule from the .symlyignore file.
 */
@RequiredArgsConstructor
class IgnoreRule {

    private final Pattern pattern;

    public boolean match(String name) {
        return pattern.matcher(name).matches();
    }

    @Override
    public int hashCode() {
        return pattern.pattern().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IgnoreRule ir
            && pattern.pattern().equals(ir.pattern.pattern());
    }

    @Override
    public String toString() {
        return pattern.pattern();
    }

    public static IgnoreRule ofRegex(String regex) {
        return new IgnoreRule(Pattern.compile(regex));
    }
}
