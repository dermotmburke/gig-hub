package com.d3bot.events.fetchers;

import java.io.IOException;

public interface EventFetcher {
    String fetch() throws IOException, InterruptedException;
}
