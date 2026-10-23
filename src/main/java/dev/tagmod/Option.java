package dev.tagmod;

/** A resolved value is saved, rather than re-reading client configuration. */
public record Option(String key, double value, String prefix, String description) {}
