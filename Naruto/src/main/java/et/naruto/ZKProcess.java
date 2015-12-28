package et.naruto;

import java.util.HashSet;
import java.util.Timer;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import et.naruto.Util.DIAG;

public class ZKProcess extends Thread {
    public final ZooKeeper zk;
    public final Timer tm;
    private final HashSet<Processer> processers=new HashSet();
    private boolean running=false;
    private static ZooKeeper CreateZooKeeper(Watcher wt) {
        try {
            return new ZooKeeper(
                "localhost:2181",
                10*1000,
                wt
            );
        } catch (Exception e) {
            DIAG.Get.d.pass_error("",e);
            return null;
        }
    }
    public ZKProcess(String name) {
        super("zkprocess:"+name);
        this.tm=new Timer();
        this.zk=CreateZooKeeper(
            new Watcher() {
                public void process(WatchedEvent event) {
                }
            }
        );
    }
    public void AddProcesser(final Processer processer) {
        this.processers.add(processer);
    }
    public void DelProcesser(final Processer processer) {
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
                if(processer.Tick(this)) {
                    changed=true;
                }
            }
            DoRun();
            if(!running) {
                break;
            }
            if(!changed) {
                Util.Sleep(1000);
            } else {
                Util.Sleep(1000);
            }
        }
    }
}
