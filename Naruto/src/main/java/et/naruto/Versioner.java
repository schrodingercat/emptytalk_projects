package et.naruto;

public class Versioner {
    public static class Fetcher {
        private long version;
        public Fetcher() {
            this.version=0;
        }
        public Fetcher(final Fetcher fetcher) {
            this.version=fetcher.version;
        }
        public boolean Fetch(final Versioner versioner) {
            final long t_version=versioner.version;
            if(t_version>this.version) {
                this.version=t_version;
                return true;
            }
            return false;
        }
    }
    private volatile long version;
    public Versioner() {
        this.version=0;
    }
    public Versioner(final Versioner versioner) {
        this.version=versioner.version;
    }
    public void Add() {
        this.version++;
    }
    public boolean Assign(Fetcher fetcher) {
        long t_version=fetcher.version;
        if(t_version>this.version) {
            this.version=t_version;
            return true;
        }
        return false;
    }
}

