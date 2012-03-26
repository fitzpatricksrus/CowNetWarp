package com.lithium3141.shellparser.states;

import com.lithium3141.shellparser.ParseException;

import java.util.List;

public abstract class State {
    public abstract List<String> parse(String parsing, String accumulator, List<String> parsed, State referrer) throws ParseException;
}
