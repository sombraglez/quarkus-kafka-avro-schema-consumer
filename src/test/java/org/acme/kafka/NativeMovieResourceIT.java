package org.acme.kafka;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeMovieResourceIT extends ConsumedMovieResourceTest {

    // Execute the same tests but in native mode.
}
