package org.zhq.core.ds;

public class BytePriorityNode<Item> {
    public byte priority;
    public Item item;
    public BytePriorityNode<Item> next;

    public BytePriorityNode(Item item) {
        this.item = item;
    }

    public void appendWithPriority(BytePriorityNode<Item> node) {
        if (next == null) {
            next = node;
        } else {
            BytePriorityNode<Item> after = next;
            if (node.priority >after.priority ) {
                next = node;
                node.next = after;
            }else{
                after.appendWithPriority(node);
            }
        }

    }
}
