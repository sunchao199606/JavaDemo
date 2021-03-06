package cn.com.sun.j2v8;


import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8Executor;

public class WebWorker {

    public void start(V8Object worker, String... s) {
        String script = s[0];
        V8Executor executor = new V8Executor(script, true, "messageHandler") {
            @Override
            protected void setup(V8 runtime) {
                configureWorker(runtime);
            }
        };
        worker.getRuntime().registerV8Executor(worker, executor);
        executor.start();
    }

    public void terminate(V8Object worker, Object... s) {
        V8Executor executor = worker.getRuntime().removeExecutor(worker);
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void postMessage(V8Object worker, String... s) {
        V8Executor executor = worker.getRuntime().getExecutor(worker);
        if (executor != null) {
            executor.postMessage(s);
        }
    }

    public void print(String s) {
        System.out.println(s);
    }

    public void start() throws InterruptedException {
        V8Executor mainExecutor = new V8Executor(
                "var w = new Worker('messageHandler = function(e) { print(e[0]); }');\n"
                        + "w.postMessage('message to send.');" + "w.postMessage('another message to send.');"
                        + "w.terminate();\n") {
            @Override
            protected void setup(V8 runtime) {
                configureWorker(runtime);
            }
        };
        mainExecutor.start();
        mainExecutor.join();
    }

    private void configureWorker(V8 runtime) {
        // 注册js Worker函数
        runtime.registerJavaMethod(this, "start", "Worker", new Class<?>[]{V8Object.class, String[].class}, true);
        // 给js Worker函数原型对象注册方法
        V8Object worker = runtime.getObject("Worker");
        V8Object prototype = runtime.executeObjectScript("Worker.prototype");
        prototype.registerJavaMethod(this, "terminate", "terminate", new Class<?>[]{V8Object.class, Object[].class}, true);
        prototype.registerJavaMethod(this, "postMessage", "postMessage", new Class<?>[]{V8Object.class, String[].class}, true);
        // 给当前runtime注册print函数
        runtime.registerJavaMethod(this, "print", "print", new Class<?>[]{String.class});
        // 给Worker函数设置原型
        worker.setPrototype(prototype);
        worker.release();
        prototype.release();
    }

    public static void main(String[] args) throws InterruptedException {
        new WebWorker().start();
    }
}

