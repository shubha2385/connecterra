package com.connecterra.helpers;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class IntervalTree {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private IntervalTreeNode root;
    private List<Interval> disjointIntervals;
    private TreeSet<Interval> deletedBlocks;

    public IntervalTree() {
	disjointIntervals = new ArrayList<>();
	deletedBlocks = new TreeSet<>();
    }

    public List<Interval> getDisJointIntervals() {
	return disjointIntervals;
    }

    private boolean isRed(IntervalTreeNode intervalNode) {
	return intervalNode == null ? false : (intervalNode.color == RED);
    }

    private int size(IntervalTreeNode intervalNode) {
	return intervalNode == null ? 0 : intervalNode.size;
    }

    public int size() {
	return size(root);
    }

    public boolean isEmpty() {
	return root == null;
    }

    public IntervalTreeNode get(Interval interval) {
	if (interval == null) {
	    throw new IllegalArgumentException("interval is empty");
	}
	return get(root, interval);
    }

    private IntervalTreeNode get(IntervalTreeNode intervalNode, Interval interval) {
	while (intervalNode != null) {
	    int cmp = interval.compareTo(intervalNode.interval);
	    if (cmp < 0) {
		intervalNode = intervalNode.left;
	    } else if (cmp > 0) {
		intervalNode = intervalNode.right;
	    } else {
		return intervalNode;
	    }
	}
	return null;
    }

    public boolean contains(Interval interval) {
	return get(interval) != null;
    }

    public void put(Interval interval) {
	if (interval == null) {
	    throw new IllegalArgumentException("interval is empty");
	}
	root = put(root, interval);
	updateDeletedBlocks(interval);
	addToDisjointIntervals(interval);
	root.color = BLACK;
    }

    private IntervalTreeNode put(IntervalTreeNode intervalNode, Interval interval) {

	if (intervalNode == null) {
	    return new IntervalTreeNode(interval, RED, 1);
	}

	int cmp = interval.compareTo(intervalNode.interval);
	if (cmp < 0) {
	    intervalNode.left = put(intervalNode.left, interval);
	} else if (cmp > 0) {
	    intervalNode.right = put(intervalNode.right, interval);
	} else {
	    intervalNode.interval = interval;
	}

	if (isRed(intervalNode.right) && !isRed(intervalNode.left)) {
	    intervalNode = rotateLeft(intervalNode);
	}
	if (isRed(intervalNode.left) && isRed(intervalNode.left.left)) {
	    intervalNode = rotateRight(intervalNode);
	}
	if (isRed(intervalNode.left) && isRed(intervalNode.right)) {
	    flipColors(intervalNode);
	}

	intervalNode.size = size(intervalNode.left) + size(intervalNode.right) + 1;

	if (intervalNode.max < interval.end) {
	    intervalNode.max = interval.end;
	}

	return intervalNode;
    }

    private void updateDeletedBlocks(Interval interval) {
	deletedBlocks = deletedBlocks.stream().filter(block -> !interval.contains(block)).flatMap(block -> {
	    return block.exactOverlap(interval) ? splitInterval(block, interval).stream()
		    : new ArrayList<>(Arrays.asList(block)).stream();
	}).collect(Collectors.toCollection(TreeSet::new));
    }

    private List<Interval> insert(List<Interval> disjointIntervals, Interval newInterval) {
	ArrayList<Interval> ans = new ArrayList<>();
	int n = disjointIntervals.size();
	if (n == 0) {
	    ans.add(newInterval);
	    return deleteBlocksIfNeeded(ans);
	}

	if (newInterval.end < disjointIntervals.get(0).start && !newInterval.doOverlap(disjointIntervals.get(0))
		|| newInterval.start > disjointIntervals.get(n - 1).end
			&& !newInterval.doOverlap(disjointIntervals.get(n - 1))) {
	    if (newInterval.end < disjointIntervals.get(0).start) {
		ans.add(newInterval);
	    }

	    ans.addAll(disjointIntervals);

	    if (newInterval.start > disjointIntervals.get(n - 1).end) {
		ans.add(newInterval);
	    }

	    return deleteBlocksIfNeeded(ans);
	}

	if (newInterval.start <= disjointIntervals.get(0).start
		&& newInterval.end >= disjointIntervals.get(n - 1).end) {
	    ans.add(newInterval);
	    return deleteBlocksIfNeeded(ans);
	}

	boolean overlap = true;
	for (int i = 0; i < n; i++) {
	    overlap = disjointIntervals.get(i).doOverlap(newInterval);
	    if (!overlap) {
		ans.add(disjointIntervals.get(i));

		if (i < n && newInterval.after(disjointIntervals.get(i))
			&& newInterval.before(disjointIntervals.get(i + 1))) {
		    ans.add(newInterval);
		}

		continue;
	    }

	    Interval temp = new Interval();
	    temp.start = Math.min(newInterval.start, disjointIntervals.get(i).start);

	    while (i < n && overlap) {
		temp.end = Math.max(newInterval.end, disjointIntervals.get(i).end);

		if (i == n - 1) {
		    overlap = false;
		} else {
		    overlap = disjointIntervals.get(i + 1).doOverlap(newInterval);
		}

		i++;
	    }

	    i--;
	    ans.add(temp);
	}
	return deleteBlocksIfNeeded(ans);
    }

    private List<Interval> deleteBlocksIfNeeded(List<Interval> intervals) {
	return intervals.parallelStream().flatMap(interval -> {
	    return splitIntervals(interval, deletedBlocks).stream();
	}).collect(Collectors.toList());
    }

    private List<Interval> splitIntervals(Interval interval, Set<Interval> deletedBlocks) {
	Deque<Interval> splitted = new ArrayDeque<>();
	splitted.push(interval);
	deletedBlocks.stream().filter(splitted.peek()::exactOverlap).forEachOrdered(block -> {
	    splitted.addAll(splitInterval(splitted.pop(), block));
	});
	return new ArrayList<>(splitted);
    }

    private List<Interval> splitInterval(Interval interval, Interval block) {
	List<Interval> splittedIntervals = new ArrayList<>();
	if (interval.start >= block.start) {
	    splittedIntervals.add(new Interval(block.end, interval.end));
	} else if (interval.end <= block.end) {
	    splittedIntervals.add(new Interval(interval.start, block.start));
	} else {
	    splittedIntervals.add(new Interval(interval.start, block.start));
	    splittedIntervals.add(new Interval(block.end, interval.end));
	}
	return splittedIntervals;
    }

    private void addToDisjointIntervals(Interval interval) {
	disjointIntervals = insert(disjointIntervals, interval);
    }

    public void deleteMin() {
	if (isEmpty()) {
	    throw new NoSuchElementException("BST underflow");
	}

	if (!isRed(root.left) && !isRed(root.right)) {
	    root.color = RED;
	}

	root = deleteMin(root);

	if (!isEmpty()) {
	    root.color = BLACK;
	}

	assert check();
    }

    private IntervalTreeNode deleteMin(IntervalTreeNode h) {
	if (h.left == null) {
	    return null;
	}

	if (!isRed(h.left) && !isRed(h.left.left)) {
	    h = moveRedLeft(h);
	}

	h.left = deleteMin(h.left);

	return balance(h);
    }

    public void deleteMax() {
	if (isEmpty()) {
	    throw new NoSuchElementException("BST underflow");
	}

	if (!isRed(root.left) && !isRed(root.right)) {
	    root.color = RED;
	}

	root = deleteMax(root);

	if (!isEmpty()) {
	    root.color = BLACK;
	}

	assert check();
    }

    private IntervalTreeNode deleteMax(IntervalTreeNode h) {
	if (isRed(h.left)) {
	    h = rotateRight(h);
	}

	if (h.right == null) {
	    return null;
	}

	if (!isRed(h.right) && !isRed(h.right.left)) {
	    h = moveRedRight(h);
	}

	h.right = deleteMax(h.right);

	return balance(h);
    }

    public void remove(Interval i) {
	if (i == null) {
	    throw new IllegalArgumentException("argument to remove() is null");
	}
	if (!contains(i)) {
	    return;
	}

	if (!isRed(root.left) && !isRed(root.right)) {
	    root.color = RED;
	}

	root = remove(root, i);
	if (!isEmpty()) {
	    root.color = BLACK;
	}

	updateDisjointIntervals();
	assert check();
    }

    private void updateDisjointIntervals() {
	disjointIntervals = mergeOverlappingIntervals();
    }

    private List<Interval> mergeOverlappingIntervals() {
	List<Interval> mergedIntervals = new ArrayList<>();
	if (this.root == null) {
	    return mergedIntervals;
	}
	reverseOrder(this.root, mergedIntervals);
	int index = 0;
	for (int i = 0; i < mergedIntervals.size(); i++) {
	    if (index != 0 && mergedIntervals.get(index - 1).doOverlap(mergedIntervals.get(i))) {
		while (index != 0 && mergedIntervals.get(index - 1).doOverlap(mergedIntervals.get(i))) {
		    Interval temp = new Interval();
		    temp.end = Math.max(mergedIntervals.get(index - 1).end, mergedIntervals.get(i).end);
		    temp.start = Math.min(mergedIntervals.get(index - 1).start, mergedIntervals.get(i).start);
		    mergedIntervals.set(index - 1, temp);
		    index--;
		}
	    } else {
		mergedIntervals.add(index, mergedIntervals.get(i));
	    }

	    index++;
	}
	mergedIntervals = mergedIntervals.subList(0, index);
	Collections.reverse(mergedIntervals);
	return deleteBlocksIfNeeded(mergedIntervals);
    }

    private void reverseOrder(IntervalTreeNode r, List<Interval> reverse) {
	if (r == null) {
	    return;
	}
	reverseOrder(r.right, reverse);
	reverse.add(r.interval);
	reverseOrder(r.left, reverse);
    }

    private IntervalTreeNode remove(IntervalTreeNode r, Interval i) {
	assert get(r, i) != null;

	if (i.compareTo(r.interval) < 0) {
	    if (!isRed(r.left) && !isRed(r.left.left)) {
		r = moveRedLeft(r);
	    }
	    r.left = remove(r.left, i);
	} else {
	    if (isRed(r.left)) {
		r = rotateRight(r);
	    }
	    if (i.compareTo(r.interval) == 0 && (r.right == null)) {
		return null;
	    }
	    if (!isRed(r.right) && !isRed(r.right.left)) {
		r = moveRedRight(r);
	    }
	    if (i.compareTo(r.interval) == 0) {
		IntervalTreeNode x = min(r.right);
		r.interval = x.interval;
		r.right = deleteMin(r.right);
	    } else {
		r.right = remove(r.right, i);
	    }
	}
	return balance(r);
    }


    private IntervalTreeNode rotateRight(IntervalTreeNode r) {
	assert (r != null) && isRed(r.left);
	IntervalTreeNode x = r.left;
	r.left = x.right;
	x.right = r;
	x.color = x.right.color;
	x.right.color = RED;
	x.size = r.size;
	r.size = size(r.left) + size(r.right) + 1;
	return x;
    }


    private IntervalTreeNode rotateLeft(IntervalTreeNode h) {
	assert (h != null) && isRed(h.right);
	IntervalTreeNode x = h.right;
	h.right = x.left;
	x.left = h;
	x.color = x.left.color;
	x.left.color = RED;
	x.size = h.size;
	h.size = size(h.left) + size(h.right) + 1;
	return x;
    }

 
    private void flipColors(IntervalTreeNode h) {
	h.color = !h.color;
	h.left.color = !h.left.color;
	h.right.color = !h.right.color;
    }

    private IntervalTreeNode moveRedLeft(IntervalTreeNode h) {
	flipColors(h);
	if (isRed(h.right.left)) {
	    h.right = rotateRight(h.right);
	    h = rotateLeft(h);
	    flipColors(h);
	}
	return h;
    }


    private IntervalTreeNode moveRedRight(IntervalTreeNode h) {
	flipColors(h);
	if (isRed(h.left.left)) {
	    h = rotateRight(h);
	    flipColors(h);
	}
	return h;
    }

     private IntervalTreeNode balance(IntervalTreeNode h) {
	assert (h != null);

	if (isRed(h.right)) {
	    h = rotateLeft(h);
	}
	if (isRed(h.left) && isRed(h.left.left)) {
	    h = rotateRight(h);
	}
	if (isRed(h.left) && isRed(h.right)) {
	    flipColors(h);
	}
	h.size = size(h.left) + size(h.right) + 1;
	return h;
    }

    public int height() {
	return height(root);
    }

    private int height(IntervalTreeNode x) {
	if (x == null)
	    return -1;
	return 1 + Math.max(height(x.left), height(x.right));
    }

    public Interval min() {
	if (isEmpty())
	    throw new NoSuchElementException("calls min() with empty tree");
	return min(root).interval;
    }


    private IntervalTreeNode min(IntervalTreeNode r) {
	assert r != null;
	if (r.left == null)
	    return r;
	else
	    return min(r.left);
    }

    public Interval max() {
	if (isEmpty())
	    throw new NoSuchElementException("calls max() with empty symbol table");
	return max(root).interval;
    }


    private IntervalTreeNode max(IntervalTreeNode r) {
	assert r != null;
	if (r.right == null)
	    return r;
	else
	    return max(r.right);
    }

    public Interval select(int k) {
	if (k < 0 || k >= size()) {
	    throw new IllegalArgumentException("argument to select() is invalid: " + k);
	}
	IntervalTreeNode x = select(root, k);
	return x.interval;
    }

    
    private IntervalTreeNode select(IntervalTreeNode r, int k) {
	assert r != null;
	assert k >= 0 && k < size(r);
	int t = size(r.left);
	if (t > k)
	    return select(r.left, k);
	else if (t < k)
	    return select(r.right, k - t - 1);
	else
	    return r;
    }

    public int rank(Interval i) {
	if (i == null)
	    throw new IllegalArgumentException("argument to rank() is null");
	return rank(i, root);
    }

 
    private int rank(Interval i, IntervalTreeNode r) {
	if (r == null)
	    return 0;
	int cmp = i.compareTo(r.interval);
	if (cmp < 0)
	    return rank(i, r.left);
	else if (cmp > 0)
	    return 1 + size(r.left) + rank(i, r.right);
	else
	    return size(r.left);
    }


    public List<Interval> intervals() {
	if (isEmpty()) {
	    return new ArrayList<>();
	}
	return intervals(min(), max());
    }

    public List<Interval> intervals(Interval lo, Interval hi) {
	if (lo == null)
	    throw new IllegalArgumentException("first argument to keys() is null");
	if (hi == null)
	    throw new IllegalArgumentException("second argument to keys() is null");

	ArrayList<Interval> queue = new ArrayList<>();
	
	intervals(root, queue, lo, hi);
	return queue;
    }

 
    private void intervals(IntervalTreeNode x, ArrayList<Interval> queue, Interval lo, Interval hi) {
	if (x == null)
	    return;
	int cmplo = lo.compareTo(x.interval);
	int cmphi = hi.compareTo(x.interval);
	if (cmplo < 0)
	    intervals(x.left, queue, lo, hi);
	if (cmplo <= 0 && cmphi >= 0)
	    queue.add(x.interval);
	if (cmphi > 0)
	    intervals(x.right, queue, lo, hi);
    }

    public int size(Interval lo, Interval hi) {
	if (lo == null)
	    throw new IllegalArgumentException("first argument to size() is null");
	if (hi == null)
	    throw new IllegalArgumentException("second argument to size() is null");

	if (lo.compareTo(hi) > 0)
	    return 0;
	if (contains(hi))
	    return rank(hi) - rank(lo) + 1;
	else
	    return rank(hi) - rank(lo);
    }


    private boolean check() {
	if (!isBST())
	    System.out.println("Not in symmetric order");
	if (!isSizeConsistent())
	    System.out.println("Subtree counts not consistent");
	if (!isRankConsistent())
	    System.out.println("Ranks not consistent");
	if (!is23())
	    System.out.println("Not a 2-3 tree");
	if (!isBalanced())
	    System.out.println("Not balanced");
	return isBST() && isSizeConsistent() && isRankConsistent() && is23() && isBalanced();
    }


    private boolean isBST() {
	return isBST(root, null, null);
    }


    private boolean isBST(IntervalTreeNode x, Interval min, Interval max) {
	if (x == null)
	    return true;
	if (min != null && x.interval.compareTo(min) <= 0)
	    return false;
	if (max != null && x.interval.compareTo(max) >= 0)
	    return false;
	return isBST(x.left, min, x.interval) && isBST(x.right, x.interval, max);
    }


    private boolean isSizeConsistent() {
	return isSizeConsistent(root);
    }

    private boolean isSizeConsistent(IntervalTreeNode x) {
	if (x == null)
	    return true;
	if (x.size != size(x.left) + size(x.right) + 1)
	    return false;
	return isSizeConsistent(x.left) && isSizeConsistent(x.right);
    }


    private boolean isRankConsistent() {
	for (int i = 0; i < size(); i++)
	    if (i != rank(select(i))) {
		return false;
	    }
	for (Interval i : intervals())
	    if (i.compareTo(select(rank(i))) != 0) {
		return false;
	    }
	return true;
    }


    private boolean is23() {
	return is23(root);
    }

    private boolean is23(IntervalTreeNode r) {
	if (r == null)
	    return true;
	if (isRed(r.right))
	    return false;
	if (r != root && isRed(r) && isRed(r.left))
	    return false;
	return is23(r.left) && is23(r.right);
    }


    private boolean isBalanced() {
	int black = 0; 
	IntervalTreeNode r = root;
	while (r != null) {
	    if (!isRed(r)) {
		black++;
	    }
	    r = r.left;
	}
	return isBalanced(root, black);
    }

    private boolean isBalanced(IntervalTreeNode x, int black) {
	if (x == null)
	    return black == 0;
	if (!isRed(x))
	    black--;
	return isBalanced(x.left, black) && isBalanced(x.right, black);
    }

    public void delete(Interval interval) {
	deletedBlocks.add(interval);
	disjointIntervals = deleteBlocksIfNeeded(this.disjointIntervals);
    }

}
