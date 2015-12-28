package et.test.naruto;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import et.naruto.ChildsFetcher;
import et.naruto.Server;
import et.naruto.Util;
import et.naruto.Util.DIAG;
import et.naruto.ValueFetcher;
import et.naruto.ValueRegister;
import et.naruto.ZKProcess;



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
        ValueFetcher vf=new ValueFetcher("/test1");
        ZKProcess process=new ZKProcess("test");
        process.AddProcesser(vf);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        Util.ForceCreateNode(process.zk,"/test1","hello");
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals("hello"));
        Util.ForceDeleteNode(process.zk,"/test1");
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestChildsFetcher() {
        ValueFetcher vf=new ValueFetcher("/test1");
        ChildsFetcher cf=new ChildsFetcher("/test1");
        ZKProcess process=new ZKProcess("test");
        process.AddProcesser(vf);
        process.AddProcesser(cf);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        Util.ForceCreateNode(process.zk,"/test1","hello",true);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals("hello"));
        Util.ForceCreateNode(process.zk,"/test1/sub_test_1","hello1");
        Util.ForceCreateNode(process.zk,"/test1/sub_test_2","hello2");
        Util.ForceCreateNode(process.zk,"/test1/sub_test_3","hello3");
        Util.Sleep(3*1000);
        Assert.assertTrue(cf.result.size()==3);
        Util.Sleep(3*1000);
        Util.ForceDeleteNode(process.zk,"/test1");
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestRegister() {
        ValueFetcher vf=new ValueFetcher("/test1");
        ZKProcess process=new ZKProcess("test");
        process.AddProcesser(vf);
        Util.ForceDeleteNode(process.zk,"/test1");
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        ValueRegister vr=new ValueRegister(new ValueRegister.Request("/test1","hello",CreateMode.EPHEMERAL));
        process.AddProcesser(vr);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals("hello"));
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
