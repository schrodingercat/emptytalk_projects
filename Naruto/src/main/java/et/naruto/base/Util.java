package et.naruto.base;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class Util {
    public static enum UNIQ {
        VALUE;
    }
    public static enum DIAG {
        Log;
        public _Diag d=new _Diag();
        public _Diag _=d;
        public _Diag __=d;
        public _Diag ___=d;
        public _Diag ____=d;
        public _Diag _____=d;
        public _Diag ______=d;
        public _Diag _______=d;
        public _Diag ________=d;
        public _Diag _________=d;
        public _Diag __________=d;
        public _Diag ___________=d;
        public _Diag ____________=d;
        public _Diag _____________=d;
        public _Diag ______________=d;
        public _Diag _______________=d;
        public _Diag ________________=d;
        public _Diag _________________=d;
        public _Diag __________________=d;
        public _Diag ___________________=d;
        public _Diag ____________________=d;
        public _Diag _____________________=d;
        public _Diag ______________________=d;
        public _Diag _______________________=d;
        public _Diag ________________________=d;
        public _Diag _________________________=d;
        public _Diag __________________________=d;
        public _Diag ___________________________=d;
        public _Diag ____________________________=d;
        public _Diag _____________________________=d;
        public _Diag ______________________________=d;
        public _Diag _______________________________=d;
        public _Diag ________________________________=d;
        public _Diag _________________________________=d;
        public _Diag __________________________________=d;
        public _Diag ___________________________________=d;
        public _Diag ____________________________________=d;
        public _Diag _____________________________________=d;
        public _Diag ______________________________________=d;
        public _Diag _______________________________________=d;
        public _Diag ________________________________________=d;
        public _Diag _________________________________________=d;
        public _Diag __________________________________________=d;
        public _Diag ___________________________________________=d;
        public _Diag ____________________________________________=d;
        public _Diag _____________________________________________=d;
        public _Diag ______________________________________________=d;
        public _Diag _______________________________________________=d;
        public _Diag ________________________________________________=d;
        public _Diag _________________________________________________=d;
        public _Diag __________________________________________________=d;
        public _Diag ___________________________________________________=d;
        public _Diag ____________________________________________________=d;
        public _Diag _____________________________________________________=d;
        public _Diag ______________________________________________________=d;
        public _Diag _______________________________________________________=d;
        public _Diag ________________________________________________________=d;
        public _Diag _________________________________________________________=d;
        public _Diag __________________________________________________________=d;
        public _Diag ___________________________________________________________=d;
        public _Diag ____________________________________________________________=d;
        public _Diag _____________________________________________________________=d;
        public _Diag ______________________________________________________________=d;
        public _Diag _______________________________________________________________=d;
        public _Diag ________________________________________________________________=d;
        /*public _Diag d=new _Diag() {
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
    public static class _Diag {
        public static enum LEVEL {
            INFO("[INFO] "),
            ERROR("[ERROR]"),
            DEBUG("[DEBUG]");
            private final String tag;
            private LEVEL(String tag) {
                this.tag=tag;
            }
            public String toString() {
                return this.tag;
            }
        }
        public static final String version="";
        public static final String class_name=_Diag.class.getName();
        private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        protected void DoOutput(LEVEL level,String msg){
            System.out.println(String.format("%s[%s] %s",level.toString(),df.format(new Date()),msg));
        };
        public static String GetParentStackClass() {
            if(true) {
                StackTraceElement[] ss=Thread.currentThread().getStackTrace();
                for(int i=0;i<ss.length;i++) {
                    if(i>0) {
                        String classname=ss[i].getClassName();
                        if(classname.indexOf(class_name)==-1) {
                            return classname+":"+ss[i].getMethodName();
                        }
                    }
                }
                return "Unknow.ClassName";
            } else {
                return "";
            }
        }
        private static String PassMsg(String msg) {
            return String.format(
                "%s[VER=%s][TID=%s][CLS=%s]",
                msg.replaceAll("\n","<[n]>"),
                version,
                Thread.currentThread().getId(),
                GetParentStackClass()
            );
        }
        private static String throwable_string(Throwable e) {
            StringWriter sw=new StringWriter();
            e.printStackTrace(new PrintWriter(sw,true));
            return sw.toString();
        }
        public void out(LEVEL level,String msg) {
            DoOutput(level,PassMsg(msg));
        }
        public void info(String msg) {
            out(LEVEL.INFO,msg);
        }
        public void I(String msg,Object... args) {
            info(String.format(msg,args));
        }
        public void debug(String msg) {
            out(LEVEL.DEBUG,msg);
        }
        public void D(String msg,Object... args) {
            debug(String.format(msg,args));
        }
        public void error(String msg) {
            out(LEVEL.ERROR,msg);
        }
        public void E(String msg,Object... args) {
            error(String.format(msg,args));
        }
        
        public RuntimeException dig_error(String msg,Throwable e) {
            RuntimeException re = new RuntimeException(e);
            if (e.getCause() != null) {
                re.setStackTrace(e.getCause().getStackTrace());
            } else {
                re.setStackTrace(e.getStackTrace());
            }
            out(LEVEL.ERROR,msg+throwable_string(re));
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
        
        public void Error() {
            Assert(false, "");
        }
        public void Error(String msg,Object... args) {
            Assert(false,String.format(msg,args));
        }
    }
    public static void Sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (Exception e) {
            DIAG.Log.d.pass_error("",e);
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
                DIAG.Log.d.pass_error("",e);
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
            DIAG.Log.d.dig_error("",e);
        }
    }
    public static void ForceCreateNode(ZooKeeper zk,String path,String data) {
        ForceCreateNode(zk,path,data,false);
    }
    public static String GetNodeData(ZooKeeper zk,String path) {
        try {
            return new String(zk.getData(path,false,null),"UTF-8");
        } catch (Exception e) {
            DIAG.Log.d.dig_error("",e);
            return null;
        }
    }
    public static TreeSet<String> GetNodeChilds(ZooKeeper zk,String path) {
        try {
            return new TreeSet(zk.getChildren(path,false));
        } catch (Exception e) {
            DIAG.Log.d.dig_error("",e);
            return null;
        }
    }
    public static void ForceCreateNode(ZooKeeper zk,String path,String data,boolean persist) {
        try {
            zk.create(path,data.getBytes(),Ids.OPEN_ACL_UNSAFE,persist?CreateMode.PERSISTENT:CreateMode.EPHEMERAL);
        } catch (Exception e) {
            DIAG.Log.d.dig_error("",e);
        }
    }
    public static String Long2String(final long value) {
        if(value>=0) {
            return String.format("%08x",value);
        } else {
            return String.format("-%08x",-value);
        }
    }
    public static long String2Long(final String value) {
        return Long.valueOf(value,16);
    }
    public static String Bytes2String(final byte[] data) {
        try {
            return new String(data,"UTF-8");
        } catch (Exception e) {
            DIAG.Log.d.pass_error("",e);
            return null;
        }
    }
    public static String GetPathName(String path) {
        return Paths.get(path).getFileName().toString();
    }
}
