package org.fluidity.foundation;

/**
 * Mock implementation of the {@link org.fluidity.foundation.ApplicationInfo} interface.
 */
public final class MockApplicationInfo implements ApplicationInfo {

    private final String shortName;
    private final String name;

    public MockApplicationInfo(final String shortName, String name) {
        this.shortName = shortName;
        this.name = name;
    }

    public String applicationShortName() {
        if (shortName == null) {
            throw new UnsupportedOperationException();
        }

        return shortName;
    }

    public String applicationName() {
        if (name == null) {
            throw new UnsupportedOperationException();
        }

        return name;
    }
}
