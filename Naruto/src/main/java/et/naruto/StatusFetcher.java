package et.naruto;

import et.naruto.Util.DIAG;

public class StatusFetcher {
    public static class Status implements Cloneable {
        @Override
        public String toString() {
            return "Status [value="+value+"]";
        }
        public String value=null;
        public Status(final String value) {
           this.value=value;
        }
    }
    @Override
	public String toString() {
		return "StatusFetcher[status="+status+"]";
	}
	public Status status;
    public final StatusFetcher this_ref;
    public StatusFetcher(final Status status) {
        this.this_ref=this;
        this.status=status;
    }
    public boolean IsUnknow() {
        if(this.status==null) {
            return true;
        } else {
            return false;
        }
    }
    public boolean IsKnow() {
        if(!IsUnknow()) {
            if(this.status.value!=null) {
                return true;
            }
        }
        return false;
    }
    public boolean IsKnowButEmpty() {
        if(IsKnow()) {
            if(this.status.value.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    public void Fetch() {
        status=new Status(null);
        try {
            DoFetching(status);
        } catch (Exception e) {
            status=null;
            DIAG.Get.d.dig_error("",e);
        }
    }
    public void DoFetching(final Status status) {
    }
}
