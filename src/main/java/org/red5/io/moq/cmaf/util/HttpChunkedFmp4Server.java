package org.red5.io.moq.cmaf.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.SampleFlags;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP chunked fMP4 demo server. This is intended for local testing and
 * should be wired with real codec configs and media payloads for actual playback.
 */
public class HttpChunkedFmp4Server {

    private final int port;
    private final byte[] initSegment;
    private final List<byte[]> fragments;
    private final long fragmentDelayMillis;
    private HttpServer server;

    public HttpChunkedFmp4Server(int port, byte[] initSegment, List<byte[]> fragments, long fragmentDelayMillis) {
        this.port = port;
        this.initSegment = initSegment;
        this.fragments = fragments;
        this.fragmentDelayMillis = fragmentDelayMillis;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/stream", new StreamHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "video/mp4");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(initSegment);
                out.flush();

                for (byte[] fragment : fragments) {
                    out.write(fragment);
                    out.flush();
                    sleepQuietly(fragmentDelayMillis);
                }
            }
        }
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] avcC = boxBytes("avcC", new byte[] { 1, 2, 3, 4 });
        byte[] esds = boxBytes("esds", new byte[] { 5, 6, 7, 8 });

        byte[] initSegment = new Fmp4InitSegmentBuilder()
            .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                1, 90000, "avc1", avcC, 640, 360))
            .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                2, 48000, "mp4a", esds, 2, 48000, 16))
            .build();

        byte[] mediaData = new byte[] { 9, 8, 7, 6, 5, 4 };
        Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
            .setSequenceNumber(1)
            .setTrackId(1)
            .setBaseDecodeTime(0)
            .setMediaData(mediaData)
            .setMediaType(CmafFragment.MediaType.VIDEO)
            .setSamples(List.of(
                new Fmp4FragmentBuilder.SampleData(3000, mediaData.length, SampleFlags.createSyncSampleFlags())
            ));

        byte[] fragmentBytes = new Fmp4FragmentBuilder().buildFragment(config).serialize();

        HttpChunkedFmp4Server server = new HttpChunkedFmp4Server(
            8080, initSegment, List.of(fragmentBytes), 1000);
        server.start();
        System.out.println("Streaming on http://localhost:8080/stream");
    }

    private static byte[] boxBytes(String type, byte[] payload) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        int size = 8 + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(size);
        buffer.put(typeBytes, 0, 4);
        buffer.put(payload);
        return buffer.array();
    }
}
