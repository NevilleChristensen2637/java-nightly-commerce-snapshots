package education.storefront.infrai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JsonEnvelope {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern FOUND = Pattern.compile("\\\"found\\\"\\s*:\\s*(true|false)");
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MESSAGE = Pattern.compile("\\\"(?:hint|message)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");

    private final String json;
    private final boolean ok;
    private final int status;

    private JsonEnvelope(String json, boolean ok, int status) {
        this.json = json;
        this.ok = ok;
        this.status = status;
    }

    static JsonEnvelope decode(String json, int status) {
        Matcher ok = OK.matcher(json);
        if (!ok.find()) {
            throw new InfraiException("INVALID_ENVELOPE", "Response did not contain an ok field", status);
        }
        return new JsonEnvelope(json, Boolean.parseBoolean(ok.group(1)), status);
    }

    void requireSuccess() {
        if (!ok) {
            throw new InfraiException(match(CODE, json, "REQUEST_REJECTED"),
                    match(MESSAGE, json, "Request was rejected"), status);
        }
        if (status >= 500) {
            throw new InfraiException("TRANSPORT_FAILURE", "Remote service returned HTTP " + status, status);
        }
    }

    boolean found() {
        Matcher matcher = FOUND.matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String match(Pattern pattern, String text, String fallback) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : fallback;
    }
}
