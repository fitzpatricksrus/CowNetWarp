package com.lithium3141.shellparser;

import com.lithium3141.shellparser.states.StartState;

import java.util.ArrayList;
import java.util.List;

public class ShellParser {
    public static List<String> parseString(String string) throws ParseException {
        return (new StartState()).parse(string, "", new ArrayList<String>(), null);
    }

    public static List<String> safeParseString(String string) {
        try {
            return ShellParser.parseString(string);
        } catch (ParseException e) {
            return null;
        }
    }
}
