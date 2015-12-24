package et.naruto;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import et.naruto.Util.DIAG;

public enum ZooKeeperInstance {
    Get;
    public final ZooKeeper zk;
    private static ZooKeeper CreateZooKeeper() {
        try {
            return new ZooKeeper(
                "localhost:2181",
                10*1000,
                new Watcher() {
                    public void process(WatchedEvent event) {
                    }
                }
            );
        } catch (Exception e) {
            DIAG.Get.d.pass_error("",e);
            return null;
        }
    }
    private ZooKeeperInstance() {
        this.zk=CreateZooKeeper();
    }
}
