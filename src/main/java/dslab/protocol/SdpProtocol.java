package dslab.protocol;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class SdpProtocol implements Protocol {
    private final ConcurrentHashMap<String, String> dnsEntries;

    @Override
    public Optional<String> processCommand(String[] command, Object... additionalParams) {
        String returner = switch (command[0]) {
            case "register" -> handleRegisterCommand(command);
            case "unregister" -> handleUnregisterCommand(command);
            case "resolve" -> handleResolveCommand(command);
            case "exit" -> handleExitCommand();
            default -> "error usage: <command> <args>";
        };
        return Optional.of(returner);
    }

    private String handleRegisterCommand(String[] parts) {
        if (parts.length != 3) return "error usage: register <name> <ip:port>";

        dnsEntries.put(parts[1], parts[2]);
        return "ok";
    }

    private String handleResolveCommand(String[] parts) {
        if (parts.length != 2) return "error usage: resolve <name>";

        String result = dnsEntries.get(parts[1]);
        return result != null ? result : "error domain not found";
    }

    private String handleUnregisterCommand(String[] parts) {
        if (parts.length != 2) return "error usage: unregister <name>";

        dnsEntries.remove(parts[1]);
        return "ok";
    }

    private String handleExitCommand() {
        return "ok bye";
    }


}
