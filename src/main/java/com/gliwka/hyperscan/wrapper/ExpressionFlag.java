package com.gliwka.hyperscan.wrapper;

/**
 * Flags influencing the matching behaviour of the scanner for a particular Expression
 */
public enum ExpressionFlag implements BitFlag {

    /**
     * Expression will be compiled with no flags.
     */
    NO_FLAG(0),


    /**
     * Matching will be performed case-insensitively.
     */
    CASELESS(1),

    /**
     * Matching a . will not exclude newlines.
     */
    DOTALL(2),


    /**
     * ^ and $ anchors match any newlines in data.
     */
    MULTILINE(4),

    /**
     * Only one match will be generated by the expression per stream.
     */
    SINGLEMATCH(8),

    /**
     * Allow expressions which can match against an empty string, such as .*.
     */
    ALLOWEMPTY(16),

    /**
     * Treat this pattern as a sequence of UTF-8 characters.
     */
    UTF8(32),

    /**
     * Use Unicode properties for character classes.
     */
    UCP(64),

    /**
     * compile pattern in prefiltering mode.
     */
    PREFILTER(128),

    /**
     * Report the leftmost start of match offset when a match is found.
     */
    SOM_LEFTMOST(256),

    /**
     * Parse the expression in logical combination syntax.
     */
    COMBINATION(512),

    /**
     * Ignore match reporting for this expression. Used for the sub-expressions in logical combinations.
     */
    QUIET(1024);

    private final int bits;

    ExpressionFlag(int bitPosition) {
        this.bits = bitPosition;
    }


    /**
     * Get the significant bits for the flag
     * @return int containing the significant bit for the flag
     */
    public int getBits() {
        return bits;
    }
}
