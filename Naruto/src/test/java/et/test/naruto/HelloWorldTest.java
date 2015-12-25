package et.test.naruto;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import et.naruto.Server;
import et.naruto.Util;
import et.naruto.Util.DIAG;

class Flow {
    private long in=0;
    private long doing=0;
    private long done=0;
    private long fetch=0;
    public Flow(long in) {
        this.in=in;
    }
    public void AddIn() {
        in++;
    }
    public boolean NeedDoing() {
        long t_in=in;
        if(t_in>doing) {
            doing=t_in;
            return true;
        }
        return false;
    }
    public void Done() {
        done=doing;
    }
    public boolean NeedFetch() {
        long t_done=done;
        if(t_done>fetch) {
            fetch=t_done;
            return true;
        }
        return false;
    }
}

class ValueFetcher {
    public final Flow flow=new Flow(1);
    public String value=null;
    public ValueFetcher() {
    }
    public String Fetch() {
        if(this.flow.NeedFetch()) {
            return this.value;
        }
        return null;
    }
    public boolean Doing(ZooKeeper zk,String name) {
        final ValueFetcher value_fetcher_ref=this;
        if(value_fetcher_ref.flow.NeedDoing()) {
            zk.getData(
                name,
                new Watcher() {
                    public void process(WatchedEvent event) {
                        value_fetcher_ref.flow.AddIn();
                    }
                },
                new DataCallback() {
                    public void processResult(
                                int rc,
                                String path,
                                Object ctx,
                                byte data[],
                                Stat stat) {
                        switch(rc) {
                        case 0:
                            try {
                                value_fetcher_ref.value=new String(data,"UTF-8");
                            } catch (Exception e) {
                                DIAG.Get.d.dig_error("",e);
                                value_fetcher_ref.value="";
                            } finally {
                            }
                            break;
                        default:
                            value_fetcher_ref.value="";
                            break;
                        }
                        value_fetcher_ref.flow.Done();
                    }
                },
                null
            );
            return true;
        }
        return false;
    }
}

class ValueRegister{
    public final Flow flow=new Flow(0);
    public final String want_value;
    public final CreateMode mode;
    public boolean succ=false;
    public ValueRegister(final String want_value,final CreateMode mode) {
        this.want_value=want_value;
        this.mode=mode;
    }
    public boolean Doing(ZooKeeper zk,String name) {
        final ValueRegister value_register_ref=this;
        if(value_register_ref.flow.NeedDoing()) {
            zk.create(
                name,
                value_register_ref.want_value.getBytes(),
                Ids.OPEN_ACL_UNSAFE,
                value_register_ref.mode,
                new StringCallback() {
                    public void processResult(int rc, String path, Object ctx, String name) {
                        switch(rc) {
                        case 0:
                            value_register_ref.succ=true;
                            break;
                        default:
                            value_register_ref.succ=false;
                            break;
                        }
                        value_register_ref.flow.Done();
                    }
                },
                null
            );
            return true;
        }
        return false;
    }
}

class ChildsFetcher {
    public final Flow flow;
    public List<Status> childs=new ArrayList();
    public List<String> childnames=null;
    public ChildsFetcher(List<Status> childs) {
        this.flow=new Flow(childs.size()==0?1:0);
        this.childs=childs;
    }
    private int MergeChilds(String name) {
        int count=0;
        if(this.flow.NeedFetch()) {
            Set<String> old=new HashSet();
            for(Status child:childs) {
                old.add(child.name);
            }
            for(String childname:childnames) {
                if(!old.contains(childname)) {
                    childs.add(
                        new Status(
                            name+"/"+childname,
                            new ValueFetcher(),
                            null,
                            null
                        )
                    );
                    count++;
                }
            }
        }
        return count;
    }
    public boolean Doing(ZooKeeper zk,String name) {
        final ChildsFetcher childs_fetcher_ref=this;
        boolean changed=false;
        if(childs_fetcher_ref.flow.NeedDoing()) {
            zk.getChildren(
                name,
                new Watcher() {
                    public void process(WatchedEvent event) {
                        childs_fetcher_ref.flow.AddIn();
                    }
                },
                new ChildrenCallback() {
                    public void processResult(
                            int rc,
                            String path,
                            Object ctx,
                            List<String> children) {
                        switch(rc) {
                        case 0:
                            childs_fetcher_ref.childnames=children;
                            break;
                        default:
                            childs_fetcher_ref.childnames=new ArrayList();
                            break;
                        }
                        childs_fetcher_ref.flow.Done();
                    }
                },
                null
            );
            changed=true;
        }
        if(MergeChilds(name)>0) {
            changed=true;
        }
        for(Status child:childs) {
            if(child.Doing(zk)) {
                changed=true;
            }
        }
        return changed;
    }
}

