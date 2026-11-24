package com.d3bot.events.fetchers;

import com.d3bot.events.utilities.FlareSolverrClient;
import com.d3bot.events.utilities.UrlFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Fetches the Banquet Records events page. Banquet sits behind a Cloudflare browser check,
 * so when {@code fetchers.flaresolverr.url} is set the page is fetched through FlareSolverr;
 * otherwise it falls back to a plain HTTP GET.
 */
@Component
public class BanquetEventFetcher implements EventFetcher {

    private final UrlFetcher urlFetcher;
    private final Optional<FlareSolverrClient> flareSolverr;
    private final String url;

    public BanquetEventFetcher(
            UrlFetcher urlFetcher,
            Optional<FlareSolverrClient> flareSolverr,
            @Value("${fetchers.banquet.url:https://www.banquetrecords.com/events?w=1000}") String url) {
        this.urlFetcher = urlFetcher;
        this.flareSolverr = flareSolverr;
        this.url = url;
    }

    @Override
    public String fetch() throws IOException, InterruptedException {
        if (flareSolverr.isPresent()) {
            return flareSolverr.get().fetch(url);
        }
        return urlFetcher.fetch(url);
    }
}
