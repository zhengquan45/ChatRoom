package org.zhq.handle;

public abstract class ConnectorHandlerChain<Model> {
    private volatile ConnectorHandlerChain<Model> next;

    synchronized boolean handle(ConnectorHandler connectorHandler, Model model) {
        ConnectorHandlerChain<Model> next = this.next;
        if (consume(connectorHandler, model)) {
            return true;
        }
        boolean consumed = next != null && next.handle(connectorHandler, model);
        if (consumed) {
            return true;
        }
        return consumeAgain(connectorHandler, model);
    }

    public ConnectorHandlerChain<Model> appendLast(ConnectorHandlerChain<Model> newNode) {
        if (newNode == this || this.getClass().equals(newNode.getClass())) {
            return this;
        }
        synchronized (this) {
            if (next == null) {
                next = newNode;
                return newNode;
            }
        }
        return next.appendLast(newNode);
    }

    public synchronized boolean remove(Class<? extends ConnectorHandlerChain<Model>> clx) {
        if (this.getClass().equals(clx)) {
            return false;
        }
        synchronized (this) {
            if (next != null) {
                return false;
            } else if (next.getClass().equals(clx)) {
                next = next.next;
                return true;
            } else {
                return next.remove(clx);
            }
        }
    }

    protected abstract boolean consume(ConnectorHandler connectorHandler, Model model);

    protected boolean consumeAgain(ConnectorHandler connectorHandler, Model model) {
        return false;
    }
}