class Status {
    public final String name;
    public final ValueFetcher value_fetcher;
    public final ValueRegister value_register;
    public final ChildsFetcher childs_fetcher;
    public Status(
            final String name,
            final ValueFetcher value_fetcher,
            final ValueRegister value_register,
            final ChildsFetcher childs_fetcher
    ) {
        this.name=name;
        this.value_fetcher=value_fetcher;
        this.value_register=value_register;
        this.childs_fetcher=childs_fetcher;
    }
    public boolean Doing(ZooKeeper zk) {
        final Status status_ref=this;
        final ValueFetcher value_fetcher_ref=status_ref.value_fetcher;
        final ValueRegister value_register_ref=status_ref.value_register;
        final ChildsFetcher childs_fetcher_ref=status_ref.childs_fetcher;
        boolean changed=false;
        if(value_fetcher_ref!=null) {
            if(value_fetcher_ref.Doing(zk, status_ref.name)) {
                changed=true;
            }
            String new_value=value_fetcher_ref.Fetch();
            if(new_value!=null) {
                if(value_register_ref!=null) {
                    value_register_ref.flow.AddIn();
                    changed=true;
                }
            }
        }
            
        if(value_register_ref!=null) {
            value_register_ref.Doing(zk,status_ref.name);
            if(value_register_ref.flow.NeedFetch()) {
                value_fetcher_ref.flow.AddIn();
                changed=true;
            }
        }
        if(childs_fetcher_ref!=null) {
            if(childs_fetcher_ref.Doing(zk,status_ref.name)) {
                changed=true;
            }
        }
        return changed;
    }
}

class ZKProcess extends Thread {
    public final ZooKeeper zk;
    public final Status status;
    public boolean running=false;
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
    public ZKProcess(final Status status) {
        super("zkprocess:"+status.name);
        this.status=status;
        this.zk=CreateZooKeeper(
            new Watcher() {
                public void process(WatchedEvent event) {
                }
            }
        );
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
            }
        }
    }
    public void run() {
        running=true;
        while(true) {
            final Status status_ref=this.status;
            boolean changed=status_ref.Doing(zk);
            if(!running) {
                break;
            }
            if(changed) {
                Util.Sleep(200);
            } else {
                Util.Sleep(1000);
            }
        }
    }
}

public class HelloWorldTest {
    @Before
    public void setUp() {
    }
    @After
    public void tearDown() {
    }
    @Test
    public void ZKUtilTest() {
        try {
            ZooKeeper zk=new ZooKeeper("localhost:2181",10*1000,null);
            Util.ForceDeleteNode(zk,"/test");
            Util.ForceDeleteNode(zk,"/test1");
            zk.close();
        } catch (Exception e) {
            Util.DIAG.Get.d.pass_error("",e);
        }
    }
    @Test
    public void ZKProcessTestFetcher() {
        Status status=new Status(
            "/test1",
            new ValueFetcher(),
            null,
            null
        );
        ZKProcess process=new ZKProcess(status);
        Util.ForceCreateNode(process.zk,"/test1","hello");
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(status.value_fetcher.value.equals("hello"));
        Util.ForceDeleteNode(process.zk,"/test1");
        Util.Sleep(3*1000);
        Assert.assertTrue(status.value_fetcher.value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestChildsFetcher() {
        Status status=new Status(
            "/test1",
            new ValueFetcher(),
            null,
            new ChildsFetcher(new ArrayList())
        );
        ZKProcess process=new ZKProcess(status);
        Util.ForceCreateNode(process.zk,"/test1","hello",true);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(status.value_fetcher.value.equals("hello"));
        Util.ForceCreateNode(process.zk,"/test1/sub_test_1","hello1");
        Util.ForceCreateNode(process.zk,"/test1/sub_test_2","hello2");
        Util.ForceCreateNode(process.zk,"/test1/sub_test_3","hello3");
        Util.Sleep(3*1000);
        Assert.assertTrue(status.childs_fetcher.childs.size()==3);
        Assert.assertTrue(status.childs_fetcher.childs.get(0).value_fetcher.value.indexOf("hello")==0);
        Util.Sleep(3*1000);
        Util.ForceDeleteNode(process.zk,"/test1");
        Util.Sleep(3*1000);
        Assert.assertTrue(status.value_fetcher.value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestRegister() {
        Status status=new Status(
            "/test1",
            new ValueFetcher(),
            new ValueRegister("hello",CreateMode.EPHEMERAL),
            null
        );
        ZKProcess process=new ZKProcess(status);
        Util.ForceDeleteNode(process.zk,"/test1");
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(status.value_fetcher.value.equals("hello"));
        Util.ForceDeleteNode(process.zk,"/test1");
        Util.Sleep(3*1000);
        Assert.assertTrue(status.value_fetcher.value.equals("hello"));
        process.Close();
        Util.ForceDeleteNode(process.zk,"/test1");
    }
    @Test
    public void ServerTest() {
        ArrayList<Server> ss=new ArrayList();
        for(int i=0;i<10;i++) {
            ss.add(new Server(new Server.Args("/","floor4:localhost", "server"+i)));
        }
        for(Server s:ss) {
            s.Start();
        }
        while(true) {
            try {
                Thread.sleep(3*1000);
                int leader_count=0;
                Server leader=null;
                for(Server s:ss) {
                    if(s.IsLeader()) {
                        leader_count++;
                        leader=s;
                    }
                }
                if(leader_count>1) {
                    Assert.assertFalse(false);
                }
                if(leader_count>=1) {
                    DIAG.Get.d.info(String.format("Stop the leader server: %s", leader));
                    leader.Stop();
                    ss.remove(leader);
                }
                if(ss.size()==0) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }
    @Test
    public void getHelloWorld_ShouldPrintHelloWorld() {
        assertEquals("helloworld","helloworld");
    }
}
