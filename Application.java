package com.connecterra.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import com.connecterra.helpers.Interval;
import com.connecterra.helpers.IntervalTree;
import com.connecterra.helpers.IntervalTreeNode;

public class Application {

    private static IntervalTree tree = new IntervalTree();

    private static void process(String input) {

	String[] args = input.split(" ");
	int start = Integer.parseInt(args[1]);
	int end = Integer.parseInt(args[2]);
	String action = args[3];

	switch (action) {
	case "ADDED":
	    tree.put(new Interval(start, end));
	    break;
	case "REMOVED":
	    tree.remove(new Interval(start, end));
	    break;
	case "DELETED":
	    tree.delete(new Interval(start, end));
	    break;
	}

	print(tree.getDisJointIntervals());
	System.out.print('\n');

    }

    private static void print(List<Interval> list) {
	list.stream().forEachOrdered(System.out::print);
	System.out.print('\n');
    }

    private static void inorder(IntervalTreeNode r) {
	if (r == null) {
	    return;
	}
	inorder(r.left);
	System.out.println(r.interval);
	inorder(r.right);
    }

    public static void main(String[] args) {
	Path currentDir = Paths.get("");
	String fileName = currentDir.toAbsolutePath() + "\\resources\\intervals.txt";
	try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	    stream.forEach(Application::process);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

}