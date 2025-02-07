package testUtils;


import java.util.Map;

public record AuthInfo(String username,
                       Map<String, String> serverResponse) {
}
