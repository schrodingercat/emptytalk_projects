package et.naruto;

import java.util.HashSet;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.ZooKeeper;

import et.naruto.Util.DIAG;
import et.naruto.Util.ZKArgs;

public class ZKProcess extends Thread {
    public final ZooKeeper zk;
    public final Timer tm;
    private final HashSet<Processer> processers=new HashSet();
    private boolean running=false;
    private AtomicBoolean ticked=new AtomicBoolean(false);
    public ZKProcess(final String name,final ZKArgs zkargs) {
        super("zkprocess:"+name);
        this.tm=new Timer();
        this.zk=zkargs.Create();
    }
    protected void AddProcesser(final Processer processer) {
        this.processers.add(processer);
    }
    protected void DelProcesser(final Processer processer) {
        this.processers.remove(processer);
    }
    public void Close() {
        Stop();
        try {
            this.zk.close();
        } catch (Exception e) {
            Util.DIAG.Get.d.pass_error("",e);
        }
    }
    public void Start() {
        super.start();
    }
    public void Stop() {
        if(running) {
            running=false;
            try {
                super.join();
            } catch (Exception e) {
                Util.DIAG.Get.d.pass_error("",e);
            } finally {
                tm.cancel();
                try {
                    zk.close();
                } catch (Exception e) {
                    Util.DIAG.Get.d.pass_error("",e);
                }
            }
        }
    }
    public void DoRun() {
    }
    public void run() {
        running=true;
        while(true) {
            boolean changed=false;
            for(Processer processer:processers) {
                if(processer.Do()) {
                    changed=true;
                }
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
