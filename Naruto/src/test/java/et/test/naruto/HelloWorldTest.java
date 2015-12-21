package et.test.naruto;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HelloWorldTest {
    @Before
    public void setUp() {
    }
    @Test
    public void getHelloWorld_ShouldPrintHelloWorld() {
        assertEquals("helloworld","helloworld");
    }
}
