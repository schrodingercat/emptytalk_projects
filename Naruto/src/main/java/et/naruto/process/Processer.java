package et.naruto.process;

import java.util.concurrent.atomic.AtomicLong;

import et.naruto.versioner.Flow;
import et.naruto.versioner.Handler;


public interface Processer {
    boolean Do();
}
