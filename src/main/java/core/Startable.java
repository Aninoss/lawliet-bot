package core;

public abstract class Startable {

    private boolean started = false;

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        run();
    }

    protected abstract void run();

}
