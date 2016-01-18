package et.naruto.process.base;

import java.util.concurrent.atomic.AtomicLong;

import et.naruto.versioner.Flow;
import et.naruto.versioner.base.Handler;


public interface Processer {
    boolean Do();
}
