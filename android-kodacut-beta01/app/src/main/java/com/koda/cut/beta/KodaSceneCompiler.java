package com.koda.cut.beta;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts KodaScript into a normalized track/element scene graph.
 *
 * The renderer can evolve independently from the AI-facing script format.
 */
public final class KodaSceneCompiler {
    private KodaSceneCompiler() {}

    public static KodaScene compile(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        String version = root.optString("koda_version", "");
        if (!"android-0.2".equals(version)) {
            throw new Exception("koda_version incompatível para o Scene Engine.");
        }

        String format = root.optString("format", "9:16");
        String mainFit = root.optString("main_fit", "contain");

        int width = 1080;
        int height = 1920;
        if ("16:9".equals(format)) {
            width = 1920;
            height = 1080;
        } else if ("1:1".equals(format)) {
            width = 1080;
            height = 1080;
        } else if ("4:5".equals(format)) {
            width = 1080;
            height = 1350;
        }

        JSONArray timeline = root.optJSONArray("timeline");
        if (timeline == null) throw new Exception("timeline ausente.");

        LinkedHashMap<String, TrackBuilder> builders = new LinkedHashMap<>();
        builders.put("video:main", new TrackBuilder(
            "video:main", "Vídeo principal", KodaScene.TrackType.VIDEO, true
        ));
        builders.put("video:overlay", new TrackBuilder(
            "video:overlay", "Camadas de vídeo", KodaScene.TrackType.VIDEO, false
        ));
        builders.put("text", new TrackBuilder(
            "text", "Texto e legendas", KodaScene.TrackType.TEXT, false
        ));
        builders.put("graphic", new TrackBuilder(
            "graphic", "Imagens e gráficos", KodaScene.TrackType.GRAPHIC, false
        ));
        builders.put("audio", new TrackBuilder(
            "audio", "Áudio", KodaScene.TrackType.AUDIO, false
        ));
        builders.put("effect", new TrackBuilder(
            "effect", "Efeitos e animações", KodaScene.TrackType.EFFECT, false
        ));

        double clipCursor = 0.0;
        int serial = 1;

        for (int i = 0; i < timeline.length(); i++) {
            JSONObject event = timeline.getJSONObject(i);
            String action = event.optString("action", "").trim();
            if (action.isEmpty()) continue;

            String asset = event.optString("asset", "");
            double start;
            double duration;
            double sourceStart = Math.max(0, event.optDouble("source_start", 0));

            String trackKey;
            switch (action) {
                case "clip": {
                    double sourceIn = Math.max(0, event.optDouble("start", 0));
                    double sourceOut = Math.max(sourceIn, event.optDouble("end", sourceIn));
                    duration = Math.max(0, sourceOut - sourceIn);
                    start = clipCursor;
                    sourceStart = sourceIn;
                    asset = "video:principal";
                    clipCursor += duration;
                    trackKey = "video:main";
                    break;
                }
                case "video_layer":
                    start = Math.max(0, event.optDouble("start", 0));
                    duration = durationFromStartEnd(event, start, 1.0);
                    trackKey = "video:overlay";
                    break;
                case "overlay":
                    start = Math.max(0, event.optDouble("at", 0));
                    duration = Math.max(0.05, event.optDouble("duration", 1.5));
                    trackKey = "graphic";
                    break;
                case "text":
                case "caption":
                    start = Math.max(0, event.optDouble("start", 0));
                    duration = durationFromStartEnd(event, start, 2.0);
                    trackKey = "text";
                    break;
                case "music":
                    start = Math.max(0, event.optDouble("start", 0));
                    duration = durationFromStartEnd(event, start, 1.0);
                    trackKey = "audio";
                    break;
                case "sfx":
                    start = Math.max(0, event.optDouble("at", 0));
                    duration = Math.max(0.05, event.optDouble("duration", 1.0));
                    trackKey = "audio";
                    break;
                case "zoom":
                case "animate":
                case "transition":
                case "effect":
                    start = Math.max(0, event.optDouble("start", event.optDouble("at", 0)));
                    duration = durationFromStartEnd(event, start, event.optDouble("duration", 1.0));
                    trackKey = "effect";
                    break;
                default:
                    start = Math.max(0, event.optDouble("start", event.optDouble("at", 0)));
                    duration = durationFromStartEnd(event, start, event.optDouble("duration", 1.0));
                    trackKey = "effect";
                    break;
            }

            JSONObject params = new JSONObject(event.toString());
            params.remove("action");
            params.remove("asset");
            params.remove("start");
            params.remove("end");
            params.remove("at");
            params.remove("duration");
            params.remove("source_start");
            params.remove("keyframes");

            List<KodaScene.Keyframe> keyframes = parseKeyframes(event.optJSONArray("keyframes"));

            KodaScene.Element element = new KodaScene.Element(
                String.format(Locale.US, "e%04d", serial++),
                action,
                asset,
                start,
                duration,
                sourceStart,
                params,
                keyframes
            );
            builders.get(trackKey).elements.add(element);
        }

        List<KodaScene.Track> tracks = new ArrayList<>();
        for (Map.Entry<String, TrackBuilder> entry : builders.entrySet()) {
            TrackBuilder builder = entry.getValue();
            if (builder.main || !builder.elements.isEmpty()) {
                tracks.add(builder.build());
            }
        }

        return new KodaScene(format, mainFit, width, height, tracks);
    }

    public static String summary(KodaScene scene) {
        return scene.tracks.size() + " tracks • " +
            scene.elementCount() + " elementos • " +
            scene.width + "×" + scene.height;
    }

    private static double durationFromStartEnd(
        JSONObject event,
        double start,
        double fallback
    ) {
        if (event.has("end")) {
            double end = Math.max(start, event.optDouble("end", start));
            return Math.max(0.05, end - start);
        }
        return Math.max(0.05, event.optDouble("duration", fallback));
    }

    private static List<KodaScene.Keyframe> parseKeyframes(JSONArray array) {
        List<KodaScene.Keyframe> result = new ArrayList<>();
        if (array == null) return result;

        for (int i = 0; i < array.length(); i++) {
            JSONObject k = array.optJSONObject(i);
            if (k == null) continue;

            result.add(new KodaScene.Keyframe(
                Math.max(0, k.optDouble("time", 0)),
                numberOrNull(k, "x"),
                numberOrNull(k, "y"),
                numberOrNull(k, "scale"),
                numberOrNull(k, "rotation"),
                numberOrNull(k, "opacity"),
                k.optString("easing", "linear")
            ));
        }
        return result;
    }

    private static Double numberOrNull(JSONObject object, String key) {
        Object value = object.opt(key);
        return value instanceof Number ? ((Number)value).doubleValue() : null;
    }

    private static final class TrackBuilder {
        final String id;
        final String name;
        final KodaScene.TrackType type;
        final boolean main;
        final List<KodaScene.Element> elements = new ArrayList<>();

        TrackBuilder(
            String id,
            String name,
            KodaScene.TrackType type,
            boolean main
        ) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.main = main;
        }

        KodaScene.Track build() {
            return new KodaScene.Track(id, name, type, main, elements);
        }
    }
}
