package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatPreferences;

/**
 * Opt-in raw-wire probe matrix against ON4KST category 9.
 *
 * <p>Login traffic is deliberately excluded from the evidence file so that no
 * credentials or login tokens are persisted. Capture begins after the initial
 * category-9 user list has completed.</p>
 */
class On4KstProbeVariantPracticeTest {
    private static final int CATEGORY = ChatCategory.VUHFR3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration QUIET_PERIOD = Duration.ofSeconds(90);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(125);
    private static final Duration VARIANT_TIMEOUT = Duration.ofMinutes(8);
    private static final Duration TOTAL_TIMEOUT = Duration.ofMinutes(45);

    private static final List<ProbeVariant> VARIANTS = List.of(
            variant("CK_CRLF", "CK\r\n"),
            variant("CK_PIPE_CRLF", "CK|\r\n"),
            variant("CK_CR_NUL", "CK\r\0"),
            variant("CK_PIPE_CR_NUL", "CK|\r\0"),
            variant("PIPE_CK_PIPE_CRLF", "|CK|\r\n")
    );

    @Test
    @Timeout(value = 47, unit = TimeUnit.MINUTES)
    void recordsProbeVariantResponsesWithoutLoginData() throws Exception {
        assumeTrue(Boolean.getBoolean("on4kst.live.variants"),
                "Live probe matrix requires -Don4kst.live.variants=true");

        ChatPreferences preferences = new ChatPreferences();
        assumeTrue(preferences.readPreferencesFromXmlFile(),
                "No readable local ON4KST test configuration");
        assumeTrue(hasText(preferences.getStn_loginCallSign())
                        && hasText(preferences.getStn_loginPassword()),
                "Local ON4KST test credentials are incomplete");

        Path evidenceDirectory = Path.of(
                "target", "on4kst-live-evidence");
        Files.createDirectories(evidenceDirectory);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .format(ZonedDateTime.now());
        Path evidenceFile = evidenceDirectory.resolve(
                timestamp + "-category-9-probe-matrix.log");
        List<ProbeResult> results = new ArrayList<>();
        long totalDeadline = System.nanoTime() + TOTAL_TIMEOUT.toNanos();

        try (Writer evidence = Files.newBufferedWriter(
                evidenceFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            writeHeader(evidence);

            for (ProbeVariant variant : VARIANTS) {
                if (System.nanoTime() >= totalDeadline) {
                    ProbeResult result = new ProbeResult(
                            variant.name(), "TOTAL_BUDGET_EXHAUSTED", "");
                    results.add(result);
                    writeResult(evidence, result);
                    continue;
                }

                ProbeResult result;
                try {
                    result = runVariant(
                            preferences, variant, totalDeadline, evidence);
                } catch (Exception exception) {
                    result = new ProbeResult(
                            variant.name(),
                            "ERROR",
                            exception.getClass().getSimpleName()
                                    + ": " + safeMessage(exception));
                    writeResult(evidence, result);
                }
                results.add(result);
                evidence.flush();
            }

            evidence.write("\nSUMMARY\n");
            for (ProbeResult result : results) {
                writeResult(evidence, result);
            }
        }

        System.out.println("[ON4KST variant test] Evidence: "
                + evidenceFile.toAbsolutePath());
        assertTrue(
                results.stream().anyMatch(result ->
                        "OK_DIRECT".equals(result.status())),
                "No probe variant received a direct OK response. Evidence: "
                        + evidenceFile.toAbsolutePath());
    }

    private ProbeResult runVariant(
            ChatPreferences preferences,
            ProbeVariant variant,
            long totalDeadline,
            Writer evidence
    ) throws Exception {
        long variantDeadline = Math.min(
                totalDeadline,
                System.nanoTime() + VARIANT_TIMEOUT.toNanos());
        evidence.write("\nVARIANT " + variant.name() + "\n");
        evidence.write("probe hex=" + HexFormat.ofDelimiter(" ")
                .withUpperCase().formatHex(variant.bytes())
                + " ascii=" + escapedAscii(variant.bytes()) + "\n");

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(
                            preferences.getStn_on4kstServersDns(),
                            preferences.getStn_on4kstServersPort()),
                    Math.toIntExact(CONNECT_TIMEOUT.toMillis()));
            socket.setSoTimeout(1_000);

            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            WireReader reader = new WireReader(input);
            completeHandshake(preferences, reader, output, variantDeadline);

            writeEvent(evidence, "STATE", new byte[0],
                    "category 9 synchronized; login capture suppressed");

            long lastInbound = System.nanoTime();
            while (System.nanoTime() < variantDeadline) {
                long quietDeadline = Math.min(
                        variantDeadline,
                        lastInbound + QUIET_PERIOD.toNanos());
                byte[] inbound = reader.readFrame(quietDeadline);
                if (inbound == null) {
                    if (System.nanoTime() >= variantDeadline) {
                        ProbeResult result = new ProbeResult(
                                variant.name(), "NO_QUIET_PHASE", "");
                        writeResult(evidence, result);
                        return result;
                    }
                    break;
                }

                writeEvent(evidence, "RX", inbound, "pre-probe");
                lastInbound = System.nanoTime();
                if ("CK".equals(On4KstProtocol.opcode(frameText(inbound)))) {
                    byte[] response = "\r\n".getBytes(StandardCharsets.US_ASCII);
                    output.write(response);
                    output.flush();
                    writeEvent(evidence, "TX", response,
                            "response to server-initiated CK");
                }
            }

            output.write(variant.bytes());
            output.flush();
            writeEvent(evidence, "TX", variant.bytes(), "client probe");

            long responseDeadline = Math.min(
                    variantDeadline,
                    System.nanoTime() + RESPONSE_TIMEOUT.toNanos());
            boolean receivedOtherFrame = false;
            while (System.nanoTime() < responseDeadline) {
                byte[] inbound = reader.readFrame(responseDeadline);
                if (inbound == null) {
                    break;
                }
                writeEvent(evidence, "RX", inbound, "post-probe");
                String text = frameText(inbound);
                if (On4KstProtocol.isClientLivenessProbeResponse(text)) {
                    String status = receivedOtherFrame
                            ? "OK_AFTER_OTHER_DATA"
                            : "OK_DIRECT";
                    ProbeResult result = new ProbeResult(
                            variant.name(), status, escapedAscii(inbound));
                    writeResult(evidence, result);
                    return result;
                }
                receivedOtherFrame = true;
            }

            ProbeResult result = new ProbeResult(
                    variant.name(),
                    receivedOtherFrame ? "OTHER_DATA_ONLY" : "NO_RESPONSE",
                    "");
            writeResult(evidence, result);
            return result;
        }
    }

    private void completeHandshake(
            ChatPreferences preferences,
            WireReader reader,
            OutputStream output,
            long variantDeadline
    ) throws Exception {
        long handshakeDeadline = Math.min(
                variantDeadline,
                System.nanoTime() + HANDSHAKE_TIMEOUT.toNanos());

        byte[] prompt = requireFrame(reader, handshakeDeadline,
                "ON4KST login prompt");
        if (!frameText(prompt).toLowerCase().contains("login")) {
            throw new IOException("Unexpected ON4KST login prompt opcode: "
                    + On4KstProtocol.opcode(frameText(prompt)));
        }

        String login = On4KstProtocol.login(
                preferences.getStn_loginCallSign(),
                preferences.getStn_loginPassword(),
                CATEGORY,
                "KST4Contest v"
                        + ApplicationConstants.APPLICATION_CURRENT_VERSION,
                0L);
        writeCrLfFrame(output, login);

        boolean loginAccepted = false;
        while (!loginAccepted) {
            byte[] inbound = requireFrame(reader, handshakeDeadline,
                    "ON4KST LOGSTAT");
            String text = frameText(inbound);
            if (!"LOGSTAT".equals(On4KstProtocol.opcode(text))) {
                continue;
            }
            String[] fields = text.split("\\|", -1);
            if (fields.length < 2 || !"100".equals(fields[1])) {
                throw new IOException("ON4KST login rejected with code "
                        + (fields.length < 2 ? "missing" : fields[1]));
            }
            loginAccepted = true;
        }

        writeCrLfFrame(output, On4KstProtocol.settingsDone(CATEGORY));
        while (true) {
            byte[] inbound = requireFrame(reader, handshakeDeadline,
                    "category-9 user-list completion");
            String text = frameText(inbound);
            if (text.startsWith("UE|" + CATEGORY + "|")) {
                return;
            }
        }
    }

    private byte[] requireFrame(
            WireReader reader,
            long deadline,
            String description
    ) throws IOException {
        byte[] frame = reader.readFrame(deadline);
        if (frame == null) {
            throw new SocketTimeoutException(
                    "Timed out waiting for " + description);
        }
        return frame;
    }

    private void writeCrLfFrame(OutputStream output, String frame)
            throws IOException {
        output.write(frame.getBytes(StandardCharsets.US_ASCII));
        output.write('\r');
        output.write('\n');
        output.flush();
    }

    private void writeHeader(Writer evidence) throws IOException {
        evidence.write("ON4KST client-probe wire evidence\n");
        evidence.write("started=" + DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.now()) + "\n");
        evidence.write("category=9\n");
        evidence.write("login and initial synchronization frames are suppressed"
                + " to exclude credentials and login tokens\n");
        evidence.write("wtKST reference: server CK frame="
                + "43 4B 7C 0D 0A (CK|<CR><LF>)\n");
        evidence.write("wtKST reference: no client CK and no server OK occur"
                + " in wtkstcomm.c\n");
        evidence.write("wtKST reference: repeated client idle payload="
                + "0D 00 0D 0A (<CR><NUL><CR><LF>)\n");
    }

    private void writeEvent(
            Writer evidence,
            String direction,
            byte[] bytes,
            String note
    ) throws IOException {
        evidence.write(DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.now()));
        evidence.write(" " + direction);
        if (bytes.length > 0) {
            evidence.write(" hex=" + HexFormat.ofDelimiter(" ")
                    .withUpperCase().formatHex(bytes));
            evidence.write(" ascii=" + escapedAscii(bytes));
        }
        if (hasText(note)) {
            evidence.write(" note=" + note);
        }
        evidence.write("\n");
        evidence.flush();
    }

    private void writeResult(Writer evidence, ProbeResult result)
            throws IOException {
        evidence.write("RESULT variant=" + result.variant()
                + " status=" + result.status());
        if (hasText(result.detail())) {
            evidence.write(" detail=" + result.detail());
        }
        evidence.write("\n");
    }

    private static ProbeVariant variant(String name, String wireText) {
        return new ProbeVariant(
                name, wireText.getBytes(StandardCharsets.US_ASCII));
    }

    private String frameText(byte[] frame) {
        int length = frame.length;
        while (length > 0 && (frame[length - 1] == '\r'
                || frame[length - 1] == '\n'
                || frame[length - 1] == 0)) {
            length--;
        }
        return new String(frame, 0, length, StandardCharsets.US_ASCII);
    }

    private String escapedAscii(byte[] bytes) {
        StringBuilder escaped = new StringBuilder();
        for (byte value : bytes) {
            int unsigned = Byte.toUnsignedInt(value);
            switch (unsigned) {
                case 0 -> escaped.append("<NUL>");
                case '\r' -> escaped.append("<CR>");
                case '\n' -> escaped.append("<LF>");
                default -> {
                    if (unsigned >= 0x20 && unsigned <= 0x7e) {
                        escaped.append((char) unsigned);
                    } else {
                        escaped.append(String.format("<%02X>", unsigned));
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "no detail" : message.replaceAll("[\\r\\n]+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ProbeVariant(String name, byte[] bytes) {
        private ProbeVariant {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    private record ProbeResult(String variant, String status, String detail) {
    }

    private static final class WireReader {
        private final InputStream input;

        private WireReader(InputStream input) {
            this.input = input;
        }

        private byte[] readFrame(long deadlineNanos) throws IOException {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            while (System.nanoTime() < deadlineNanos) {
                try {
                    int value = input.read();
                    if (value < 0) {
                        throw new IOException("ON4KST closed the TCP session");
                    }
                    frame.write(value);
                    if (value == '\n' || value == 0) {
                        return frame.toByteArray();
                    }
                } catch (SocketTimeoutException timeout) {
                    // Continue until the caller's monotonic deadline expires.
                }
            }
            return frame.size() == 0 ? null : frame.toByteArray();
        }
    }
}
