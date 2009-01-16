package org.fluidity.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides static access to Fluid Tools internal settings.
 */
public final class SystemSettings {

    public static final String FLUIDITY = "fluidity";
    public static final String SYSTEM_PREFIX = FLUIDITY + ":";

    private SystemSettings() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    private static final Map<String, String> properties = new HashMap<String, String>();

    static {
        final Properties props = new Properties();

        try {
            final ClassLoader classloader = ClassLoaderUtils.findClassLoader(SystemSettings.class);
            final InputStream stream = classloader.getResourceAsStream(FLUIDITY + ".properties");

            if (stream != null) {
                props.load(stream);

                for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                    properties.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(final String key, final String value) {
        synchronized (properties) {
            properties.put(key, value);
        }
    }

    public static boolean isSet(final String key, final String... values) {
        synchronized (properties) {
            if (!properties.containsKey(key)) {
                properties.put(key, System.getProperty(SYSTEM_PREFIX + key));
            }
        }

        final String value = get(key);

        for (final String expected : values) {
            if (expected.equals(value)) {
                return true;
            }
        }

        return false;
    }

    private static String get(String key) {
        synchronized (properties) {
            return properties.get(key);
        }
    }
}
