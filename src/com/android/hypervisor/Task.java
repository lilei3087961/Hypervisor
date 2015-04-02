

package com.android.hypervisor;

import java.util.Date;


public abstract class Task implements Runnable {
   
    private Date generateTime = null;
    
    private Date submitTime = null;
   
    private Date beginExceuteTime = null;
   
    private Date finishTime = null;

    private long taskId;

    public Task() {
        this.generateTime = new Date();
    }

   
    public void run() {
        
    }

    public abstract Task[] taskCore() throws Exception;

   
    protected abstract boolean useDb();

   
    protected abstract boolean needExecuteImmediate();

  
    public abstract String info();

    public Date getGenerateTime() {
        return generateTime;
    }

    public Date getBeginExceuteTime() {
        return beginExceuteTime;
    }

    public void setBeginExceuteTime(Date beginExceuteTime) {
        this.beginExceuteTime = beginExceuteTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

}
