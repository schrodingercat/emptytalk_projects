package et.test.naruto;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import et.naruto.Args;
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
        for(int i=0;i<30;i++) {
            ss.add(new Server(new Args(base_path,"floor4:localhost", "server"+i),zkargs));
        }
        for(Server s:ss) {
            s.Start();
        }
        Util.Sleep(3*1000);
        String resolutions_data=Util.GetNodeData(zk,base_path+"/Resolutions");
        Assert.assertTrue(!resolutions_data.isEmpty());
        while(true) {
            Util.Sleep(3*1000);
            int pre_leader_count=0;
            int leader_count=0;
            Server pre_leader=null;
            Server leader=null;
            for(Server s:ss) {
                if((s.is_pre_leader_out.result!=null)&&s.is_pre_leader_out.result) {
                    pre_leader_count++;
                    pre_leader=s;
                }
                if(s.is_leader_out.result!=null&&s.is_leader_out.result) {
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
        Assert.assertTrue(Util.GetNodeChilds(zk,base_path+"/Resolutions").size()==30);
        Assert.assertTrue(Util.GetNodeChilds(zk,base_path+"/ResolutionsClosed").size()==29);
    }
    @Test
    public void getHelloWorld_ShouldPrintHelloWorld() {
        assertEquals("helloworld","helloworld");
    }
}
