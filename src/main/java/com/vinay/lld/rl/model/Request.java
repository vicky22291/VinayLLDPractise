package com.vinay.lld.rl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class Request {
    private final Map<String, String> headers;
    private final String body;
}
