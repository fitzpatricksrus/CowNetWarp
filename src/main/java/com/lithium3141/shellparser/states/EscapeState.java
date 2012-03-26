package com.lithium3141.shellparser.states;

import com.lithium3141.shellparser.ParseException;

import java.util.List;

public class EscapeState extends State {

    @Override
    public List<String> parse(String parsing, String accumulator, List<String> parsed, State referrer) throws ParseException {
        if (parsing.length() == 0) {
            throw new ParseException("Unexpected end of string after escape character");
        }

        return referrer.parse(parsing.substring(1), accumulator + (char) (parsing.getBytes()[0]), parsed, this);
    }

}
