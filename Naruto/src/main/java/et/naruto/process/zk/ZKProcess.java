package et.naruto.process.zk;

import java.util.HashSet;
import java.util.Timer;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.ZooKeeper;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;
import et.naruto.base.Util.ZKArgs;
import et.naruto.process.base.Process;


public class ZKProcess extends Process implements AutoCloseable {
    public final ZooKeeper zk;
    public final Timer tm;
    public ZKProcess(final String name,final ZKArgs zkargs) {
        super("zkprocess:"+name);
        this.tm=new Timer();
        this.zk=zkargs.Create();
    }
    public void Start() {
        super.start();
    }
    public void close() {
        super.close();
        tm.cancel();
        try {
            zk.close();
        } catch (Exception e) {
            Util.DIAG.Log.d.pass_error("",e);
        }
    }
}
