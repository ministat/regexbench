package org.example.regexbench;

import com.gliwka.hyperscan.jna.HyperscanLibrary;
import com.gliwka.hyperscan.jna.HyperscanLibraryDirect;
import com.gliwka.hyperscan.wrapper.*;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LongestMatchScanner extends Scanner {
    protected Map<Long, long[]> _matched = new ConcurrentHashMap<Long, long[]>();

    private HyperscanLibrary.match_event_handler longestMatchHandler = new HyperscanLibrary.match_event_handler() {
        public int invoke(int id, long from, long to, int flags, Pointer context) {
            long[] tuple = new long[]{(long)id, from, to};
            LongestMatchScanner.this._matched.put(from, tuple);
            return 0;
        }
    };

    public List<Match> longestScan(final Database db, final String input) {
        Pointer dbPointer = db.getPointer();
        final byte[] utf8bytes = input.getBytes(StandardCharsets.UTF_8);
        final int bytesLength = utf8bytes.length;
        _matched.clear();
        int hsError = HyperscanLibraryDirect.hs_scan(dbPointer, input, bytesLength,
                0, scratch, longestMatchHandler, Pointer.NULL);
        if(hsError != 0)
            throw Util.hsErrorIntToException(hsError);

        final int[] byteToIndex = Util.utf8ByteIndexesMapping(input, bytesLength);
        final LinkedList<Match> matches = new LinkedList<Match>();
        _matched.forEach((k, v) -> {
            int id = (int)v[0];
            long from = v[1];
            long to = v[2] < 1 ? 1 : v[2]; //prevent index out of bound exception later
            String match = "";
            Expression matchingExpression = db.getExpression(id);

            if(matchingExpression.getFlags().contains(ExpressionFlag.SOM_LEFTMOST)) {
                int startIndex = byteToIndex[(int)from];
                int endIndex = byteToIndex[(int)to - 1];
                match = input.substring(startIndex, endIndex + 1);
            }

            matches.add(new Match(byteToIndex[(int)from], byteToIndex[(int)to - 1], match, matchingExpression));
        });
        return matches;
    }
}
