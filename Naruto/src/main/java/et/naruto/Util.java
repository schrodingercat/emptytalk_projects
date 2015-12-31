package et.naruto;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.TreeSet;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class Util {
    public static enum DIAG {
        Get;
        public Diag d=new Diag();
        /*public Diag d=new Diag() {
            private static final Logger l=Logger.getLogger(DIAG.class);
            protected void DoInfo(String msg){
                l.info(msg);
            };
            protected void DoError(String msg){
                l.error(msg);
            };
            protected void DoDebug(String msg){
                l.debug(msg);
            };
        }*/
    }
    public static class Diag {
        public static String version="";
        protected void DoInfo(String msg){
            System.out.println(msg);
        };
        protected void DoError(String msg){
            System.out.println(msg);
        };
        protected void DoDebug(String msg){
            System.out.println(msg);
        };
        public static String GetParentStackClass() {
            StackTraceElement[] ss=Thread.currentThread().getStackTrace();
            for(int i=0;i<ss.length;i++) {
                if(i>0) {
                    String classname=ss[i].getClassName();
                    if(classname.indexOf("et.naruto.Util$Diag")==-1) {
                        return classname+":"+ss[i].getMethodName();
                    }
                }
            }
            return "Unknow.ClassName";
        }
        private static String PassMsg(String msg) {
            return String.format(
                "%s[VER=%s][TID=%s][CLS=%s]",
                msg.replaceAll("\n","<[n]>"),
                Thread.currentThread().getId(),
                version,
                GetParentStackClass()
            );
        }
        private static String throwable_string(Throwable e) {
            StringWriter sw=new StringWriter();
            e.printStackTrace(new PrintWriter(sw,true));
            return sw.toString();
        }
            
        public void info(String msg) {
            DoInfo(PassMsg(msg));
        }
        public void debug(String msg) {
            DoDebug(PassMsg(msg));
        }
        public void error(String msg) {
            DoError(PassMsg(msg));
        }
        
        public RuntimeException dig_error(String msg,Throwable e) {
            RuntimeException re = new RuntimeException(e);
            if (e.getCause() != null) {
                re.setStackTrace(e.getCause().getStackTrace());
            } else {
                re.setStackTrace(e.getStackTrace());
            }
            error(msg+throwable_string(re));
            return re;
        }
        
        public void pass_error(String msg,Throwable e) {
            RuntimeException re = dig_error(msg,e);
            throw re;
        }
        
        public void Assert(boolean checked, String info) {
            if (!checked) {
                try {
                    throw new RuntimeException(PassMsg("Assert for: " + info));
                } catch (Exception e) {
                    pass_error("",e);
                }
            }
        }
        
        public void Error(String info) {
            Assert(false, info);
        }
        public void Error() {
            Assert(false, "");
        }
    }
    public static void Sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (Exception e) {
            DIAG.Get.d.pass_error("",e);
        }
    }
    public static class ZKArgs {
        public final String connectString;
        public final long sessionTimeout=10*1000;
        public ZKArgs(final String connectString) {
            this.connectString=connectString;
        }
        public ZooKeeper Create() {
            try {
                return new ZooKeeper("localhost:2181",10*1000,null);
            } catch (Exception e) {
                DIAG.Get.d.pass_error("",e);
                return null;
            }
        }
    }
    public static void ForceDeleteNode(ZooKeeper zk,String path) {
        try {
            List<String> childs=zk.getChildren(path,false);
            for(String child:childs) {
                ForceDeleteNode(zk,path+"/"+child);
            }
            zk.delete(path,-1);
        } catch (Exception e) {
            DIAG.Get.d.dig_error("",e);
        }
    }
    public static void ForceCreateNode(ZooKeeper zk,String path,String data) {
        ForceCreateNode(zk,path,data,false);
    }
    public static String GetNodeData(ZooKeeper zk,String path) {
        try {
            return new String(zk.getData(path,false,null),"UTF-8");
        } catch (Exception e) {
            DIAG.Get.d.dig_error("",e);
            return null;
        }
    }
    public static TreeSet<String> GetNodeChilds(ZooKeeper zk,String path) {
        try {
            return new TreeSet(zk.getChildren(path,false));
        } catch (Exception e) {
            DIAG.Get.d.dig_error("",e);
            return null;
        }
    }
    public static void ForceCreateNode(ZooKeeper zk,String path,String data,boolean persist) {
        try {
            zk.create(path,data.getBytes(),Ids.OPEN_ACL_UNSAFE,persist?CreateMode.PERSISTENT:CreateMode.EPHEMERAL);
        } catch (Exception e) {
            DIAG.Get.d.dig_error("",e);
        }
    }
}
