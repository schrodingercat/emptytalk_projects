package et.naruto.versioner;

public class Versioner extends Versionable {
    public Versioner() {
    }
    public Versioner(final Versioner versioner) {
        super(versioner);
    }
    public void Add() {
        this.version++;
        if(this.version>1000) {
            int i=0;
            i++;
        }
    }
    public static interface IWatch {
        public void Do();
    }
    public boolean Watch(final IWatch watch,final Versionable... versionables) {
        long t_version=0;
        for(Versionable v:versionables) {
            if(v!=null) {
                t_version+=v.version;
            }
        }
        if(t_version>this.version) {
            if(t_version>1000) {
                int i=0;
                i++;
            }
            if(watch!=null) {
                watch.Do();
            }
            this.version=t_version;
            return true;
        }
        return false;
    }
}

