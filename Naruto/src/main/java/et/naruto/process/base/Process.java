package et.naruto.process.base;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;

public class Process extends Thread {
    private long count=0;
    private final HashSet<Processer> processers=new HashSet();
    private boolean running=false;
    private final AtomicBoolean ticked=new AtomicBoolean(false);
    private final ReentrantLock lock=new ReentrantLock();
    private final Condition cond=lock.newCondition();
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
                Util.DIAG.Log.d.pass_error("",e);
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
                try {
                    this.lock.lock();
                    if(!ticked.getAndSet(false)) {
                        try {
                            DIAG.Log.________________________________________________________________.D("Process Into WWWWWWWWWWWWWaiTTTTTTTTTTTTT Count(%s) !!!!!",count);
                            this.cond.await();
                        } catch (Exception e) {
                            DIAG.Log.d.pass_error("",e);
                        }
                    }
                } finally {
                    this.lock.unlock();
                }
            }
            count++;
        }
    }
    public void Tick() {
        if(Thread.currentThread().getId()==super.getId()) {
            ticked.set(true);
            return;
        }
        if(ticked.getAndSet(true)) {
        } else {
            try {
                this.lock.lock();
                DIAG.Log.________________________________________________________________.D("Process TTTTTTTTTTTicKKKKKKKKKKKKKKK from  [TID=%s] Count(%s) !!!!!",super.getId(),count);
                this.cond.signal();
            } finally {
                this.lock.unlock();
            }
        }
    }
}
