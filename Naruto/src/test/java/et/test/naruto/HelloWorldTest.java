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
import et.naruto.Util.ZKArgs;
import et.naruto.ValueFetcher;
import et.naruto.ValueRegister;
import et.naruto.ZKProcess;



public class HelloWorldTest {
    public static ZKArgs zkargs=new ZKArgs("localhost:2181");
    public static String base_path="/naruto_test";
    public static ZooKeeper zk=zkargs.Create();
    @Before
    public void setUp() {
    }
    @After
    public void tearDown() {
        Util.ForceDeleteNode(zk,base_path);
    }
    @Test
    public void ZKProcessTestFetcher() {
        ValueFetcher vf=new ValueFetcher(base_path);
        ZKProcess process=new ZKProcess("test",zkargs);
        process.AddProcesser(vf);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        Util.ForceCreateNode(process.zk,base_path,"hello");
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals("hello"));
        Util.ForceDeleteNode(process.zk,base_path);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestChildsFetcher() {
        ValueFetcher vf=new ValueFetcher(base_path);
        ChildsFetcher cf=new ChildsFetcher(base_path);
        ZKProcess process=new ZKProcess("test",zkargs);
        process.AddProcesser(vf);
        process.AddProcesser(cf);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        Util.ForceCreateNode(process.zk,base_path,"hello",true);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals("hello"));
        Util.ForceCreateNode(process.zk,base_path+"/sub_test_1","hello1");
        Util.ForceCreateNode(process.zk,base_path+"/sub_test_2","hello2");
        Util.ForceCreateNode(process.zk,base_path+"/sub_test_3","hello3");
        Util.Sleep(3*1000);
        Assert.assertTrue(cf.result.size()==3);
        Util.Sleep(3*1000);
        Util.ForceDeleteNode(process.zk,base_path);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestRegister() {
        ValueFetcher vf=new ValueFetcher(base_path);
        ZKProcess process=new ZKProcess("test",zkargs);
        process.AddProcesser(vf);
        Util.ForceDeleteNode(process.zk,base_path);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals(""));
        ValueRegister vr=new ValueRegister(new ValueRegister.Request(base_path,"hello",CreateMode.EPHEMERAL));
        process.AddProcesser(vr);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result.value.equals("hello"));
        process.Close();
        Util.ForceDeleteNode(process.zk,base_path);
    }
    @Test
    public void ServerTest() {
        Util.ForceCreateNode(zk,base_path,"server_test",true);
        ArrayList<Server> ss=new ArrayList();
        for(int i=0;i<10;i++) {
            ss.add(new Server(new Server.Args(base_path,"floor4:localhost", "server"+i),zkargs));
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
