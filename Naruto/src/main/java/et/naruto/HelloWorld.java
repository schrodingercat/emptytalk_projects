package et.naruto;

import org.apache.log4j.Logger;

public class HelloWorld {
    private static final Logger LOGGER=Logger.getLogger(HelloWorld.class);
    public static void main(String[] args) {
        System.out.println("Hello World!");
        LOGGER.info("Test Log4j Hello World!");
    }
}
