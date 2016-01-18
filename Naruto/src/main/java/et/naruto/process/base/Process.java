package et.naruto.process.base;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;

public class Process extends Thread {
    private final HashSet<Processer> processers=new HashSet();
    private boolean running=false;
    private AtomicBoolean ticked=new AtomicBoolean(false);
    public Process(final String name) {
        super("zkprocess:"+name);
    }
    public final void AddProcesser(final Processer processer) {
        this.processers.add(processer);
        Tick();
    }
    public final void DelProcesser(final Processer processer) {
        this.processers.remove(processer);
        Tick();
    }
    public void Start() {
        super.start();
    }
    public void Stop() {
        if(running) {
            running=false;
            this.Tick();
            try {
                super.join();
            } catch (Exception e) {
                Util.DIAG.Get.d.pass_error("",e);
            } finally {
            }
        }
    }
    public void DoRun() {
    }
    public void run() {
        running=true;
        while(true) {
            boolean changed=false;
            for(Processer processer:processers.toArray(new Processer[0])) {
                if(processer.Do()) {
                    changed=true;
                }
            }
            if(changed) {
                Tick();
            }
            DoRun();
            if(!running) {
                break;
            }
            
            if(!ticked.getAndSet(false)) {
                synchronized(this) {
                    if(!ticked.getAndSet(false)) {
                        try {
                            this.wait(200);
                        } catch (Exception e) {
                            DIAG.Get.d.pass_error("",e);
                        }
                    }
                }
            }
        }
    }
    public void Tick() {
        if(ticked.getAndSet(true)) {
        } else {
            synchronized(this) {
                this.notify();
            }
        }
    }
}
