package et.test.naruto;

import java.util.ArrayList;
import java.util.TreeSet;

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
import et.naruto.process.zk.ChildsFetcher;
import et.naruto.process.zk.ValueFetcher;
import et.naruto.process.zk.ValueRegister;
import et.naruto.process.zk.ZKProcess;
import et.naruto.register.NodeSync;
import et.naruto.register.RegistersClient;
import et.naruto.register.RegistersSync;
import et.naruto.register.RegistersUpdater;
import et.naruto.register.ServerSync;
import et.naruto.resolutionsurface.ResolutionSurfaceSync;





public class MainTest {
    public static ZKArgs zkargs=new ZKArgs("localhost:2181");
    public static String base_path="/naruto_test";
    public static ZooKeeper zk=zkargs.Create();
    public static Args CreateArgs(final int seq) {
        return new Args(base_path,"floor4:localhost", "server"+seq);
    }
    @Before
    public void setUp() {
        Util.ForceDeleteNode(zk,base_path);
    }
    @After
    public void tearDown() {
        Util.ForceDeleteNode(zk,base_path);
    }
    @Test
    public void testValueFetcher() {
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
        process.Stop();
    }
    @Test
    public void testChildsFetcher() {
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
        process.Stop();
    }
    @Test
    public void testValueRegister() {
        ZKProcess process=new ZKProcess("test",zkargs);
        ValueFetcher vf=new ValueFetcher(process,base_path);
        Util.ForceDeleteNode(process.zk,base_path);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals(""));
        ValueRegister vr=new ValueRegister(process,new ValueRegister.Request(base_path,"hello",CreateMode.EPHEMERAL));
        Util.Sleep(3*1000);
        Assert.assertTrue(vf.result().value.equals("hello"));
        process.Stop();
        Util.ForceDeleteNode(process.zk,base_path);
    }
    @Test
    public void testRegistersSync() {
        Util.ForceCreateNode(zk,base_path,"registers_sync_test",true);
        ZKProcess process=new ZKProcess("test",zkargs);
        Args args=CreateArgs(0);
        RegistersSync nf=new RegistersSync(process,args);
        process.Start();
        Util.ForceCreateNode(zk,args.GetRegistersPath(),"registers_test",true);
        Util.ForceCreateNode(zk,args.GetNodePath(),"node_test",true);
        Util.ForceCreateNode(zk,args.GetServerPath(),"server_test",true);
        Util.ForceCreateNode(zk,args.GetActivePath(),"active_test",false);
        Util.Sleep(3*1000);
        NodeSync node_sync=nf.dealer.result().get(args.node_path);
        Assert.assertTrue(node_sync!=null);
        ServerSync server_sync=node_sync.dealer.result().get(args.server_num);
        Assert.assertTrue(server_sync!=null);
        Assert.assertTrue(server_sync.active_fetcher.result().value.equals("active_test"));
        process.Stop();
        nf.Close();
    }
    private static int register_active_count=0;
    @Test
    public void testRegistersUpdater() {
        Util.ForceCreateNode(zk,base_path,"registers_sync_test",true);
        ZKProcess process=new ZKProcess("test",zkargs);
        Args args=CreateArgs(0);
        RegistersUpdater nf=new RegistersUpdater(process,args) {
            protected byte[] DoGetActiveInfo() {
                return (""+(register_active_count++)).getBytes();
            }
        };
        process.Start();
        Util.Sleep(3*1000+30*1000);
        Assert.assertTrue(Integer.valueOf(Util.GetNodeData(zk,args.GetActivePath()),10)>0);
        process.Stop();
        nf.Close();
    }
    
    private static class RegistersClientTest {
        public final Args args;
        public final RegistersClient register_client;
        public final RegistersUpdater register_updater;
        public RegistersClientTest(final int i) {
            this.args=MainTest.CreateArgs(i);
            this.register_client=new RegistersClient(args,zkargs);
            this.register_updater=new RegistersUpdater(this.register_client.zkprocess,args) {
                protected byte[] DoGetActiveInfo() {
                    return args.GetServerId().getBytes();
                }
            };
        }
    }
    @Test
    public void testRegistersClient() {
        Util.ForceCreateNode(zk,base_path,"test_registers_client",true);
        final ArrayList<RegistersClientTest> ss=new ArrayList();
        long length=30;
        for(int i=0;i<length;i++) {
            ss.add(new RegistersClientTest(i));
        }
        for(RegistersClientTest s:ss) {
            s.register_client.Start();
        }
        
        Util.Sleep(3000);
        TreeSet<String> nodes=Util.GetNodeChilds(zk,this.base_path+"/Registers");
        Assert.assertTrue(nodes.size()==1);
        TreeSet<String> servers=Util.GetNodeChilds(zk,this.base_path+"/Registers/"+nodes.first());
        Assert.assertTrue(servers.size()==length);
        
        for(RegistersClientTest s:ss) {
            Assert.assertTrue(s.register_client.registers_sync.dealer.result().get(s.args.node_path).
                dealer.result().get(s.args.server_num).
                    active_fetcher.result().value.equals(s.args.GetServerId()));
            s.register_client.Stop();
        }
    }
    @Test
    public void testResolutionSurfaceSync() {
        Util.ForceCreateNode(zk,base_path,"resolution_surface_sync_test",true);
        ZKProcess process=new ZKProcess("test",zkargs);
        ResolutionSurfaceSync rs=new ResolutionSurfaceSync(base_path+"/Resolutions","hello",process);
        process.Start();
        Util.Sleep(3*1000);
        Assert.assertTrue(Util.GetNodeData(zk,base_path+"/Resolutions").length()>0);
        Assert.assertTrue(Util.GetNodeData(zk,base_path+"/ResolutionsClosed").length()>0);
        process.Stop();
        rs.Close();
    }
    
    @Test
    public void testElectionServer() {
        Util.ForceCreateNode(zk,base_path,"server_test",true);
        final ArrayList<Server> ss=new ArrayList();
        long length=50;
        for(int i=0;i<length;i++) {
            ss.add(new Server(CreateArgs(i),zkargs));
        }
        for(Server s:ss) {
            s.Start();
        }
        Util.Sleep(500);
        String resolutions_data=Util.GetNodeData(zk,base_path+"/Resolutions");
        Assert.assertTrue(!resolutions_data.isEmpty());
        int pure_pre_leader_count=0;
        while(true) {
            Util.Sleep(50);
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
        Assert.assertTrue(Util.GetNodeChilds(zk,base_path+"/Resolutions").size()>=(length-pure_pre_leader_count));
        Assert.assertTrue(Util.GetNodeChilds(zk,base_path+"/ResolutionsClosed").size()>=(length-1-pure_pre_leader_count));
    }
    @Test
    public void RegisterTest() {
        
    }
}
