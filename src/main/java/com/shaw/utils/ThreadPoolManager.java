package com.shaw.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 异步任务调用线程池
 */
public enum ThreadPoolManager {
    INSTANCE;// 唯一的实例
    private int POOL_SIZE_MIN = 100; // 线程池最小数量
    private int POOL_SIZE_MAX = 200;// 线程池最大数量数
    private int TIME_KEEP_ALIVE = 10;// 线程允许空闲时间
    private int SIZE_WORK_QUEUE = 40;// 线程池缓存队列大小
    // 线程执行类 属于google Guava包下，在Future基础上对线程池的封装，
    ListeningExecutorService executorService;
    //是否开启记录日志
    public static final boolean OPEN_LOGGER = true;
    //日志记录类
    private Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

    // 通过构造函数完成 线程池初始化和装饰。
    ThreadPoolManager() {
        // 构造线程池执行器
        final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(POOL_SIZE_MIN, POOL_SIZE_MAX, TIME_KEEP_ALIVE,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(SIZE_WORK_QUEUE), new RejectedExecution());
        // 包装线程池执行器
        executorService = MoreExecutors.listeningDecorator(threadPool);
        // 如果打开线程池日志，将适用房定时任务线程定时记录线程池状态到日志中
        if (OPEN_LOGGER) {
            Runnable mAccessBufferThread = new Runnable() {
                @Override
                public void run() {
                    logger.debug(String.format(
                            "business_treadPool thread message activeCount: %s,CompletedTaskCount: %s,taskCount: %s, queue_size: %s",
                            threadPool.getActiveCount(), threadPool.getCompletedTaskCount(), threadPool.getTaskCount(),
                            threadPool.getQueue().size()));
                }
            };
            // 初始化一个线程池大小为1的定时执行器。
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            // 加入定时任务，每10秒输出一次线程池状态
            scheduler.scheduleAtFixedRate(mAccessBufferThread, 0, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * 向线程池中添加Callable任务
     */
    public <T> ListenableFuture<T> addExecuteTask(Callable<T> task) {
        return executorService.submit(task);
    }

    /**
     * 向线程池中添加Runnable任务
     */
    public ListenableFuture<?> addExecuteTask(Runnable task) {
        return executorService.submit(task);
    }

    public void execute(Runnable task) {
        executorService.execute(task);
    }


    // 线程池拒绝任务处理策略。即进入放入队列中被阻塞。
    public class RejectedExecution implements RejectedExecutionHandler {
        public RejectedExecution() {
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.info("Reject msg: {} {}", r.toString(), executor.getActiveCount());
        }
    }
}



