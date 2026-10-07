package com.koda.cut.beta;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal scene graph for Koda Cut.
 *
 * Inspired by modern non-linear editors: the AI/KodaScript describes intent,
 * this model normalizes it into tracks/elements before the renderer executes it.
 */
public final class KodaScene {
    public enum TrackType {
        VIDEO,
        TEXT,
        AUDIO,
        GRAPHIC,
        EFFECT
    }

    public final String format;
    public final String mainFit;
    public final int width;
    public final int height;
    public final List<Track> tracks;

    KodaScene(
        String format,
        String mainFit,
        int width,
        int height,
        List<Track> tracks
    ) {
        this.format = format;
        this.mainFit = mainFit;
        this.width = width;
        this.height = height;
        this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
    }

    public int elementCount() {
        int total = 0;
        for (Track track : tracks) total += track.elements.size();
        return total;
    }

    public int count(TrackType type) {
        int total = 0;
        for (Track track : tracks) {
            if (track.type == type) total += track.elements.size();
        }
        return total;
    }

    public static final class Track {
        public final String id;
        public final String name;
        public final TrackType type;
        public final boolean main;
        public final List<Element> elements;

        Track(
            String id,
            String name,
            TrackType type,
            boolean main,
            List<Element> elements
        ) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.main = main;
            this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
        }
    }

    public static final class Element {
        public final String id;
        public final String action;
        public final String asset;
        public final double start;
        public final double duration;
        public final double sourceStart;
        public final JSONObject params;
        public final List<Keyframe> keyframes;

        Element(
            String id,
            String action,
            String asset,
            double start,
            double duration,
            double sourceStart,
            JSONObject params,
            List<Keyframe> keyframes
        ) {
            this.id = id;
            this.action = action;
            this.asset = asset;
            this.start = start;
            this.duration = duration;
            this.sourceStart = sourceStart;
            this.params = params;
            this.keyframes = Collections.unmodifiableList(new ArrayList<>(keyframes));
        }
    }

    public static final class Keyframe {
        public final double time;
        public final Double x;
        public final Double y;
        public final Double scale;
        public final Double rotation;
        public final Double opacity;
        public final String easing;

        Keyframe(
            double time,
            Double x,
            Double y,
            Double scale,
            Double rotation,
            Double opacity,
            String easing
        ) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotation = rotation;
            this.opacity = opacity;
            this.easing = easing;
        }
    }
}
