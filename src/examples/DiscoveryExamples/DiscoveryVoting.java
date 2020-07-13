package DiscoveryExamples;

public class DiscoveryVoting {
    boolean out = false;
    int counter = 0;

    public void vote1(boolean a, boolean b, boolean c) {
        out = (a && b) || (b && c) || (a && c);
    }

    public void vote2(boolean a, boolean b, boolean c, int threshold) {
        out = ((a && b) || (b && c) || (a && c)) && (threshold < 10) && (threshold > 5);
    }

    public void vote3(boolean a, boolean b, boolean c) {
        out = a || b || c;
    }

    public void vote4(boolean a, boolean b, boolean c) {
        out = (a || b) && (b || c) && (a || c);
    }

    public void vote0(boolean a, boolean b, boolean c) {
        out = a;
    }

    public void vote5(boolean a, boolean b, boolean c) {
        out = ((a && b) || (b && c) || (a && c));
    }

    public static void main(String[] args) {
        DiscoveryVoting discoveryVoting = new DiscoveryVoting();
        discoveryVoting.makeStep(true, true, true, 4, 1, true);
    }


    public void makeStep(boolean a, boolean b, boolean c, int threshold, int counter, boolean symVar) {
        if (symVar) {
            //vote5(a, b, c);
            //vote2(a, b, c, threshold);
            //this.counter = counter;
            vote2(a, b, c, threshold);
        }
    }
}
