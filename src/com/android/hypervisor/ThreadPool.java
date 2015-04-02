package com.android.hypervisor;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
* 
* @author obullxl
*/
public final class ThreadPool {

    
    private static ThreadPool instance = ThreadPool.getInstance();

    public static final int SYSTEM_BUSY_TASK_COUNT = 150;  
   
    public static int worker_num = 5;
    
    private static int taskCounter = 0;

    public static boolean systemIsBusy = false;

    private static List<Task> taskQueue = Collections
            .synchronizedList(new LinkedList<Task>());
    
    public PoolWorker[] workers;

    private ThreadPool() {
        workers = new PoolWorker[5];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new PoolWorker(i);
        }
    }

    private ThreadPool(int pool_worker_num) {
        worker_num = pool_worker_num;
        workers = new PoolWorker[worker_num];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new PoolWorker(i);
        }
    }

    public static synchronized ThreadPool getInstance() {
        if (instance == null)
            return new ThreadPool();
        return instance;
    }
   
    public void addTask(Task newTask) {
        synchronized (taskQueue) {
            newTask.setTaskId(++taskCounter);
            newTask.setSubmitTime(new Date());
            taskQueue.add(newTask);
           
            taskQueue.notifyAll();
        }
    }
   
    public void batchAddTask(Task[] taskes) {
        if (taskes == null || taskes.length == 0) {
            return;
        }
        synchronized (taskQueue) {
            for (int i = 0; i < taskes.length; i++) {
                if (taskes[i] == null) {
                    continue;
                }
                taskes[i].setTaskId(++taskCounter);
                taskes[i].setSubmitTime(new Date());
                taskQueue.add(taskes[i]);
            }
           
            taskQueue.notifyAll();
        }
        for (int i = 0; i < taskes.length; i++) {
            if (taskes[i] == null) {
                continue;
            }
        }
    }
   
    public String getInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nTask Queue Size:" + taskQueue.size());
        for (int i = 0; i < workers.length; i++) {
            sb.append("\nWorker " + i + " is "
                    + ((workers[i].isWaiting()) ? "Waiting." : "Running."));
        }
        return sb.toString();
    }
   
    /**
     * destroy this  threads pools;
     * this is called by above layer 
     */
    public synchronized void destroy() {
        for (int i = 0; i < worker_num; i++) {
            workers[i].stopWorker();
            workers[i] = null;
        }
        taskQueue.clear();
    }

   
    private class PoolWorker extends Thread {
        private int index = -1;
       
        private boolean isRunning = true;
       
        private boolean isWaiting = true;

        public PoolWorker(int index) {
            this.index = index;
            start();
        }

        public void stopWorker() {
            this.isRunning = false;
        }

        public boolean isWaiting() {
            return this.isWaiting;
        }
       
        public void run() {
            while (isRunning) {
                Task task = null;
                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                           
                            taskQueue.wait(20);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                   
                    task = (Task) taskQueue.remove(0);
                }
                if (task != null) {
                    isWaiting = false;
                    try {
                       
                        if (task.needExecuteImmediate()) {
                            new Thread(task).start();  
                        } else {
                        	System.out.println("chenrui");
                            task.run();                
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isWaiting = true;
                    task = null;
                }
            }
        }
    }
}
