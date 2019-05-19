package com.connecterra.helpers;

public class IntervalTreeNode {

    public Interval interval;
    public IntervalTreeNode left;
    public IntervalTreeNode right;
    boolean color;
    int size;
    int max;

    public IntervalTreeNode(Interval interval, boolean color, int size) {
	this.interval = interval;
	this.color = color;
	this.size = size;
	left = null;
	right = null;
	max = interval.end;
    }

}
