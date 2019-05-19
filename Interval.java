package com.connecterra.helpers;

public class Interval implements Comparable<Interval> {

    private static final int MERGE_DISTANCE = 7;

    int start;
    int end;

    public Interval() {

    }

    public Interval(int start, int end) {
	this.start = start;
	this.end = end;
    }

    private boolean withinMergeDistance(Interval interval) {
	return (start - interval.end >= 0 && start - interval.end <= Interval.MERGE_DISTANCE
		|| interval.start - end >= 0 && interval.start - end <= Interval.MERGE_DISTANCE);
    }

    public boolean doOverlap(Interval interval) {
	return exactOverlap(interval) || withinMergeDistance(interval);
    }

    public boolean exactOverlap(Interval interval) {
	return intersects(interval);
    }

    public boolean contains(Interval interval) {
	return start <= interval.start && end >= interval.end;
    }

    public boolean isContainedIn(Interval interval) {
	return interval.contains(this);
    }

    public boolean intersects(Interval interval) {
	return start <= interval.end && interval.start <= end;
    }

    public boolean after(Interval interval) {
	return start > interval.end;
    }

    public boolean before(Interval interval) {
	return end < interval.start;
    }

    @Override
    public int compareTo(Interval o) {
	if (equals(o))
	    return 0;
	if (start != o.start)
	    return (start - o.start);
	return (end - o.end);
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof Interval) {
	    return (start == ((Interval) obj).start && end == ((Interval) obj).end);
	} else {
	    return false;
	}
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append('[');
	sb.append(start);
	sb.append(',');
	sb.append(end);
	sb.append(']');
	return sb.toString();
    }

}