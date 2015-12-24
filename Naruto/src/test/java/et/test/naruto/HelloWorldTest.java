package et.test.naruto;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import et.naruto.Server;
import et.naruto.Util.DIAG;



public class HelloWorldTest {
	private ZooKeeper zk=null;
    @Before
    public void setUp() {
    }
    @After
    public void tearDown() {
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
