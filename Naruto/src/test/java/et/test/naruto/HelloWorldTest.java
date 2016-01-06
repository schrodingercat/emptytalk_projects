package et.test.naruto;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import et.naruto.base.Util;
import et.naruto.base.Util.DIAG;
import et.naruto.base.Util.ZKArgs;
import et.naruto.election.Args;
import et.naruto.election.Server;
import et.naruto.process.ChildsFetcher;
import et.naruto.process.ValueFetcher;
import et.naruto.process.ValueRegister;
import et.naruto.process.ZKProcess;



public class HelloWorldTest {
    public static ZKArgs zkargs=new ZKArgs("localhost:2181");
    public static String base_path="/naruto_test";
    public static ZooKeeper zk=zkargs.Create();
    @Before
    public void setUp() {
        Util.ForceDeleteNode(zk,base_path);
    }
    @After
    public void tearDown() {
        Util.ForceDeleteNode(zk,base_path);
    }
    @Test
    public void ZKProcessTestFetcher() {
        ZKProcess process=new ZKProcess("test",zkargs);
        ValueFetcher vf=new ValueFetcher(process,base_path);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals(""));
        Util.ForceCreateNode(process.zk,base_path,"hello");
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals("hello"));
        Util.ForceDeleteNode(process.zk,base_path);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestChildsFetcher() {
        ZKProcess process=new ZKProcess("test",zkargs);
        ValueFetcher vf=new ValueFetcher(process,base_path);
        ChildsFetcher cf=new ChildsFetcher(process,base_path);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals(""));
        Util.ForceCreateNode(process.zk,base_path,"hello",true);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals("hello"));
        Util.ForceCreateNode(process.zk,base_path+"/sub_test_1","hello1");
        Util.ForceCreateNode(process.zk,base_path+"/sub_test_2","hello2");
        Util.ForceCreateNode(process.zk,base_path+"/sub_test_3","hello3");
        Util.Sleep(3*1000);
        Assert.assertTrue(cf.result().childs.size()==3);
        Util.Sleep(3*1000);
        Util.ForceDeleteNode(process.zk,base_path);
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals(""));
        process.Close();
    }
    @Test
    public void ZKProcessTestRegister() {
        ZKProcess process=new ZKProcess("test",zkargs);
        ValueFetcher vf=new ValueFetcher(process,base_path);
        Util.ForceDeleteNode(process.zk,base_path);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals(""));
        ValueRegister vr=new ValueRegister(process,new ValueRegister.Request(base_path,"hello",CreateMode.EPHEMERAL));
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals("hello"));
        process.Close();
        Util.ForceDeleteNode(process.zk,base_path);
    }
    @Test
    public void ServerTest() {
        Util.ForceCreateNode(zk,base_path,"server_test",true);
        ArrayList<Server> ss=new ArrayList();
        long length=30;
        for(int i=0;i<length;i++) {
            ss.add(new Server(new Args(base_path,"floor4:localhost", "server"+i),zkargs));
        }
        for(Server s:ss) {
            s.Start();
        }
        Util.Sleep(500);
        String resolutions_data=Util.GetNodeData(zk,base_path+"/Resolutions");
        Assert.assertTrue(!resolutions_data.isEmpty());
        int pure_pre_leader_count=0;
        while(true) {
            Util.Sleep(500);
            int pre_leader_count=0;
            int leader_count=0;
            Server pre_leader=null;
            Server leader=null;
            for(Server s:ss) {
                if(s.master.IsPreLeader()) {
                    pre_leader_count++;
                    pre_leader=s;
                }
                if(s.master.IsLeader()) {
                    leader_count++;
                    leader=s;
                }
            }
            if(pre_leader_count>1) {
                Assert.assertTrue(false);
            }
            if(leader_count>1) {
                Assert.assertTrue(false);
            }
            if(ss.size()%2==1) {
                if(pre_leader_count>=1) {
                    if(ss.size()==21) {
                        int i=0;
                        i++;
                    }
                    DIAG.Get.d.info(String.format("Stop the pre_leader server: %s", pre_leader));
                    pre_leader.Stop();
                    if(!pre_leader.master.IsLeader()) {
                         pure_pre_leader_count++;
                    }
                    ss.remove(pre_leader);
                }
            } else {
                if(leader_count>=1) {
                    DIAG.Get.d.info(String.format("Stop the leader server: %s", leader));
                    leader.Stop();
                    ss.remove(leader);
                }
            }
            if(ss.size()==0) {
                break;
            }
        }
        Assert.assertTrue(Util.GetNodeChilds(zk,base_path+"/Resolutions").size()==(length-pure_pre_leader_count));
        Assert.assertTrue(Util.GetNodeChilds(zk,base_path+"/ResolutionsClosed").size()==(length-1-pure_pre_leader_count));
    }
    @Test
    public void getHelloWorld_ShouldPrintHelloWorld() {
        assertEquals("helloworld","helloworld");
    }
}
