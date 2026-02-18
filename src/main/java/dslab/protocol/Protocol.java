package dslab.protocol;

import java.util.Optional;

/**
 * Interface for processing protocol commands.
 * Implementations of this interface are responsible for processing
 * commands and generating an appropriate response.
 *
 * <p>The {@link #processCommand(String[], Object...)} method is used to process
 * a command passed as a String array. Optional additional parameters can also
 * be provided.</p>
 */
public interface Protocol {
    /**
     * Processes a command and returns a response.
     *
     * @param command An array of strings containing the command and its arguments.
     * @param additionalParams Optional additional parameters that may be used
     *                        during command processing.
     * @return An Optional containing the result of the command processing, or an empty Optional if no result.
     */
    Optional<String> processCommand(String[] command, Object... additionalParams);
}
